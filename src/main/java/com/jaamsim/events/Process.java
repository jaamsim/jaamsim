/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2014 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.events;

import java.util.ArrayList;

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
public final class Process extends Thread {
	// Properties required to manage the pool of available Processes
	private static final ArrayList<Process> pool; // storage for all available Processes
	private static final int maxPoolSize = 100; // Maximum number of Processes allowed to be pooled at a given time
	private static int numProcesses = 0; // Total of all created processes to date (used to name new Processes)

	private EventManager eventManager; // The EventManager that is currently managing this Process
	private Process nextProcess; // The Process from which the present process was created
	private ProcessTarget target; // The entity whose method is to be executed

	// This is a very special reference that is only safe to use form the currently
	// executing Process, it is essentiall a Threadlocal variable
	private EventManager currentEVT;

	private boolean dieFlag;
	private boolean activeFlag;
	private boolean condWait;

	// Initialize the storage for the pooled Processes
	static {
		pool = new ArrayList<Process>(maxPoolSize);
	}

	private Process(String name) {
		// Construct a thread with the given name
		super(name);
	}

	/**
	 * Returns the currently executing Process.
	 */
	public static final Process current() {
		Thread cur = Thread.currentThread();
		if (cur instanceof Process)
			return (Process)cur;

		throw new ProcessError("Non-process thread called Process.current()");
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
			synchronized (pool) {
				// Add ourselves to the pool and wait to be assigned work
				pool.add(this);
				// Set the present process to sleep, and release its lock
				// (done by pool.wait();)
				// Note: the try/while(true)/catch construct is needed to avoid
				// spurious wake ups allowed as of Java 5.  All legitimate wake
				// ups are done through the InterruptedException.
				try {
					while (true) { pool.wait(); }
				} catch (InterruptedException e) {}
			}

			// Process has been woken up, execute the method we have been assigned
			ProcessTarget t;
			synchronized (this) {
				currentEVT = eventManager;
				t = target;
				target = null;
				activeFlag = true;
			}
			if (t != null)
				currentEVT.executeTarget(t);
			else
				currentEVT.executeEvents(this);

			// Ensure all state is cleared before returning to the pool
			synchronized (this) {
				eventManager = null;
				nextProcess = null;
				target = null;
				currentEVT = null;
				activeFlag = false;
				dieFlag = false;
				condWait = false;
			}
		}
	}

	// Set up a new process for the given entity, method, and arguments
	// Called from Process.start() and from EventManager.startExternalProcess()
	static Process allocate(EventManager eventManager, Process next, ProcessTarget proc) {

		// Create the new process
		Process newProcess = Process.getProcess();

		// Setup the process state for execution
		synchronized (newProcess) {
			newProcess.eventManager = eventManager;
			newProcess.nextProcess = next;
			newProcess.target = proc;
			newProcess.activeFlag = false;
			newProcess.dieFlag = false;
			newProcess.condWait = false;
		}

		return newProcess;
	}

	// Return a process from the pool or create a new one
	private static Process getProcess() {
		while (true) {
			synchronized (pool) {
				// If there is an available process in the pool, then use it
				if (pool.size() > 0) {
					return pool.remove(pool.size() - 1);
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

	static EventManager currentEVT() {
		return Process.current().currentEVT;
	}

	synchronized void setNextProcess(Process next) {
		nextProcess = next;
	}

	/**
	 * Returns true if we woke a next Process, otherwise return false.
	 */
	synchronized boolean wakeNextProcess() {
		Process p = nextProcess;
		nextProcess = null;
		activeFlag = false;
		if (p != null) {
			p.interrupt();
			return true;
		}

		return false;
	}

	synchronized void kill() {
		if (activeFlag)
			throw new ProcessError("Cannot terminate an active thread");
		dieFlag = true;
		this.interrupt();
	}

	synchronized boolean shouldDie() {
		return dieFlag;
	}

	synchronized void setActive() {
		activeFlag = true;
	}

	synchronized void setCondWait(boolean b) {
		condWait = b;
	}

	synchronized boolean isCondWait() {
		return condWait;
	}
}
