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

import com.sandwell.JavaSimulation.ErrorException;

/**
 * Class EventManager - Sandwell Discrete Event Simulation
 * <p>
 * The EventManager is responsible for scheduling future events, controlling
 * conditional event evaluation, and advancing the simulation time. Events are
 * scheduled in based on:
 * <ul>
 * <li>1 - The execution time scheduled for the event
 * <li>2 - The priority of the event (if scheduled to occur at the same time)
 * <li>3 - If both 1) and 2) are equal, the order in which the event was
 * scheduled (FILO - Stack ordering)
 * </ul>
 * <p>
 * The event time is scheduled using a backing long value. Double valued time is
 * taken in by the scheduleWait function and scaled to the nearest long value
 * using the simTimeFactor.
 * <p>
 * The EventManager thread is always the bottom thread on the threadStack, so
 * that after each event has finished, along with any spawned events, the
 * program control will pass back to the EventManager.
 * <p>
 * The runnable interface is implemented so that the eventManager runs as a
 * separate thread.
 * <p>
 * EventManager is held as a static member of class entity, this ensures that
 * all entities will schedule themselves with the same event manager.
 */
public final class EventManager implements Runnable {
	public final String name;

	private final Object lockObject; // Object used as global lock for synchronization
	private final ArrayList<Event> eventStack;
	private boolean executeEvents;

	private final ArrayList<Process> conditionalList; // List of all conditionally waiting processes
	private final Thread eventManagerThread;

	private long currentTick; // Master simulation time (long)
	private long nextTick; // The next tick to execute events at
	private long targetTick; // the largest time we will execute events for (run to time)

	// Real time execution state
	private long realTimeTick;    // the simulation tick corresponding to the wall-clock millis value
	private long realTimeMillis;  // the wall-clock time in millis

	private boolean executeRealTime;  // TRUE if the simulation is to be executed in Real Time mode
	private boolean rebaseRealTime;   // TRUE if the time keeping for Real Time model needs re-basing
	private int realTimeFactor;       // target ratio of elapsed simulation time to elapsed wall clock time

	private EventTimeListener timelistener;
	private EventErrorListener errListener;
	private EventTraceListener trcListener;

	/**
	 * Allocates a new EventManager with the given parent and name
	 *
	 * @param parent the connection point for this EventManager in the tree
	 * @param name the name this EventManager should use
	 */
	private EventManager(String name) {
		// Basic initialization
		this.name = name;
		lockObject = new Object();

		// Initialize the thread which processes events from this EventManager
		eventManagerThread = new Thread(this, "evt-" + name);

		// Initialize and event lists and timekeeping variables
		currentTick = 0;
		nextTick = 0;
		eventStack = new ArrayList<Event>();
		conditionalList = new ArrayList<Process>();

		executeEvents = false;
		executeRealTime = false;
		realTimeFactor = 1;
		rebaseRealTime = true;
		setTimeListener(null);
		setErrorListener(null);
	}

	// Used to handshake with the calling thread and make sure the evt thread
	// has made it to the first wait state
	private static class InitListener implements EventTimeListener {
		final Thread waitThread;

		InitListener() {
			waitThread = Thread.currentThread();
		}

		@Override
		public void tickUpdate(long tick) {}
		@Override
		public void timeRunning(boolean running) {
			synchronized (this) {
				waitThread.interrupt();
			}
		}
	}

	public static EventManager initEventManager(String name) {
		EventManager evtman = new EventManager(name);
		InitListener e = new InitListener();
		synchronized (e) {
			evtman.setTimeListener(e);
			evtman.eventManagerThread.start();
			try {
				e.wait();
			}
			catch (InterruptedException e2) {}
			evtman.setTimeListener(null);
		}
		return evtman;
	}

	public final void setTimeListener(EventTimeListener l) {
		synchronized (lockObject) {
			if (l != null)
				timelistener = l;
			else
				timelistener = new DefaultTimeListener();
		}
	}

	public final void setErrorListener(EventErrorListener l) {
		synchronized (lockObject) {
			if (l != null)
				errListener = l;
			else
				errListener = new DefaultErrorListener();
		}
	}

	public final void setTraceListener(EventTraceListener l) {
		synchronized (lockObject) {
			trcListener = l;
		}
	}

