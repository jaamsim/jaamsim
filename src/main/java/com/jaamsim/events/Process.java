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

	// These refs are only used under the pool lock to communciate to the newly woken threads
	private EventManager eventManager; // The EventManager that is currently managing this Process
	private ProcessTarget target; // The entity whose method is to be executed

	private final AtomicReference<Process> nextProcess = new AtomicReference<>(); // The Process from which the present process was created
	private final AtomicBoolean dieFlag = new AtomicBoolean();
	private final AtomicBoolean activeFlag = new AtomicBoolean();

	// These are a very special references that is only safe to use from the currently
	// executing Process, they are essentially Threadlocal variables that are only valid
	// when activeFlag == true
	private EventManager evt;

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
			ProcessTarget t;
			synchronized (pool) {
				// Ensure all state is cleared before returning to the pool
				evt = null;
				nextProcess.set(null);
				activeFlag.set(false);
				dieFlag.set(false);

				// Add ourselves to the pool and wait to be assigned work
				pool.add(this);
				// Set the present process to sleep, and release its lock
				// (done by pool.wait();)
				// Note: the try/while(true)/catch construct is needed to avoid
				// spurious wake ups allowed as of Java 5.  All legitimate wake
				// ups are done through the InterruptedException.
				try {
					while (true) {
						pool.wait();
						System.out.println("Spurious wakeup in process pool.");
					}
				} catch (InterruptedException e) {}

				evt = eventManager;
				eventManager = null;
				t = target;
				target = null;
				activeFlag.set(true);
			}

			evt.execute(this, t);
		}
	}

	final EventManager evt() {
		return evt;
	}

	// Set up a new process for the given entity, method, and arguments and return a process from the pool or create a new one.
	static Process allocate(EventManager evt, Process next, ProcessTarget targ) {
		while (true) {
			synchronized (pool) {
				// If there is an available process in the pool, then use it
				if (pool.size() > 0) {
					Process proc = pool.remove(pool.size() - 1);
					proc.eventManager = evt;
					proc.target = targ;
					proc.nextProcess.set(next);
					return proc;
				}
				// If there are no process in the pool, then create a new one and add it to the pool
				else {
					numProcesses++;
					Process temp = new Process("processthread-" + numProcesses);
					temp.start(); // Note: Thread.start() calls Process.run which adds the new process to the pool
				}
			}

			// Allow the Process.run method to execute so that it can add the
			// new process to the pool
			// Note: that the lock on the pool has been dropped, so that the
			// Process.run method can grab it.
			try { Thread.sleep(10); } catch (InterruptedException e) {}
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
		super.interrupt();
	}

	void setNextProcess(Process next) {
		nextProcess.set(next);
	}

	/**
	 * Returns true if we woke a next Process, otherwise return false.
	 */
	final boolean wakeNextProcess() {
		Process next = nextProcess.getAndSet(null);
		if (next != null) {
			next.wake();
			return false;
		}
		else {
			return true;
		}
	}

	void kill() {
		if (activeFlag.get())
			throw new ProcessError("Cannot terminate an active thread");
		dieFlag.set(true);
		this.wake();
	}

	/**
	 * This is used to tear down a live threadstack when an error is received from
	 * the model.
	 */
	Process forceKillNext() {
		Process ret = nextProcess.getAndSet(null);
		if (ret != null) {
			ret.dieFlag.set(true);
			ret.wake();
		}
		return ret;
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
