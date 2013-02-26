/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import com.jaamsim.ui.ExceptionBox;
import com.sandwell.JavaSimulation3D.GUIFrame;

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
public class Process extends Thread {
	// Properties required to manage the pool of available Processes
	private static final ArrayList<Process> pool; // storage for all available Processes
	private static final int maxPoolSize = 100; // Maximum number of Processes allowed to be pooled at a given time
	private static int numProcesses = 0; // Total of all created processes to date (used to name new Processes)

	private static double timeScale; // the scale from discrete to continuous time

	private Entity target; // The entity whose method is to be executed
	private Method method; // The method to be executed
	private Object[] arguments; // The arguments passed to the method to be executed

	private EventManager eventManager; // The EventManager that is currently managing this Process
	private Process nextProcess; // The Process from which the present process was created

	private int flags;  // Present execution state of the process
	static final int TERMINATE = 0x01;  // The process should terminate immediately
	static final int ACTIVE = 0x02;     // The process is currently executing code
	static final int COND_WAIT = 0x04;  // The process is waiting for a condition to be satisfied
	static final int SCHED_WAIT = 0x08; // The process is waiting until a future simulation time
	// Note: The ACTIVE, COND_WAIT, and SCED_WAIT flags are mutually exclusive.
	// The TERMINATE flag can only be set at the same time as COND_WAIT or a
	// SCHED_WAIT flag.

	// Initialize the storage for the pooled Processes
	static {
		pool = new ArrayList<Process>(maxPoolSize);
	}

	private Process(String name) {
		// Construct a thread with the given name
		super(name);
		// Initialize the state flags
		flags = 0;
	}

	/**
	 * Returns the currently executing Process.
	 */
	public static final Process current() {
		try {
			return (Process)Thread.currentThread();
		}
		catch (ClassCastException e) {
			throw new ErrorException("Non-process thread called for Process.current()");
		}
	}

	static long currentTime() {
		return Process.current().getEventManager().currentTime();
	}

	public static final void terminate(Process proc) {
		// Just return if given a null Process
		if (proc == null)
			return;

		Process.current().getEventManager().terminateThread(proc);
	}