	public void clear() {
		synchronized (lockObject) {
			currentTick = 0;
			nextTick = 0;
			targetTick = Long.MAX_VALUE;
			rebaseRealTime = true;

			// Kill threads on the event stack
			for (Event each : eventStack) {
				if (each.process == null)
					continue;

				if (each.process.testFlag(Process.ACTIVE)) {
					throw new ErrorException( "Cannot terminate an active thread" );
				}

				each.process.setFlag(Process.TERMINATE);
				each.process.interrupt();
			}
			eventStack.clear();

			// Kill conditional threads
			for (Process each : conditionalList) {
				if (each.testFlag(Process.ACTIVE)) {
					throw new ErrorException( "Cannot terminate an active thread" );
				}

				each.setFlag(Process.TERMINATE);
				each.interrupt();
			}
			conditionalList.clear();
		}
	}

	/**
	 * Main event processing loop for the eventManager.
	 *
	 * Each eventManager runs its loop continuous throughout the simulation run
	 * with its own thread. The loop for each eventManager is never terminated.
	 * It is only paused and restarted as required. The run method is called by
	 * eventManager.start().
	 */
	@Override
	public void run() {
		synchronized (lockObject) {
			// Loop continuously
			while (true) {
				if (eventStack.isEmpty() ||
				    eventStack.get(eventStack.size() - 1).schedTick >= targetTick) {
					executeEvents = false;
				}

				if (!executeEvents) {
					timelistener.timeRunning(false);
					this.threadWait();
					timelistener.timeRunning(true);
					continue;
				}

				// If the next event is at the current tick, execute it
				if (eventStack.get(eventStack.size() - 1).schedTick == currentTick) {
					// Remove the event from the future events
					Event nextEvent = eventStack.remove(eventStack.size() - 1);
					if (trcListener != null) trcListener.traceEvent(this, nextEvent);
					Process p = nextEvent.process;
					if (p == null)
						p = Process.allocate(this, nextEvent.target);
					// Pass control to this event's thread
					p.setNextProcess(null);
					switchThread(p);
					continue;
				}

				// If the next event would require us to advance the time, check the
				// conditonal events
				if (eventStack.get(eventStack.size() - 1).schedTick > nextTick) {
					if (conditionalList.size() > 0) {
						// Loop through the conditions in reverse order and add to the linked
						// list of active threads
						for (int i = 0; i < conditionalList.size() - 1; i++) {
							conditionalList.get(i).setNextProcess(conditionalList.get(i + 1));
						}
						conditionalList.get(conditionalList.size() - 1).setNextProcess(null);

						// Wake up the first conditional thread to be tested
						// at this point, nextThread == conditionalList.get(0)
						switchThread(conditionalList.get(0));
					}

					// If a conditional event was satisfied, we will have a new event at the
					// beginning of the eventStack for the current tick, go back to the
					// beginning, otherwise fall through to the time-advance
					nextTick = eventStack.get(eventStack.size() - 1).schedTick;
					if (nextTick == currentTick)
						continue;
				}

				// Advance to the next event time
				if (executeRealTime) {
					// Loop until the next event time is reached
					long realTick = this.calcRealTimeTick();
					if (realTick < nextTick) {
						// Update the displayed simulation time
						currentTick = realTick;
						timelistener.tickUpdate(currentTick);
						//Halt the thread for 20ms and then reevaluate the loop
						try { lockObject.wait(20); } catch( InterruptedException e ) {}
						continue;
					}
				}

				// advance time
				currentTick = nextTick;
				timelistener.tickUpdate(currentTick);
			}
		}
	}

	/**
	 * Return the simulation time corresponding the given wall clock time
	 * @param simTime = the current simulation time used when setting a real-time basis
	 * @return simulation time in seconds
	 */
	private long calcRealTimeTick() {
		long curMS = System.currentTimeMillis();
		if (rebaseRealTime) {
			realTimeTick = currentTick;
			realTimeMillis = curMS;
			rebaseRealTime = false;
		}

		double simElapsedsec = ((curMS - realTimeMillis) * realTimeFactor) / 1000.0d;
		long simElapsedTicks = Process.secondsToTicks(simElapsedsec);
		return realTimeTick + simElapsedTicks;
	}

