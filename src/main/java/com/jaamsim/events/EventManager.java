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
 * The EventManager is responsible for scheduling future events, controlling
 * conditional event evaluation, and advancing the simulation time. Events are
 * scheduled in based on:
 * <ul>
 * <li>1 - The execution time scheduled for the event
 * <li>2 - The priority of the event (if scheduled to occur at the same time)
 * <li>3 - If both 1) and 2) are equal, the user specified FIFO or LIFO order
 * </ul>
 * <p>
 * The event time is scheduled using a backing long value. Double valued time is
 * taken in by the scheduleWait function and scaled to the nearest long value
 * using the simTimeFactor.
 * <p>
 * Most EventManager functionality is available through static methods that rely
 * on being in a running model context which will access the eventmanager that is
 * currently running, think of it like a thread-local variable for all model threads.
 */
public final class EventManager {
	public final String name;

	private final Object lockObject; // Object used as global lock for synchronization

	private final EventTree eventTree;

	private volatile boolean executeEvents;
	private boolean processRunning;

	private final ArrayList<ConditionalEvent> condEvents;

	private long currentTick; // Master simulation time (long)
	private long nextTick; // The next tick to execute events at
	private long targetTick; // the largest time we will execute events for (run to time)

	private double ticksPerSecond; // The number of discrete ticks per simulated second
	private double secsPerTick;    // The length of time in seconds each tick represents

	// Real time execution state
	private long realTimeTick;    // the simulation tick corresponding to the wall-clock millis value
	private long realTimeMillis;  // the wall-clock time in millis

	private volatile boolean executeRealTime;  // TRUE if the simulation is to be executed in Real Time mode
	private volatile boolean rebaseRealTime;   // TRUE if the time keeping for Real Time model needs re-basing
	private volatile int realTimeFactor;       // target ratio of elapsed simulation time to elapsed wall clock time

	private EventTimeListener timelistener;
	private EventErrorListener errListener;
	private EventTraceListener trcListener;

	/**
	 * Allocates a new EventManager with the given parent and name
	 *
	 * @param parent the connection point for this EventManager in the tree
	 * @param name the name this EventManager should use
	 */
	public EventManager(String name) {
		// Basic initialization
		this.name = name;
		lockObject = new Object();

		// Initialize and event lists and timekeeping variables
		currentTick = 0;
		nextTick = 0;

		setTickLength(1e-6d);

		eventTree = new EventTree();
		condEvents = new ArrayList<>();

		executeEvents = false;
		processRunning = false;
		executeRealTime = false;
		realTimeFactor = 1;
		rebaseRealTime = true;
		setTimeListener(null);
		setErrorListener(null);
	}

	public final void setTimeListener(EventTimeListener l) {
		synchronized (lockObject) {
			if (l != null)
				timelistener = l;
			else
				timelistener = new NoopListener();

			timelistener.tickUpdate(currentTick);
		}
	}

	public final void setErrorListener(EventErrorListener l) {
		synchronized (lockObject) {
			if (l != null)
				errListener = l;
			else
				errListener = new NoopListener();
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
			timelistener.tickUpdate(currentTick);
			rebaseRealTime = true;

			eventTree.runOnAllNodes(new KillAllEvents());
			eventTree.reset();
			clearFreeList();

			for (int i = 0; i < condEvents.size(); i++) {
				condEvents.get(i).target.kill();
				if (condEvents.get(i).handle != null) {
					condEvents.get(i).handle.event = null;
				}
			}
			condEvents.clear();
		}
	}

	private static class KillAllEvents implements EventNode.Runner {
		@Override
		public void runOnNode(EventNode node) {
			Event each = node.head;
			while (each != null) {
				if (each.handle != null) {
					each.handle.event = null;
					each.handle = null;
				}

				each.target.kill();
				each = each.next;
			}
		}
	}

	private boolean executeTarget(Process cur, ProcessTarget t) {
		try {
			// If the event has a captured process, pass control to it
			Process p = t.getProcess();
			if (p != null) {
				p.setNextProcess(cur);
				p.wake();
				threadWait(cur);
				return true;
			}

			// Execute the method
			t.process();

			// Notify the event manager that the process has been completed
			if (trcListener != null) trcListener.traceProcessEnd(this, currentTick);
			return !cur.wakeNextProcess();
		}
		catch (Throwable e) {
			// This is how kill() is implemented for sleeping processes.
			if (e instanceof ThreadKilledException)
				return false;

			// Tear down any threads waiting for this to finish
			Process next = cur.forceKillNext();
			while (next != null) {
				next = next.forceKillNext();
			}
			executeEvents = false;
			processRunning = false;
			errListener.handleError(this, e, currentTick);
			return false;
		}
	}