	public static final void interrupt(Process proc) {
		// Just return if given a null Process
		if (proc == null)
			return;

		Process.current().getEventManager().interrupt(proc);
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
			this.execute();
		}
	}

	private void execute() {
		Method procMethod;
		Entity procTarget;
		Object[] procArgs;

		// Save a locally-consistent set of target/method/args, synchronized
		// against against a call to allocate() from a separate thread
		synchronized (this) {
			procMethod = this.method;
			procTarget = this.target;
			procArgs = this.arguments;
			this.target = null;
			this.method = null;
			this.arguments = null;
		}

		try {
			// Execute the method
			procMethod.invoke(procTarget, procArgs);

			// Notify the event manager that the process has been completed
			synchronized (this) {
				eventManager.releaseProcess();
			}
			return;
		}

		// Normal exceptions throw by procMethod.invoke() are of type
		// InvocationTargetException
		catch (InvocationTargetException e) {

			// If the process was killed by a terminateThread method then
			// return to the beginning of the process loop
			if (e.getCause() instanceof ThreadKilledException) {
				return;
			}
			// Trap an out of memory error and print a message to the command
			// line
			else if (e.getCause() instanceof java.lang.OutOfMemoryError) {
				System.err.println("Out of Memory " + procTarget.getCurrentTime());
				System.err.println("use the -Xmx flag during execution for more memory");
				System.err.println("Further debug information:");
				OutOfMemoryError err = (OutOfMemoryError)e.getCause();
				System.err.println("Error: " + err.getMessage());
				for (StackTraceElement each : err.getStackTrace())
					System.out.println(each.toString());
				GUIFrame.shutdown(1);
			}
			// All other errors are trapped normally
			else {
				this.makeExceptionBox(e.getCause());
			}
		}

		// Other exception are also possible in the case of programming errors
		catch (IllegalAccessException e) { this.makeExceptionBox(e); }
		catch (IllegalArgumentException e) { this.makeExceptionBox(e); }
		catch (NullPointerException e) { this.makeExceptionBox(e); }
		catch (ExceptionInInitializerError e) { this.makeExceptionBox(e); }
		catch (Throwable e) {
			System.err.println("Caught unknown error, exiting " + e.getClass().getName());
			this.makeExceptionBox(e);
		}
	}


	/**
	 * Create an error message box
	 */
	private void makeExceptionBox(Throwable e) {
		// pause the simulation on a fatal exception
		EventManager.simulation.pause();
		System.err.println("EXCEPTION AT TIME: " + EventManager.simulation.getCurrentTime());
		ExceptionBox exp = ExceptionBox.instance();
		exp.setError(e);
	}

	// Create a new process for the given entity, method, and arguments and transfer
	// control to this process.
	static void start(Entity target, String methodName, Object[] arguments) {

		// Create the new process
		EventManager evt = Process.current().getEventManager();
		Process newProcess = Process.allocate(evt, target, methodName, arguments);
		// Notify the eventManager that a new process has been started
		evt.traceProcess(target, methodName);


		// Transfer control to the new process
		newProcess.setNextProcess(Process.current());
		evt.switchThread(newProcess);
	}

	// Set up a new process for the given entity, method, and arguments
	// Called from Process.start() and from EventManager.startExternalProcess()
	static Process allocate(EventManager eventManager, Entity target, String methodName, Object[] arguments) {

		// Create the new process
		Process newProcess = Process.getProcess();

		// Find the method to be executed
		Method method = Process.findEntityMethod(target.getClass(), methodName, arguments);

		// Setup the process state for execution
		synchronized (newProcess) {
			newProcess.target = target;
			newProcess.method = method;
			newProcess.arguments = arguments;
			newProcess.eventManager = eventManager;
			newProcess.flags = 0;
		}

		return newProcess;
	}

	// Look up the method with the given name for the given entity and argument list.
	private static Method findEntityMethod(Class<?> targetClass, String methodName, Object... arguments) {
		Class<?>[] argClasses = new Class<?>[arguments.length];

		// Fill in the class of each argument, if there are any
		for (int i = 0; i < arguments.length; i++) {
			// The argument itself is null, no class information available
			if (arguments[i] == null) {
				argClasses[i] = null;
				continue;
			}

			argClasses[i] = arguments[i].getClass();

			// We wrap primitive doubles as Double, put back the primitive type
			if (argClasses[i] == Double.class) {
				argClasses[i] = Double.TYPE;
			}

			// We wrap primitive integers as Integer, put back the primitive type
			if (argClasses[i] == Integer.class) {
				argClasses[i] = Integer.TYPE;
			}
		}

		// Attempt to lookup the method using exact type information
		try {
			return targetClass.getMethod(methodName, argClasses);
		}
		catch (SecurityException e) {
			throw new ErrorException("Security Exception when finding method: %s", methodName);
		}
		catch (NullPointerException e) {
			throw new ErrorException("Name passed to startProcess was NULL");
		}
		catch (NoSuchMethodException e) {
			// Get a list of all our methods
			Method[] methods = targetClass.getMethods();

			// Loop over all methods looking for a unique method name
			int matchIndexHolder = -1;
			int numMatches = 0;
			for (int i = 0; i < methods.length; i++) {
				if (methods[i].getName().equals(methodName)) {
					numMatches++;
					matchIndexHolder = i;
				}
			}

			// If there was only one method found, use it
			if (numMatches == 1)
				return methods[matchIndexHolder];
			else
				throw new ErrorException("Method: %s does not exist, could not invoke.", methodName);
		}
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

	synchronized void setNextProcess(Process next) {
		nextProcess = next;
	}

	synchronized Process getNextProcess() {
		return nextProcess;
	}

	synchronized EventManager getEventManager() {
		return eventManager;
	}

	synchronized void setEventManager(EventManager eventManager) {
		this.eventManager = eventManager;
	}

	synchronized String getClassMethod() {
		if (target != null && method != null) {
			return String.format("%s.%s", target.getClass().getSimpleName(), method.getName());
		} else {
			return "Unknown Method State";
		}
	}

	synchronized void setFlag(int flag) {
		flags |= flag;
	}

	synchronized void clearFlag(int flag) {
		flags &= ~flag;
	}

	synchronized boolean testFlag(int flag) {
		return (flags & flag) != 0;
	}

	static void setSimTimeScale(double scale) {
		timeScale = scale;
	}

	public static double getSimTimeFactor() {
		return timeScale;
	}


	public static double getEventTolerance() {
		return (1.0d / getSimTimeFactor());
	}

}