	/**
	 * Called when a process has finished invoking a model method and unwinds
	 * the threadStack one level.
	 */
	void releaseProcess() {
		synchronized (lockObject) {
			assertNotWaitUntil();
			if (trcListener != null) trcListener.traceProcessEnd(this);
			Process next = Process.current().getNextProcess();

			if (next != null) {
				next.interrupt();
			} else {
				// TODO: check for the switching of eventmanagers
				eventManagerThread.interrupt();
			}
		}
	}

	/**
	// Pause the current active thread and restart the next thread on the
	// active thread list. For this case, a future event or conditional event
	// has been created for the current thread.  Called by
	// eventManager.scheduleWait() and related methods, and by
	// eventManager.waitUntil().
	// restorePreviousActiveThread()
	 * Must hold the lockObject when calling this method.
	 */
	private void popThread() {
		Process next = Process.current().getNextProcess();

		Process.current().clearFlag(Process.ACTIVE);
		if (next != null) {
			Process.current().setNextProcess(null);
			switchThread(next);
		} else {
			// TODO: check for the switching of eventmanagers
			switchThread(eventManagerThread);
		}
		Process.current().wake(this);
	}

	/**
	 * Must hold the lockObject when calling this method
	 * @param next
	 */
	private void switchThread(Thread next) {
		next.interrupt();
		threadWait();
	}

	/**
	 * Calculate the time for an event taking into account numeric overflow.
	 * Must hold the lockObject when calling this method
	 */
	private long calculateEventTime(long waitLength) {
		// Test for negative duration schedule wait length
		if(waitLength < 0)
			throw new ErrorException("Negative duration wait is invalid (wait length specified to be %d )", waitLength);

		// Check for numeric overflow of internal time
		long nextEventTime = currentTick + waitLength;
		if (nextEventTime < 0)
			nextEventTime = Long.MAX_VALUE;

		return nextEventTime;
	}

	public void scheduleSingleProcess(long waitLength, int eventPriority, boolean fifo, ProcessTarget t) {
		assertNotWaitUntil();
		synchronized (lockObject) {
			long eventTime = calculateEventTime(waitLength);
			for (int i = eventStack.size() - 1; i >= 0; i--) {
				Event each = eventStack.get(i);
				// We passed where any duplicate could be, break out to the
				// insertion part
				if (each.schedTick > eventTime)
					break;

				// if we have an exact match, do not schedule another event
				if (each.schedTick == eventTime &&
				    each.priority == eventPriority &&
				    each.target == t) {
					if (trcListener != null) trcListener.traceSchedProcess(this, each);
					return;
				}
			}

			// Create an event for the new process at the present time, and place it on the event stack
			Event newEvent = new Event(currentTick, eventTime, eventPriority, null, t);
			if (trcListener != null) trcListener.traceSchedProcess(this, newEvent);
			addEventToStack(newEvent, fifo);
		}
	}

	/**
	 * Schedules a future event to occur with a given priority.  Lower priority
	 * events will be executed preferentially over higher priority.  This is
	 * by lower priority events being placed higher on the event stack.
	 * @param ticks the number of discrete ticks from now to schedule the event.
	 * @param priority the priority of the scheduled event: 1 is the highest priority (default is priority 5)
	 */
	public void waitTicks(long ticks, int priority, boolean fifo) {
		assertNotWaitUntil();
		synchronized (lockObject) {
			long nextEventTime = calculateEventTime(ticks);
			Event temp = new Event(currentTick, nextEventTime, priority, Process.current(), null);
			if (trcListener != null) trcListener.traceWait(this, temp);
			addEventToStack(temp, fifo);
			popThread();
		}
	}

	/**
	 * Adds a new event to the event stack.  This method will add an event to
	 * the event stack based on its scheduled time, priority, and in stack
	 * order for equal time/priority.
	 *
	 * Must hold the lockObject when calling this method.
	 */
	private void addEventToStack(Event newEvent, boolean fifo) {
		int i = eventStack.size() - 1;
		for (; i >= 0; i--) {
			// skip the events that happen at an earlier time
			if (eventStack.get(i).schedTick < newEvent.schedTick)
				continue;

			// events at the same time use priority as a tie-breaker
			if (eventStack.get(i).schedTick == newEvent.schedTick) {
				// skip the events that happen at an earlier priority
				if (eventStack.get(i).priority < newEvent.priority)
					continue;

				if (eventStack.get(i).priority == newEvent.priority) {
					// events of equal time and priority are scheduled in LIFO order, so this is
					// the insertion point, unless we are explicitly doing FIFO ordering
					if (fifo) continue;
				}
			}

			// We fell through all checks, we are at the insertion index, break out
			break;
		}
		// Insert the event in the stack
		eventStack.add(i + 1, newEvent);
	}

