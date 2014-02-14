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

import java.util.ArrayList;

import com.jaamsim.events.EventErrorListener;
import com.jaamsim.events.EventTimeListener;
import com.jaamsim.events.EventTraceListener;
import com.jaamsim.events.ProcessTarget;

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

	private boolean traceEvents = false;
	private EventTraceRecord traceRecord;

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

		traceRecord = new EventTraceRecord();

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

	static EventManager initEventManager(String name) {
		EventManager evtman = new EventManager(name);
		evtman.eventManagerThread.start();
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

	public final void setTrace(boolean enable) {
		synchronized (lockObject) {
			traceEvents = enable;
			if (traceEvents)
				trcListener = traceRecord;
			else
				trcListener = null;
		}
	}

	public final void setTraceListener(EventTraceListener l) {
		synchronized (lockObject) {
			trcListener = l;
		}
	}

	void clear() {
		currentTick = 0;
		nextTick = 0;
		targetTick = Long.MAX_VALUE;
		rebaseRealTime = true;

		traceRecord.clearTrace();
		EventTracer.init();

		synchronized (lockObject) {
			setTrace(false);
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
				    eventStack.get(0).schedTick >= targetTick) {
					executeEvents = false;
				}

				if (!executeEvents) {
					timelistener.timeRunning(false);
					this.threadWait();
					timelistener.timeRunning(true);
					continue;
				}

				// If the next event is at the current tick, execute it
				if (eventStack.get(0).schedTick == currentTick) {
					// Remove the event from the future events
					Event nextEvent = eventStack.remove(0);
					traceEvent(nextEvent);
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
				if (eventStack.get(0).schedTick > nextTick) {
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
					nextTick = eventStack.get(0).schedTick;
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
		traceProcessEnd();
		synchronized (lockObject) {
			assertNotWaitUntil();
			Process next = Process.current().getNextProcess();

			if (next != null) {
				next.interrupt();
			} else {
				// TODO: check for the switching of eventmanagers
				Process.current().getEventManager().eventManagerThread.interrupt();
			}
		}
	}

	// Pause the current active thread and restart the next thread on the
	// active thread list. For this case, a future event or conditional event
	// has been created for the current thread.  Called by
	// eventManager.scheduleWait() and related methods, and by
	// eventManager.waitUntil().
	// restorePreviousActiveThread()
	private void popThread() {
		synchronized (lockObject) {
			Process next = Process.current().getNextProcess();

			Process.current().clearFlag(Process.ACTIVE);
			if (next != null) {
				Process.current().setNextProcess(null);
				switchThread(next);
			} else {
				// TODO: check for the switching of eventmanagers
				switchThread(Process.current().getEventManager().eventManagerThread);
			}
			Process.current().wake(this);
		}
	}

	private void switchThread(Thread next) {
		synchronized (lockObject) {
			next.interrupt();
			threadWait();
		}
	}

	/**
	 * Calculate the time for an event taking into account numeric overflow.
	 */
	private long calculateEventTime(long base, long waitLength) {
		// Check for numeric overflow of internal time
		long nextEventTime = base + waitLength;
		if (nextEventTime < 0)
			nextEventTime = Long.MAX_VALUE;

		return nextEventTime;
	}

	void scheduleSingleProcess(long waitLength, int eventPriority, ProcessTarget t) {
		assertNotWaitUntil();

		long eventTime = calculateEventTime(Process.currentTick(), waitLength);
		synchronized (lockObject) {
			for (int i = 0; i < eventStack.size(); i++) {
				Event each = eventStack.get(i);
				// We passed where any duplicate could be, break out to the
				// insertion part
				if (each.schedTick > eventTime)
					break;

				// if we have an exact match, do not schedule another event
				if (each.schedTick == eventTime &&
				    each.priority == eventPriority &&
				    each.target == t) {
					Process.current().getEventManager().traceSchedProcess(each);
					return;
				}
			}

			// Create an event for the new process at the present time, and place it on the event stack
			Event newEvent = new Event(this.currentTick(), eventTime, eventPriority, null, t);
			Process.current().getEventManager().traceSchedProcess(newEvent);
			addEventToStack(newEvent);
		}
	}

	/**
	 * Schedules a future event to occur with a given priority.  Lower priority
	 * events will be executed preferentially over higher priority.  This is
	 * by lower priority events being placed higher on the event stack.
	 * @param ticks the number of discrete ticks from now to schedule the event.
	 * @param priority the priority of the scheduled event: 1 is the highest priority (default is priority 5)
	 */
	void waitTicks(long ticks, int priority) {
		// Test for negative duration schedule wait length
		if(ticks < 0)
			throw new ErrorException("Negative duration wait is invalid (wait length specified to be %d )", ticks);

		assertNotWaitUntil();
		long nextEventTime = calculateEventTime(Process.currentTick(), ticks);

		Event temp = new Event(currentTick(), nextEventTime, priority, Process.current(), null);
		Process.current().getEventManager().traceWait(temp);
		addEventToStack(temp);
		popThread();
	}

	/**
	 * Adds a new event to the event stack.  This method will add an event to
	 * the event stack based on its scheduled time, priority, and in stack
	 * order for equal time/priority.
	 */
	private void addEventToStack(Event newEvent) {
		synchronized (lockObject) {
			if (newEvent.schedTick < currentTick) {
				throw new ErrorException("Going back in time");
			}

			int i = 0;
			for (; i < eventStack.size(); i++) {
				// skip all event that happen before the new event
				if (eventStack.get(i).schedTick < newEvent.schedTick) {
					continue;
				}
				// next stack event happens at a later time, i is the insertion index
				if (eventStack.get(i).schedTick > newEvent.schedTick) {
					break;
				}
				// skip the higher priority events at the same time
				if (eventStack.get(i).priority < newEvent.priority) {
					continue;
				}
				// scheduleLastFIFO is special because it adds in queue, rather
				// than stack ordering, so keep going until we find an event that
				// happens at a later time without regard to priority
				if (newEvent.priority != Entity.PRIO_LASTFIFO) {
					break;
				}
			}
			// Insert the event in the stack
			eventStack.add(i, newEvent);
		}
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
	void waitUntil() {
		synchronized (lockObject) {
			if (!conditionalList.contains(Process.current())) {
				if (trcListener != null) trcListener.traceWaitUntil(this);
				Process.current().setFlag(Process.COND_WAIT);
				conditionalList.add(Process.current());
			}
		}
		popThread();
	}

	void waitUntilEnded(int priority) {
		synchronized (lockObject) {
			// Do not wait at all if we never actually were on the waitUntilStack
			// ie. we never called waitUntil
			if (!conditionalList.remove(Process.current()))
				return;

			if (trcListener != null) trcListener.traceWaitUntilEnded(this);
			Process.current().clearFlag(Process.COND_WAIT);
			waitTicks(0, priority);
		}
	}

	void start(ProcessTarget t) {
		Process newProcess = Process.allocate(this, t);
		// Notify the eventManager that a new process has been started
		traceProcessStart(t);

		// Transfer control to the new process
		newProcess.setNextProcess(Process.current());
		switchThread(newProcess);
	}

	/**
	 *	Removes the thread from the pending list and executes it immediately
	 */
	void interrupt( Process intThread ) {
		synchronized (lockObject) {
			if (intThread.testFlag(Process.ACTIVE)) {
				throw new ErrorException( "Cannot interrupt an active thread" );
			}

			assertNotWaitUntil();

			for (int i = 0; i < eventStack.size(); i++) {
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
	void interrupt(ProcessTarget t) {
		synchronized (lockObject) {
			assertNotWaitUntil();

			for (int i = 0; i < eventStack.size(); i++) {
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

	void terminateThread( Process killThread ) {
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

			for( int i = 0; i < eventStack.size(); i++ ) {
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
	void terminate(ProcessTarget t) {
		synchronized (lockObject) {
			assertNotWaitUntil();

			for( int i = 0; i < eventStack.size(); i++ ) {
				if (eventStack.get(i).target == t) {
					Event temp = eventStack.remove(i);
					if (trcListener != null) trcListener.traceKill(this, temp);
					return;
				}
			}
		}
		throw new ErrorException("Tried to terminate a target in %s that couldn't be found", name);
	}

	long currentTick() {
		return currentTick;
	}

	void setExecuteRealTime(boolean useRealTime, int factor) {
		executeRealTime = useRealTime;
		realTimeFactor = factor;
		if (useRealTime)
			rebaseRealTime = true;
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

	void scheduleProcess(long waitLength, int eventPriority, ProcessTarget t) {
		long schedTick = calculateEventTime(currentTick, waitLength);
		Event e = new Event(currentTick, schedTick, eventPriority, null, t);
		this.traceSchedProcess(e);
		addEventToStack(e);
	}

	/**
	 * Sets the value that is tested in the doProcess loop to determine if the
	 * next event should be executed.  If set to false, the eventManager will
	 * execute a threadWait() and wait until an interrupt is generated.  It is
	 * guaranteed in this state that there is an empty thread stack and the
	 * thread referenced in activeThread is the eventManager thread.
	 */
	void pause() {
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
	void resume(long targetTicks) {
		synchronized (lockObject) {
			targetTick = targetTicks;
			rebaseRealTime = true;
			if (executeEvents)
				return;

			executeEvents = true;
			eventManagerThread.interrupt();
		}
	}

	public void traceAllEvents(boolean enable) {
		EventTracer.traceAllEvents(this, enable);
	}

	public void verifyAllEvents(boolean enable) {
		EventTracer.verifyAllEvents(this, enable);
	}

	private void traceWait(Event evt) {
		if (traceEvents) traceRecord.traceWait(name, evt);
	}

	private void traceEvent(Event evt) {
		if (traceEvents) traceRecord.traceEvent(name, evt);
	}

	private void traceProcessStart(ProcessTarget t) {
		if (traceEvents) traceRecord.formatBegin(name, currentTick, t);
	}

	private void traceProcessEnd() {
		if (traceEvents) traceRecord.formatExit(name, currentTick);
	}

	private void traceSchedProcess(Event target) {
		if (traceEvents) traceRecord.formatSchedProcessTrace(name, currentTick, target);
	}

	/**
	 * Holder class for event data used by the event monitor to schedule future
	 * events.
	 */
	public static class Event {
		public final long addedTick; // The tick at which this event was queued to execute
		public final long schedTick; // The tick at which this event will execute
		public final int priority;   // The schedule priority of this event

		final ProcessTarget target;
		final Process process;

		/**
		 * Constructs a new event object.
		 * @param currentTick the current simulation tick
		 * @param scheduleTick the simulation tick the event is schedule for
		 * @param prio the event priority for scheduling purposes
		 * @param caller
		 * @param process
		 */
		Event(long currentTick, long scheduleTick, int prio, Process process, ProcessTarget target) {
			addedTick = currentTick;
			schedTick = scheduleTick;
			priority = prio;

			this.target = target;
			this.process = process;
		}

		String getDesc() {
			if (target != null)
				return target.getDescription();

			StackTraceElement[] callStack = process.getStackTrace();
			boolean seenEntity = false;
			for (int i = 0; i < callStack.length; i++) {
				if (callStack[i].getClassName().equals("com.sandwell.JavaSimulation.Entity")) {
					seenEntity = true;
					continue;
				}

				if (seenEntity)
					return String.format("%s:%s", callStack[i].getClassName(), callStack[i].getMethodName());
			}

			// Possible the process hasn't started running yet, check the Process target
			// state
			return "Unknown Method State";
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
