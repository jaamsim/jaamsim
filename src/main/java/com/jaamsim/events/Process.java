/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2020 JaamSim Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.events;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Process is a subclass of Thread that can be managed by the discrete event
 * simulation.
 *
 * This is the basis for all functionality required by startProcess and the
 * discrete event model. Each process creates its own thread to run in. These
 * threads are managed by the eventManager and when a Process has completed
 * running is pooled for reuse.
 *
 * LOCKING: All state in the Process must be updated from a synchronized block
 * using the Process itself as the lock object. Care must be taken to never take
 * the eventManager's lock while holding the Process's lock as this can cause a
 * deadlock with other threads trying to wake you from the threadPool.
 */
final class Process extends Thread {
	// Properties required to manage the pool of available Processes
	private static final ArrayList<Process> pool; // storage for all available Processes
	private static final int maxPoolSize = 100; // Maximum number of Processes allowed to be pooled at a given time
	private static int numProcesses = 0; // Total of all created processes to date (used to name new Processes)

	private static final ReentrantLock poolLock = new ReentrantLock();
	private static final Condition waitForProcess = poolLock.newCondition();
	private final Condition waitInPool = poolLock.newCondition();

	private final AtomicReference<EventManager> evt = new AtomicReference<>(); // The EventManager that is currently managing this Process
	final AtomicReference<Condition> waitInEvt = new AtomicReference<>(); // The EventManager that is currently managing this Process
	private final AtomicReference<Process> nextProcess = new AtomicReference<>(); // The Process from which the present process was created
	private final AtomicBoolean dieFlag = new AtomicBoolean();
	private final AtomicBoolean activeFlag = new AtomicBoolean();

	// Initialize the storage for the pooled Processes
	static {
		pool = new ArrayList<>(maxPoolSize);
	}

	private Process(String name) {
		// Construct a thread with the given name
		super(name);
	}

	/**
	 * Returns the currently executing Process.
	 */
	static final Process current() {
		try {
			return (Process)Thread.currentThread();
		}
		catch (ClassCastException e) {
			throw new ProcessError("Non-process thread called Process.current()");
		}
	}

	/**
	 * Run method invokes the method on the target with the given arguments.
	 * A process loops endlessly after it is created executing the method on the
	 * target set as the entry point.  After completion, it calls endProcess and
	 * will return it to a process pool if space is available, otherwise the resources
	 * including the backing thread will be released.
	 *
	 * This method is called by Process.getProcess()
	 */
	@Override
	public void run() {
		while (true) {
			poolLock.lock();
			try {
				// Ensure all state is cleared before returning to the pool
				evt.set(null);
				waitInEvt.set(null);
				nextProcess.set(null);
				activeFlag.set(false);
				dieFlag.set(false);

				// Add ourselves to the pool and wait to be assigned work
				pool.add(this);
				waitForProcess.signal();
				// Set the present process to sleep, and release its lock
				// (done by pool.wait();)
				// Note: the try/while(true)/catch construct is needed to avoid
				// spurious wake ups allowed as of Java 5.  All legitimate wake
				// ups are done through the InterruptedException.
				while (true) {
					waitInPool.awaitUninterruptibly();
					if (evt.get() == null)
						System.out.println("Spurious wakeup in process pool.");
					else
						break;
				}

				activeFlag.set(true);
			}
			finally {
				poolLock.unlock();
			}

			evt.get().execute(this);
		}
	}

	final EventManager evt() {
		return evt.get();
	}

	// Set up a new process for the given entity, method, and arguments and return a process from the pool or create a new one.
	static Process allocate(EventManager evt, Process next) {
		while (true) {
			poolLock.lock();
			try {
				// If there is an available process in the pool, then use it
				if (pool.size() > 0) {
					Process proc = pool.remove(pool.size() - 1);
					proc.evt.set(evt);
					proc.waitInEvt.set(evt.getWaitCondition());
					proc.nextProcess.set(next);
					proc.waitInPool.signal();
					return proc;
				}
				// If there are no process in the pool, then create a new one and add it to the pool
				else {
					numProcesses++;
					Process temp = new Process("processthread-" + numProcesses);
					temp.start(); // Note: Thread.start() calls Process.run which adds the new process to the pool
				}

				waitForProcess.awaitUninterruptibly();
			}
			finally {
				poolLock.unlock();
			}
		}
	}

	/**
	 * We override this method to prevent user code from breaking the event state machine.
	 * If user code explicitly interrupted a Process it would likely run event code
	 * much earlier than intended.
	 */
	@Override
	public void interrupt() {
		new Throwable("AUDIT: direct call of Process.interrupt").printStackTrace();
	}

	/**
	 * This is the wrapper to allow internal code to advance the state machine by waking
	 * a Process.
	 */
	final void wake() {
		waitInEvt.get().signal();
	}

	void setNextProcess(Process next) {
		nextProcess.set(next);
	}

	/**
	 * Returns true if we woke a next Process, otherwise return false.
	 */
	final Process getNextProcess() {
		return nextProcess.getAndSet(null);
	}

	/**
	 * This is used to tear down a live threadstack when an error is received from
	 * the model.
	 */
	final void tearDownRunningProcesses() {
		Process next = nextProcess.getAndSet(null);
		while (next != null) {
			next.dieFlag.set(true);
			next.wake();
			next = next.nextProcess.getAndSet(null);
		}
	}

	boolean shouldDie() {
		return dieFlag.get();
	}

	final Process preCapture() {
		activeFlag.set(false);
		return nextProcess.getAndSet(null);
	}

	final void postCapture() {
		activeFlag.set(true);
	}
}