	/**
	 * Debugging aid to test that we are not executing a conditional event, useful
	 * to try and catch places where a waitUntil was missing a waitUntilEnded.
	 * While not fatal, it will print out a stack dump to try and find where the
	 * waitUntilEnded was missed.
	 */
	private void assertNotWaitUntil() {
		Process process = Process.current();
		if (process.testFlag(Process.COND_WAIT)) {
			System.out.println("AUDIT - waitUntil without waitUntilEnded " + process);
			for (StackTraceElement elem : process.getStackTrace()) {
				System.out.println(elem.toString());
			}
		}
	}

	/**
	 * Used to achieve conditional waits in the simulation.  Adds the calling
	 * thread to the conditional stack, then wakes the next waiting thread on
	 * the thread stack.
	 */
	public void waitUntil() {
		synchronized (lockObject) {
			if (!conditionalList.contains(Process.current())) {
				if (trcListener != null) trcListener.traceWaitUntil(this);
				Process.current().setFlag(Process.COND_WAIT);
				conditionalList.add(Process.current());
			}
			popThread();
		}
	}

	public void waitUntilEnded() {
		synchronized (lockObject) {
			// Do not wait at all if we never actually were on the waitUntilStack
			// ie. we never called waitUntil
			if (!conditionalList.remove(Process.current()))
				return;

			Process cur = Process.current();
//			if (!cur.testFlag(Process.COND_WAIT)) {
//				System.out.println("ERROR - waitUntil without waitUntilEnded " + cur);
//				for (StackTraceElement elem : cur.getStackTrace()) {
//					System.out.println(elem.toString());
//				}
//			}

			cur.clearFlag(Process.COND_WAIT);
			Event temp = new Event(currentTick, currentTick, 0, cur, null);
			if (trcListener != null) trcListener.traceWaitUntilEnded(this, temp);
			addEventToStack(temp, true);
			popThread();
		}
	}

	public void start(ProcessTarget t) {
		Process newProcess = Process.allocate(this, t);
		// Notify the eventManager that a new process has been started
		synchronized (lockObject) {
			if (trcListener != null) trcListener.traceProcessStart(this, t);
			// Transfer control to the new process
			newProcess.setNextProcess(Process.current());
			switchThread(newProcess);
		}
	}

	/**
	 *	Removes the thread from the pending list and executes it immediately
	 */
	public void interrupt( Process intThread ) {
		synchronized (lockObject) {
			if (intThread.testFlag(Process.ACTIVE)) {
				throw new ErrorException( "Cannot interrupt an active thread" );
			}

			assertNotWaitUntil();

			for (int i = eventStack.size() - 1; i >= 0; i--) {
				if (eventStack.get(i).process == intThread) {
					Event interruptEvent = eventStack.remove(i);
					if (trcListener != null) trcListener.traceInterrupt(this, interruptEvent);
					interruptEvent.process.setNextProcess(Process.current());
					switchThread(interruptEvent.process);
					return;
				}
			}
			throw new ErrorException("Tried to interrupt a thread in %s that couldn't be found", name);
		}
	}

	/**
	 *	Removes an event from the pending list and executes it immediately.
	 */
	public void interrupt(ProcessTarget t) {
		synchronized (lockObject) {
			assertNotWaitUntil();

			for (int i = eventStack.size() - 1; i >= 0; i--) {
				if (eventStack.get(i).target == t) {
					Event interruptEvent = eventStack.remove(i);
					if (trcListener != null) trcListener.traceInterrupt(this, interruptEvent);
					Process proc = Process.allocate(this, interruptEvent.target);
					proc.setNextProcess(Process.current());
					switchThread(interruptEvent.process);
					return;
				}
			}
			throw new ErrorException("Tried to interrupt a thread in %s that couldn't be found", name);
		}
	}