	/**
	 * Main event execution method the eventManager, this is the only entrypoint
	 * for Process objects taken out of the pool.
	 */
	final void execute(Process cur, ProcessTarget t) {
		synchronized (lockObject) {
			// This occurs in the startProcess or interrupt case where we start
			// a process with a target already assigned
			if (t != null) {
				executeTarget(cur, t);
				return;
			}

			if (processRunning)
				return;

			processRunning = true;
			timelistener.timeRunning(true);

			// Loop continuously
			while (true) {
				EventNode nextNode = eventTree.getNextNode();
				if (nextNode == null ||
				    currentTick >= targetTick) {
					executeEvents = false;
				}

				if (!executeEvents) {
					processRunning = false;
					timelistener.timeRunning(false);
					return;
				}

				// If the next event is at the current tick, execute it
				if (nextNode.schedTick == currentTick) {
					// Remove the event from the future events
					Event nextEvent = nextNode.head;
					ProcessTarget nextTarget = nextEvent.target;
					if (trcListener != null) trcListener.traceEvent(this, currentTick, nextNode.schedTick, nextNode.priority, nextTarget);

					removeEvent(nextEvent);

					// the return from execute target informs whether or not this
					// thread should grab an new Event, or return to the pool
					if (executeTarget(cur, nextTarget))
						continue;
					else
						return;
				}

				// If the next event would require us to advance the time, check the
				// conditonal events
				if (eventTree.getNextNode().schedTick > nextTick) {
					if (condEvents.size() > 0) {
						evaluateConditions(cur);
						if (!executeEvents) continue;
					}

					// If a conditional event was satisfied, we will have a new event at the
					// beginning of the eventStack for the current tick, go back to the
					// beginning, otherwise fall through to the time-advance
					nextTick = eventTree.getNextNode().schedTick;
					if (nextTick == currentTick)
						continue;
				}

				// Advance to the next event time
				if (executeRealTime) {
					// Loop until the next event time is reached
					long realTick = this.calcRealTimeTick();
					if (realTick < nextTick && realTick < targetTick) {
						// Update the displayed simulation time
						currentTick = realTick;
						timelistener.tickUpdate(currentTick);
						//Halt the thread for 20ms and then reevaluate the loop
						try { lockObject.wait(20); } catch( InterruptedException e ) {}
						continue;
					}
				}

				// advance time
				if (targetTick < nextTick)
					currentTick = targetTick;
				else
					currentTick = nextTick;

				timelistener.tickUpdate(currentTick);
			}
		}
	}

