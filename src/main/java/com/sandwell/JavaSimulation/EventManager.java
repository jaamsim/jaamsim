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

import com.jaamsim.events.ProcessTarget;
import com.jaamsim.ui.FrameBox;
import com.sandwell.JavaSimulation3D.GUIFrame;

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
	private static final ArrayList<EventManager> definedManagers;
	static final EventManager rootManager;

	boolean traceEvents = false;

	private static int eventState;
	static final int EVENTS_STOPPED = 0;
	static final int EVENTS_RUNNING = 1;
	static final int EVENTS_RUNONE = 2;
	static final int EVENTS_TIMESTEP = 3;
	static final int EVENTS_UNTILTIME = 4;

	final String name;
	private final EventManager parent;
	private final ArrayList<EventManager> children;

	private final Object lockObject; // Object used as global lock for synchronization
	private final ArrayList<Event> eventStack;
	/*
	 * eventStack is a list of all future events for this eventManager in order
	 * of execution.  The next event to be executed is found at position 0.
	 */
	private final ArrayList<Process> conditionalList; // List of all conditionally waiting processes
	private final Thread eventManagerThread;

	private int activeChildCount; // The number of currently executing child eventManagers
	private long currentTick; // Master simulation time (long)
	private long targetTick; // The time a child eventManager will run to before waking the parent eventManager

	// Real time execution state
	private boolean executeRealTime;  // TRUE if the simulation is to be executed in Real Time mode
	private boolean rebaseRealTime;   // TRUE if the time keeping for Real Time model needs re-basing
	private int realTimeFactor;       // target ratio of elapsed simulation time to elapsed wall clock time
	private double baseSimTime;       // simulation time at which Real Time mode was commenced
	private long baseTimeMillis;      // wall clock time in milliseconds at which Real Time model was commenced

	private EventTraceRecord traceRecord;

	private long debuggingTime;
	/*
	 * event time to debug, implements the run to time functionality
	 */

	static final int PRIO_DEFAULT = 5;
	static final int PRIO_LASTLIFO = 11;
	static final int PRIO_LASTFIFO = 12;

	static final int STATE_WAITING = 0;
	static final int STATE_EXITED = 1;
	static final int STATE_INTERRUPTED = 2;
	static final int STATE_TERMINATED = 3;
	/*
	 * Used to communicate with the eventViewer about the status of a given event
	 */

	static {
		eventState = EVENTS_STOPPED;
		definedManagers = new ArrayList<EventManager>();

		rootManager = new EventManager(null, "DefaultEventManager");
		definedManagers.add(rootManager);
		rootManager.start();
	}

	/**
	 * Allocates a new EventManager with the given parent and name
	 *
	 * @param parent the connection point for this EventManager in the tree
	 * @param name the name this EventManager should use
	 */
	EventManager(EventManager parent, String name) {
		// Basic initialization
		this.name = name;
		lockObject = new Object();

		// Initialize the tree structure for multiple EventManagers
		this.parent = parent;
		if (this.parent != null) {
			this.parent.addChild(this);
		}
		children = new ArrayList<EventManager>();
		activeChildCount = 0;

		// Initialize the thread which processes events from this EventManager
		eventManagerThread = new Thread(this, "evt-" + name);

		traceRecord = new EventTraceRecord();

		// Initialize and event lists and timekeeping variables
		currentTick = 0;
		targetTick = 0;
		eventStack = new ArrayList<Event>();
		conditionalList = new ArrayList<Process>();

		executeRealTime = false;
		realTimeFactor = 1;
		rebaseRealTime = true;
	}

	void start() {
		eventManagerThread.start();
	}

	static void defineEventManager(String name) {
		EventManager evt = new EventManager(rootManager, name);
		definedManagers.add(evt);
		evt.start();
	}

	static EventManager getDefinedManager(String name) {
		for (EventManager each : definedManagers) {
			if (each.name.equals(name)) {
				return each;
			}
		}

		return null;
	}

	private void addChild(EventManager child) {
		synchronized (lockObject) {
			children.add(child);
			activeChildCount++;
		}
	}

	// Initialize the eventManager.  This method is needed only for re-initialization.
	// It is not used when the eventManager is first created.
	void basicInit() {
		targetTick = Long.MAX_VALUE;
		currentTick = 0;
		rebaseRealTime = true;

		traceRecord.clearTrace();
		EventTracer.init();
		traceEvents = false;

		synchronized (lockObject) {
			// Kill threads on the event stack
			for (Event each : eventStack) {
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

	private void evaluateConditionals() {
		// All conditional events if time advanced
		synchronized (lockObject) {
			if (conditionalList.size() == 0) {
				return;
			}

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
	}

	private boolean checkStopConditions() {
		switch (EventManager.getEventState()) {
		case EVENTS_RUNNING:
		case EVENTS_STOPPED:
			return false;
		case EVENTS_RUNONE:
			return true;
		case EVENTS_TIMESTEP:
			if (eventStack.get(0).schedTick != debuggingTime)
				return true;
			else
				return false;
		case EVENTS_UNTILTIME:
			if (eventStack.get(0).schedTick >= debuggingTime)
				return true;
			else
				return false;
		default:
			return false;
		}
	}

	// Notify the parent eventManager than this eventManager has completed all its
	// events up to some specified simulation time.
	private void wakeParent() {
		synchronized (lockObject) {
			// For the top level eventManager, notify the simulation object
			if (parent == null) {
				// Stop the eventManager's thread
				threadWait();
			}
			// For an eventManager that does have a parent
			else {
				// Wake up the parent and stop this eventManager's thread
				parent.wake();
				threadWait();
			}
		}
	}

	// Called by child eventManager to restart its parent eventManager
	private void wake() {
		synchronized (lockObject) {
			// Decrement the number of active child eventManagers
			// (note that this must be done here and not when the child calls this
			// method because the parent's lockobject must be held to modify its property)
			activeChildCount--;
			if (activeChildCount == 0) {
				eventManagerThread.interrupt();
			}
		}
	}

	// Restart executing future events for each child eventManager
	// @nextTime - maximum simulation time to execute until
	private void updateChildren(long nextTick) {
 		synchronized (lockObject) {
 			// Temporary debug code to account for racy initialization
 			if (activeChildCount != 0) {
 				System.out.println("Child count corrupt:"+activeChildCount);
 				activeChildCount = 0;
 			}
 			// Loop through the child eventManagers
			for (EventManager child : children) {
				// Increment the number of active children and restart the
				// child eventManager
				activeChildCount++;
				synchronized (child.lockObject) {
					child.targetTick = nextTick;
					child.eventManagerThread.interrupt();
				}
			}
			if (activeChildCount > 0)
				threadWait();
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

		// Loop continuously
		while (true) {

			// 1) Check whether the model has been paused
			if (parent == null &&
			    EventManager.getEventState() == EventManager.EVENTS_STOPPED) {
				synchronized (lockObject) {
					this.threadWait();
				}
				continue;
			}

			// 2) Execute the next event at the present simulation time

			// Is there another event at this simulation time?
			if (eventStack.size() > 0 &&
				eventStack.get(0).schedTick == currentTick) {

				// Remove the event from the future events
				Event nextEvent = eventStack.remove(0);
				this.retireEvent(nextEvent, STATE_EXITED);
				Process p = nextEvent.process;
				if (p == null)
					p = Process.allocate(this, nextEvent.target);
				// Pass control to this event's thread
				p.setNextProcess(null);
				switchThread(p);

				continue;
			}

			// 3) Check to see if the target simulation time has been reached
			if (currentTick == targetTick) {

				// Notify the parent eventManager that this child has finished
				this.wakeParent();
				continue;
			}

			// 4) Advance to the next event time
			// (at this point, there are no more events at the present event time)

			// Check all the conditional events
			this.evaluateConditionals();

			// Determine the next event time
			long nextTick;
			if (eventStack.size() > 0) {
				nextTick = Math.min(eventStack.get(0).schedTick, targetTick);
			}
			else {
				nextTick = targetTick;
			}

			// Update all the child eventManagers to this next event time
			this.updateChildren(nextTick);

			// Only the top-level eventManager should update the master simulation time
			if (parent == null) {

				// Advance simulation time smoothly between events
				if (executeRealTime) {
					this.doRealTime(nextTick);

					// Has the simulation been stopped and restarted
					if( eventStack.get(0).schedTick == 0 )
						continue;
				}

				// Update the displayed simulation time
				FrameBox.timeUpdate( Process.ticksToSeconds(nextTick) );
			}

			// Set the present time for this eventManager to the next event time
			if (eventStack.size() > 0 && eventStack.get(0).schedTick < nextTick) {
				System.out.format("Big trouble:%s %d %d\n", name, nextTick, eventStack.get(0).schedTick);
				nextTick = eventStack.get(0).schedTick;
			}
			currentTick = nextTick;

			if (checkStopConditions()) {
				synchronized (lockObject) {
					EventManager.setEventState(EventManager.EVENTS_STOPPED);
				}
				GUIFrame.instance().updateForSimulationState(GUIFrame.SIM_STATE_PAUSED);
			}
		}
	}

	/**
	 * Advance simulation time smoothly between events.
	 * @param nextTick = internal simulation time for the next event
	 */
	private void doRealTime(long nextTick) {
		double nextSimTime = Process.ticksToSeconds(nextTick);

		// Loop until the next event time is reached
		double simTime = this.calcSimTime(Process.ticksToSeconds(currentTick));
		while( simTime < nextSimTime && executeRealTime ) {

			// Update the displayed simulation time
			FrameBox.timeUpdate(simTime);

			// Wait for 20 milliseconds
			this.threadPause(20);

			// Has the simulation set to the paused state during the wait?
			if( EventManager.getEventState() == EventManager.EVENTS_STOPPED ) {
				synchronized (lockObject) {
					this.threadWait();
				}
				// Has the simulation been stopped and restarted?
				if( eventStack.get(0).schedTick == 0 )
					return;
			}

			// Calculate the simulation time corresponding to the present wall clock time
			simTime = this.calcSimTime(simTime);
		}
	}

	/**
	 * Return the simulation time corresponding the given wall clock time
	 * @param simTime = the current simulation time used when setting a real-time basis
	 * @return simulation time in seconds
	 */
	private double calcSimTime(double simTime) {
		long curMS = System.currentTimeMillis();
		if (rebaseRealTime) {
			baseSimTime = simTime;
			baseTimeMillis = curMS;
			rebaseRealTime = false;
		}

		double simSeconds = ((curMS - baseTimeMillis) * realTimeFactor) / 1000.0d;
		return baseSimTime + simSeconds;
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

	void switchThread(Thread next) {
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
					//System.out.println("Suppressed duplicate event:" +Process.currentTick() + " " + each.target.getDescription());
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
		if (!Process.current().getEventManager().isParentOf(this)) {
			System.out.format("Crossing eventManager boundary dst:%s src:%s\n",
			                  name, Process.current().getEventManager().name);
			long time = Process.current().getEventManager().currentTick() + ticks;
			if (eventStack.size() > 0 && eventStack.get(0).schedTick > time)
				System.out.format("Next Event:%d This Event:%d\n", eventStack.get(0).schedTick, time);
		}

		long nextEventTime = calculateEventTime(Process.currentTick(), ticks);

		Event temp = new Event(currentTick(), nextEventTime, priority, Process.current(), null);
		Process.current().getEventManager().traceEvent(temp, STATE_WAITING);
		addEventToStack(temp);
		popThread();
	}

	/**
	 * Schedules a future event to occur with a given priority.  Lower priority
	 * events will be executed preferentially over higher priority.  This is
	 * by lower priority events being placed higher on the event stack.
	 * @param waitLength the length of time from now to schedule the event.
	 * @param eventPriority the priority of the scheduled event: 1 is the highest priority (default is priority 5)
	 */
	void scheduleWait(long waitLength, int eventPriority) {
		// Test for zero duration scheduled wait length
		if (waitLength == 0)
			return;

		waitTicks(waitLength, eventPriority);
	}

	/**
	 * Adds a new event to the event stack.  This method will add an event to
	 * the event stack based on its scheduled time, priority, and in stack
	 * order otherwise.  Called from scheduleWait in order to abstract the
	 * underlying data structure and event scheduling routines if necessary.
	 * This implementation utilizes a stack structure, but his could be
	 * replaced with a heap or other basic structure.
	 * @param waitLength is the length of time in the future to schedule the
	 * event.
	 * @param eventPriority is the priority of the event, default 0
	 */
	private void addEventToStack(Event newEvent) {
		synchronized (lockObject) {
			if (newEvent.schedTick < currentTick) {
				System.out.println("Time travel detected - whoops");
				throw new ErrorException("Going back in time");
			}

			int i;
			// skip all event that happen before the new event
			for (i = 0; i < eventStack.size(); i++) {
				if (eventStack.get(i).schedTick < newEvent.schedTick) {
					continue;
				} else {
					break;
				}
			}

			// skip all events at an equal time that have higher priority
			for (; i < eventStack.size(); i++) {
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
				if (newEvent.priority != PRIO_LASTFIFO) {
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
				Process.current().getEventManager().traceWaitUntil(0);
				Process.current().setFlag(Process.COND_WAIT);
				conditionalList.add(Process.current());
			}
		}
		popThread();
	}

	void waitUntilEnded() {
		synchronized (lockObject) {
			if (!conditionalList.remove(Process.current())) {
				// Do not wait at all if we never actually were on the waitUntilStack
				// ie. we never called waitUntil
				return;
			} else {
				traceWaitUntil(1);
				Process.current().clearFlag(Process.COND_WAIT);
				waitTicks(0, PRIO_LASTFIFO);
			}
		}
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
					retireEvent(interruptEvent, STATE_INTERRUPTED);
					interruptEvent.process.setNextProcess(Process.current());
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
					retireEvent(temp, STATE_TERMINATED);
					killThread.setFlag(Process.TERMINATE);
					killThread.interrupt();
					return;
				}
			}
			//if (parent != null) {
			//	parent.terminateThread(killThread);
			//	return;
			//}
		}
		System.out.format("Threadevt:%s", killThread.getEventManager().name);
		throw new ErrorException("Tried to terminate a thread in %s that couldn't be found", name);
	}

	private void retireEvent(Event retired, int reason) {
		traceEvent(retired, reason);
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
		synchronized( lockObject ) {
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
	}

	private void threadPause(long millisToWait) {
		// Ensure that the thread owns the global thread lock
		synchronized( lockObject ) {
			try {
				/*
				 * Halt the thread and allow timeouts to wake us.
				 */
				lockObject.wait(millisToWait);
			}
			// Catch the exception when the thread is interrupted
			catch( InterruptedException e ) {}
		}
	}

	private boolean isParentOf(EventManager child) {
		// Allow trivial case where we check against ourself
		EventManager temp = child;

		while (temp != null) {
			if (this == temp) {
				return true;
			} else {
				temp = temp.parent;
			}
		}

		return false;
	}

	void scheduleProcess(long waitLength, int eventPriority, ProcessTarget t) {
		long schedTick = currentTick + waitLength;
		Event e = new Event(currentTick, schedTick, eventPriority, null, t);
		this.traceSchedProcess(e);
		addEventToStack(e);
	}

	private static synchronized int getEventState() {
		return eventState;
	}

	private static synchronized void setEventState(int state) {
		eventState = state;
	}

	/**
	 * Sets the value that is tested in the doProcess loop to determine if the
	 * next event should be executed.  If set to false, the eventManager will
	 * execute a threadWait() and wait until an interrupt is generated.  It is
	 * guaranteed in this state that there is an empty thread stack and the
	 * thread referenced in activeThread is the eventManager thread.
	 */
	void pause() {
		EventManager.setEventState(EventManager.EVENTS_STOPPED);
	}

	/**
	 * Sets the value that is tested in the doProcess loop to determine if the
	 * next event should be executed.  Generates an interrupt of activeThread
	 * in case the eventManager thread has already been paused and needs to
	 * resume the event execution loop.  This prevents the model being resumed
	 * from an inconsistent state.
	 */
	void resume() {
		rebaseRealTime = true;
		if (EventManager.getEventState() != EventManager.EVENTS_STOPPED)
			return;

		synchronized( lockObject ) {
			EventManager.setEventState(EventManager.EVENTS_RUNNING);
			eventManagerThread.interrupt();
		}
	}

	public void nextOneEvent() {
		EventManager.setEventState(EventManager.EVENTS_RUNONE);
		startDebugging();
	}

	public void nextEventTime() {
		debuggingTime = eventStack.get(0).schedTick;
		EventManager.setEventState(EventManager.EVENTS_TIMESTEP);
		startDebugging();
	}

	public void runToTime( double stopTime ) {
		debuggingTime = ((long)(stopTime * Process.getSimTimeFactor()));
		EventManager.setEventState(EventManager.EVENTS_UNTILTIME);
		GUIFrame.instance().updateForSimulationState(GUIFrame.SIM_STATE_RUNNING);
		startDebugging();
	}

	public void startDebugging() {
		synchronized( lockObject ) {
			if (eventStack.size() == 0)
				return;

			eventManagerThread.interrupt();
		}

	}

	private void traceEvent(Event evt, int reason) {
		if (traceEvents) traceRecord.formatEventTrace(name, evt, reason);
	}

	void traceProcessStart(ProcessTarget t) {
		if (traceEvents) traceRecord.formatBegin(name, currentTick, t);
	}

	void traceProcessEnd() {
		if (traceEvents) traceRecord.formatExit(name, currentTick);
	}

	private void traceSchedProcess(Event target) {
		if (traceEvents) traceRecord.formatSchedProcessTrace(name, currentTick, target);
	}

	private void traceWaitUntil(int reason) {
		if (traceEvents) traceRecord.formatWaitUntilTrace(name, currentTick, reason);
	}

	/**
	 * Holder class for event data used by the event monitor to schedule future
	 * events.
	 */
	static class Event {
		final long addedTick; // The tick at which this event was queued to execute
		final long schedTick; // The tick at which this event will execute
		final int priority;   // The schedule priority of this event

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
}