	public void terminateThread( Process killThread ) {
		synchronized (lockObject) {
			if (killThread.testFlag(Process.ACTIVE)) {
				throw new ErrorException( "Cannot terminate an active thread" );
			}

			assertNotWaitUntil();

			if (conditionalList.remove(killThread)) {
				killThread.setFlag(Process.TERMINATE);
				killThread.interrupt();
				return;
			}

			for (int i = eventStack.size() - 1; i >= 0; i--) {
				if (eventStack.get(i).process == killThread) {
					Event temp = eventStack.remove(i);
					if (trcListener != null) trcListener.traceKill(this, temp);
					killThread.setFlag(Process.TERMINATE);
					killThread.interrupt();
					return;
				}
			}
		}
		throw new ErrorException("Tried to terminate a thread in %s that couldn't be found", name);
	}

	/**
	 *	Removes an event from the pending list and executes it immediately.
	 */
	public void terminate(ProcessTarget t) {
		synchronized (lockObject) {
			assertNotWaitUntil();

			for (int i = eventStack.size() - 1; i >= 0; i--) {
				if (eventStack.get(i).target == t) {
					Event temp = eventStack.remove(i);
					if (trcListener != null) trcListener.traceKill(this, temp);
					return;
				}
			}
		}
		throw new ErrorException("Tried to terminate a target in %s that couldn't be found", name);
	}

	public long currentTick() {
		synchronized (lockObject) {
			return currentTick;
		}
	}

	public void setExecuteRealTime(boolean useRealTime, int factor) {
		synchronized (lockObject) {
			executeRealTime = useRealTime;
			realTimeFactor = factor;
			if (useRealTime)
				rebaseRealTime = true;
		}
	}

	/**
	 * Locks the calling thread in an inactive state to the global lock.
	 * When a new thread is created, and the current thread has been pushed
	 * onto the inactive thread stack it must be put to sleep to preserve
	 * program ordering.
	 * <p>
	 * The function takes no parameters, it puts the calling thread to sleep.
	 * This method is NOT static as it requires the use of wait() which cannot
	 * be called from a static context
	 * <p>
	 * There is a synchronized block of code that will acquire the global lock
	 * and then wait() the current thread.
	 */
	private void threadWait() {
		// Ensure that the thread owns the global thread lock
		try {
			/*
			 * Halt the thread and only wake up by being interrupted.
			 *
			 * The infinite loop is _absolutely_ necessary to prevent
			 * spurious wakeups from waking us early....which causes the
			 * model to get into an inconsistent state causing crashes.
			 */
			while (true) { lockObject.wait(); }
		}
		// Catch the exception when the thread is interrupted
		catch( InterruptedException e ) {}

	}

	public void scheduleProcess(long waitLength, int eventPriority, boolean fifo, ProcessTarget t) {
		synchronized (lockObject) {
			long schedTick = calculateEventTime(waitLength);
			Event e = new Event(currentTick, schedTick, eventPriority, null, t);
			if (trcListener != null) trcListener.traceSchedProcess(this, e);
			addEventToStack(e, fifo);
		}
	}

	/**
	 * Sets the value that is tested in the doProcess loop to determine if the
	 * next event should be executed.  If set to false, the eventManager will
	 * execute a threadWait() and wait until an interrupt is generated.  It is
	 * guaranteed in this state that there is an empty thread stack and the
	 * thread referenced in activeThread is the eventManager thread.
	 */
	public void pause() {
		synchronized (lockObject) {
			executeEvents = false;
		}
	}

	/**
	 * Sets the value that is tested in the doProcess loop to determine if the
	 * next event should be executed.  Generates an interrupt of activeThread
	 * in case the eventManager thread has already been paused and needs to
	 * resume the event execution loop.  This prevents the model being resumed
	 * from an inconsistent state.
	 */
	public void resume(long targetTicks) {
		synchronized (lockObject) {
			targetTick = targetTicks;
			rebaseRealTime = true;
			if (executeEvents)
				return;

			executeEvents = true;
			eventManagerThread.interrupt();
		}
	}

	@Override
	public String toString() {
		return name;
	}

	void handleProcessError(Throwable t) {
		this.pause();
		synchronized (lockObject) {
			errListener.handleError(t, currentTick);
		}
	}

	private static class DefaultTimeListener implements EventTimeListener {
		@Override
		public void tickUpdate(long tick) {}
		@Override
		public void timeRunning(boolean running) {}
	}

	private static class DefaultErrorListener implements EventErrorListener {
		@Override
		public void handleError(Throwable t, long currentTick) {}
	}
}