	private void evaluateConditions(Process cur) {
		cur.begCondWait();
		try {
			for (int i = 0; i < condEvents.size();) {
				ConditionalEvent c = condEvents.get(i);
				if (c.c.evaluate()) {
					condEvents.remove(i);
					EventNode node = getEventNode(currentTick, 0);
					Event evt = getEvent();
					evt.node = node;
					evt.target = c.target;
					evt.handle = c.handle;
					if (evt.handle != null) {
						// no need to check the handle.isScheduled as we just unscheduled it above
						// and we immediately switch it to this event
						evt.handle.event = evt;
					}
					if (trcListener != null) trcListener.traceWaitUntilEnded(this, currentTick, c.target);
					node.addEvent(evt, true);
					continue;
				}
				i++;
			}
		}
		catch (Throwable e) {
			executeEvents = false;
			processRunning = false;
			errListener.handleError(this, e, currentTick);
		}

		cur.endCondWait();
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
		long simElapsedTicks = secondsToNearestTick(simElapsedsec);
		return realTimeTick + simElapsedTicks;
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
	private void captureProcess(Process cur) {
		// if we don't wake a new process, take one from the pool
		if (!cur.wakeNextProcess()) {
			processRunning = false;
			Process.processEvents(this);
		}

		threadWait(cur);
		cur.setActive();
	}

	/**
	 * Calculate the time for an event taking into account numeric overflow.
	 * Must hold the lockObject when calling this method
	 */
	private long calculateEventTime(long waitLength) {
		// Test for negative duration schedule wait length
		if(waitLength < 0)
			throw new ProcessError("Negative duration wait is invalid, waitLength = " + waitLength);

		// Check for numeric overflow of internal time
		long nextEventTime = currentTick + waitLength;
		if (nextEventTime < 0)
			nextEventTime = Long.MAX_VALUE;

		return nextEventTime;
	}

	public static final void waitTicks(long ticks, int priority, boolean fifo, EventHandle handle) {
		Process cur = Process.current();
		cur.evt().waitTicks(cur, ticks, priority, fifo, handle);
	}

	public static final void waitSeconds(double secs, int priority, boolean fifo, EventHandle handle) {
		Process cur = Process.current();
		long ticks = cur.evt().secondsToNearestTick(secs);
		cur.evt().waitTicks(cur, ticks, priority, fifo, handle);
	}

	/**
	 * Schedules a future event to occur with a given priority.  Lower priority
	 * events will be executed preferentially over higher priority.  This is
	 * by lower priority events being placed higher on the event stack.
	 * @param ticks the number of discrete ticks from now to schedule the event.
	 * @param priority the priority of the scheduled event: 1 is the highest priority (default is priority 5)
	 */
	private void waitTicks(Process cur, long ticks, int priority, boolean fifo, EventHandle handle) {
		synchronized (lockObject) {
			cur.checkCondWait();
			long nextEventTime = calculateEventTime(ticks);
			WaitTarget t = new WaitTarget(cur);
			EventNode node = getEventNode(nextEventTime, priority);
			Event evt = getEvent();
			evt.node = node;
			evt.target = t;
			evt.handle = handle;
			if (handle != null) {
				if (handle.isScheduled())
					throw new ProcessError("Tried to schedule using an EventHandle already in use");
				handle.event = evt;
			}

			if (trcListener != null) trcListener.traceWait(this, currentTick, nextEventTime, priority, t);
			node.addEvent(evt, fifo);
			captureProcess(cur);
		}
	}

	/**
	 * Find an eventNode in the list, if a node is not found, create one and
	 * insert it.
	 */
	private EventNode getEventNode(long tick, int prio) {
		return eventTree.createOrFindNode(tick, prio);
	}

	private Event freeEvents = null;
	private Event getEvent() {
		if (freeEvents != null) {
			Event evt = freeEvents;
			freeEvents = evt.next;
			return evt;
		}

		return new Event();
	}

	private void clearFreeList() {
		freeEvents = null;
	}

	public static final void waitUntil(Conditional cond, EventHandle handle) {
		Process cur = Process.current();
		cur.evt().waitUntil(cur, cond, handle);
	}

	/**
	 * Used to achieve conditional waits in the simulation.  Adds the calling
	 * thread to the conditional stack, then wakes the next waiting thread on
	 * the thread stack.
	 */
	private void waitUntil(Process cur, Conditional cond, EventHandle handle) {
		synchronized (lockObject) {
			cur.checkCondWait();
			WaitTarget t = new WaitTarget(cur);
			ConditionalEvent evt = new ConditionalEvent(cond, t, handle);
			if (handle != null) {
				if (handle.isScheduled())
					throw new ProcessError("Tried to waitUntil using a handle already in use");
				handle.event = evt;
			}
			condEvents.add(evt);
			if (trcListener != null) trcListener.traceWaitUntil(this, currentTick);
			captureProcess(cur);
		}
	}

	public static final void scheduleUntil(ProcessTarget t, Conditional cond, EventHandle handle) {
		Process cur = Process.current();
		cur.evt().schedUntil(cur, t, cond, handle);
	}

	private void schedUntil(Process cur, ProcessTarget t, Conditional cond, EventHandle handle) {
		synchronized (lockObject) {
			cur.checkCondWait();
			ConditionalEvent evt = new ConditionalEvent(cond, t, handle);
			if (handle != null) {
				if (handle.isScheduled())
					throw new ProcessError("Tried to scheduleUntil using a handle already in use");
				handle.event = evt;
			}
			condEvents.add(evt);
			if (trcListener != null) trcListener.traceWaitUntil(this, currentTick);
		}
	}

	public static final void startProcess(ProcessTarget t) {
		Process cur = Process.current();
		cur.evt().start(cur, t);
	}

	private void start(Process cur, ProcessTarget t) {
		Process newProcess = Process.allocate(this, cur, t);
		// Notify the eventManager that a new process has been started
		synchronized (lockObject) {
			cur.checkCondWait();
			if (trcListener != null) trcListener.traceProcessStart(this, t, currentTick);
			// Transfer control to the new process
			newProcess.wake();
			threadWait(cur);
		}
	}

	/**
	 * Remove an event from the eventList, must hold the lockObject.
	 * @param idx
	 * @return
	 */
	private void removeEvent(Event evt) {
		EventNode node = evt.node;
		node.removeEvent(evt);
		if (node.head == null) {
			if (!eventTree.removeNode(node.schedTick, node.priority))
				throw new ProcessError("Tried to remove an eventnode that could not be found");
		}

		// Clear the event to reuse it
		evt.node = null;
		evt.target = null;
		if (evt.handle != null) {
			evt.handle.event = null;
			evt.handle = null;
		}

		evt.next = freeEvents;
		freeEvents = evt;
	}

	private ProcessTarget rem(EventHandle handle) {
		ProcessTarget t = handle.event.target;
		BaseEvent base = handle.event;
		if (base instanceof Event) {
			removeEvent((Event)base);
		}
		else {
			handle.event = null;
			base.handle = null;
			condEvents.remove(base);
		}
		return t;
	}

	/**
	 * Removes the event held in the EventHandle and disposes of it, the ProcessTarget is not run.
	 * If the handle does not currently hold a scheduled event, this method simply returns.
	 * @throws ProcessError if called outside of a Process context
	 */
	public static final void killEvent(EventHandle handle) {
		Process cur = Process.current();
		cur.evt().killEvent(cur, handle);
	}

	/**
	 *	Removes an event from the pending list without executing it.
	 */
	private void killEvent(Process cur, EventHandle handle) {
		synchronized (lockObject) {
			cur.checkCondWait();

			// no handle given, or Handle was not scheduled, nothing to do
			if (handle == null || handle.event == null)
				return;

			if (trcListener != null) trcKill(handle.event);
			ProcessTarget t = rem(handle);

			t.kill();
		}
	}

	private void trcKill(BaseEvent event) {
		if (event instanceof Event) {
			EventNode node = ((Event)event).node;
			trcListener.traceKill(this, currentTick, node.schedTick, node.priority, event.target);
		}
		else {
			trcListener.traceKill(this, currentTick, -1, -1, event.target);
		}
	}

	/**
	 * Interrupts the event held in the EventHandle and immediately runs the ProcessTarget.
	 * If the handle does not currently hold a scheduled event, this method simply returns.
	 * @throws ProcessError if called outside of a Process context
	 */
	public static final void interruptEvent(EventHandle handle) {
		Process cur = Process.current();
		cur.evt().interruptEvent(cur, handle);
	}

	/**
	 *	Removes an event from the pending list and executes it.
	 */
	private void interruptEvent(Process cur, EventHandle handle) {
		synchronized (lockObject) {
			cur.checkCondWait();

			// no handle given, or Handle was not scheduled, nothing to do
			if (handle == null || handle.event == null)
				return;

			if (trcListener != null) trcInterrupt(handle.event);
			ProcessTarget t = rem(handle);

			Process proc = t.getProcess();
			if (proc == null)
				proc = Process.allocate(this, cur, t);
			proc.setNextProcess(cur);
			proc.wake();
			threadWait(cur);
		}
	}

	private void trcInterrupt(BaseEvent event) {
		if (event instanceof Event) {
			EventNode node = ((Event)event).node;
			trcListener.traceInterrupt(this, currentTick, node.schedTick, node.priority, event.target);
		}
		else {
			trcListener.traceInterrupt(this, currentTick, -1, -1, event.target);
		}
	}

	public void setExecuteRealTime(boolean useRealTime, int factor) {
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
	private void threadWait(Process cur) {
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
		if (cur.shouldDie())
			throw new ThreadKilledException("Thread killed");
	}

	public static final long secondsToTicks(double secs) {
		Process cur = Process.current();
		return cur.evt().secondsToNearestTick(secs);
	}

	public static final double calcSimTime(double secs) {
		EventManager evt = Process.current().evt();
		long ticks = evt.secondsToNearestTick(secs) + evt.currentTick;
		if (ticks < 0)
			ticks = Long.MAX_VALUE;

		return evt.ticksToSeconds(ticks);
	}

	public static final long calcSimTicks(double secs) {
		EventManager evt = Process.current().evt();
		long ticks = evt.secondsToNearestTick(secs) + evt.currentTick;
		if (ticks < 0)
			ticks = Long.MAX_VALUE;

		return ticks;
	}

	public void scheduleProcessExternal(long waitLength, int eventPriority, boolean fifo, ProcessTarget t, EventHandle handle) {
		synchronized (lockObject) {
			long schedTick = calculateEventTime(waitLength);
			EventNode node = getEventNode(schedTick, eventPriority);
			Event evt = getEvent();
			evt.node = node;
			evt.target = t;
			evt.handle = handle;
			if (handle != null) {
				if (handle.isScheduled())
					throw new ProcessError("Tried to schedule using an EventHandle already in use");
				handle.event = evt;
			}
			if (trcListener != null) trcListener.traceSchedProcess(this, currentTick, schedTick, eventPriority, t);
			node.addEvent(evt, fifo);
		}
	}

	/**
	 * Schedule a future event in the controlling EventManager for the current Process,
	 * if called outside of a Process context, returns null.
	 * @throws ProcessError if called outside of a Process context
	 *
	 * @param waitLength the number of ticks in the future to schedule this event
	 * @param eventPriority the priority of the scheduled event
	 * @param fifo break ties with previously scheduled events using FIFO/LIFO ordering
	 * @param t the process target to run when the event is executed
	 * @param handle an optional handle to hold onto the scheduled event
	 */
	public static final void scheduleTicks(long waitLength, int eventPriority, boolean fifo, ProcessTarget t, EventHandle handle) {
		Process cur = Process.current();
		cur.evt().scheduleTicks(cur, waitLength, eventPriority, fifo, t, handle);
	}

	/**
	 * Schedule a future event in the controlling EventManager for the current Process,
	 * if called outside of a Process context, returns null.
	 * @throws ProcessError if called outside of a Process context
	 *
	 * @param secs the number of seconds in the future to schedule this event
	 * @param eventPriority the priority of the scheduled event
	 * @param fifo break ties with previously scheduled events using FIFO/LIFO ordering
	 * @param t the process target to run when the event is executed
	 * @param handle an optional handle to hold onto the scheduled event
	 */
	public static final void scheduleSeconds(double secs, int eventPriority, boolean fifo, ProcessTarget t, EventHandle handle) {
		Process cur = Process.current();
		long ticks = cur.evt().secondsToNearestTick(secs);
		cur.evt().scheduleTicks(cur, ticks, eventPriority, fifo, t, handle);
	}

	private void scheduleTicks(Process cur, long waitLength, int eventPriority, boolean fifo, ProcessTarget t, EventHandle handle) {
		cur.checkCondWait();
		long schedTick = calculateEventTime(waitLength);
		EventNode node = getEventNode(schedTick, eventPriority);
		Event evt = getEvent();
		evt.node = node;
		evt.target = t;
		evt.handle = handle;
		if (handle != null) {
			if (handle.isScheduled())
				throw new ProcessError("Tried to schedule using an EventHandle already in use");
			handle.event = evt;
		}
		if (trcListener != null) trcListener.traceSchedProcess(this, currentTick, schedTick, eventPriority, t);
		node.addEvent(evt, fifo);
	}

	/**
	 * Sets the value that is tested in the doProcess loop to determine if the
	 * next event should be executed.  If set to false, the eventManager will
	 * execute a threadWait() and wait until an interrupt is generated.  It is
	 * guaranteed in this state that there is an empty thread stack and the
	 * thread referenced in activeThread is the eventManager thread.
	 */
	public void pause() {
		executeEvents = false;
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
			Process.processEvents(this);
		}
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Returns whether or not we are currently running in a Process context
	 * that has a controlling EventManager.
	 * @return true if we are in a Process context, false otherwise
	 */
	public static final boolean hasCurrent() {
		return (Thread.currentThread() instanceof Process);
	}

	/**
	 * Returns the controlling EventManager for the current Process.
	 * @throws ProcessError if called outside of a Process context
	 */
	public static final EventManager current() {
		return Process.current().evt();
	}

	/**
	 * Returns the current simulation tick for the current Process.
	 * @throws ProcessError if called outside of a Process context
	 */
	public static final long simTicks() {
		return Process.current().evt().currentTick;
	}

	/**
	 * Returns the current simulation time in seconds for the current Process.
	 * @throws ProcessError if called outside of a Process context
	 */
	public static final double simSeconds() {
		return Process.current().evt()._simSeconds();
	}

	private double _simSeconds() {
		return currentTick * secsPerTick;
	}

	public final void setTickLength(double tickLength) {
		secsPerTick = tickLength;
		ticksPerSecond = Math.round(1e9d / secsPerTick) / 1e9d;
	}

	/**
	 * Convert the number of seconds rounded to the nearest tick.
	 */
	public final long secondsToNearestTick(double seconds) {
		return Math.round(seconds * ticksPerSecond);
	}

	/**
	 * Convert the number of ticks into a value in seconds.
	 */
	public final double ticksToSeconds(long ticks) {
		return ticks * secsPerTick;
	}
}
