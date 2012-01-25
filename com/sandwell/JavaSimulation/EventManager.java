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

import com.sandwell.JavaSimulation3D.EventViewer;

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
	private static Simulation simulation; // Simulation object

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
	private long currentTime; // Master simulation time (long)
	private long targetTime; // The time a child eventManager will run to before waking the parent eventManager

	// Real time execution state
	private boolean executeRealTime;
	private long previousInternalTime;
	private long previousWallTime;

	private EventTraceRecord traceRecord;

	private EventViewer currentViewer;
	/*
	 * TODO: eventViewer needs to show events for all eventManagers, not just a
	 * single eventManager
	 */
	private boolean wasPaused; // Holds the state of the model when an eventViewer was opened

	private long debuggingTime;
	/*
	 * TODO: rename this to better reflect its function. Used by the eventViewer in
	 * doOneEvent, doEventsAtTime, runToTime
	 *
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
		currentTime = 0;
		targetTime = 0;
		eventStack = new ArrayList<Event>();
		conditionalList = new ArrayList<Process>();

		executeRealTime = false;
		previousInternalTime = -1;
	}

	void start() {
		eventManagerThread.start();
	}

	static void setSimulation(Simulation sim) {
		simulation = sim;
	}

	private void addChild(EventManager child) {
		synchronized (lockObject) {
			children.add(child);
			activeChildCount++;
		}
	}

	void basicInit() {
		targetTime = Long.MAX_VALUE;
		currentTime = 0;
		traceRecord.clear();
	}

	// Initialize the eventManager.  This method is needed only for re-initialization.
	// It is not used when the eventManager is first created.
	void initialize() {

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

	private void doDebug() {
		synchronized (lockObject) {
			simulation.setEventState(Simulation.EVENTS_STOPPED);
		}

		// update the event display if there is one present
		if (currentViewer != null)
			currentViewer.update();
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
	private void updateChildren(long nextTime) {
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
					child.targetTime = nextTime;
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
	public void run() {

		// Loop continuously
		while (true) {

			// 1) Check whether the model has been paused
			if (parent == null &&
				simulation.getEventState() == Simulation.EVENTS_STOPPED) {
				synchronized (lockObject) {
					this.threadWait();
				}
				continue;
			}

			// 2) Execute the next event at the present simulation time

			// Is there another event at this simulation time?
			if (eventStack.size() > 0 &&
				eventStack.get(0).eventTime == currentTime) {

				// Remove the event from the future events
				Event nextEvent = eventStack.remove(0);
				this.retireEvent(nextEvent, STATE_EXITED);

				// If required, track the events for this entity
				if (nextEvent.caller.testFlag(Entity.FLAG_TRACKEVENTS)) {
					System.out.println(String.format("TRACK caller: %s at:%d[%.3f]",
						nextEvent.caller.getName(), currentTime, currentTime/Simulation.getSimTimeFactor()));
				}

				// Pass control to this event's thread
				nextEvent.process.setNextProcess(null);
				switchThread(nextEvent.process);

				continue;
			}

			// 3) Check to see if the target simulation time has been reached
			if (currentTime == targetTime) {

				// Notify the parent eventManager that this child has finished
				this.wakeParent();
				continue;
			}

			// 4) Advance to the next event time
			// (at this point, there are no more events at the present event time)

			// Check all the conditional events
			this.evaluateConditionals();

			// Determine the next event time
			long nextTime;
			if (eventStack.size() > 0) {
				nextTime = Math.min(eventStack.get(0).eventTime, targetTime);
			}
			else {
				nextTime = targetTime;
			}

			// Update all the child eventManagers to this next event time
			this.updateChildren(nextTime);

			// Only the top-level eventManager should update the master simulation
			// time
			if (parent == null)
				this.updateTime(nextTime);

			// Set the present time for this eventManager to the next event time
			if (eventStack.size() > 0 && eventStack.get(0).eventTime < nextTime) {
				System.out.format("Big trouble:%s %d %d\n", name, nextTime, eventStack.get(0).eventTime);
				nextTime = eventStack.get(0).eventTime;
			}
			currentTime = nextTime;

			if (simulation.getEventState() == Simulation.EVENTS_RUNONE) {
				doDebug();
			} else if (simulation.getEventState() == Simulation.EVENTS_TIMESTEP) {
				if (eventStack.get(0).eventTime != debuggingTime) {
					doDebug();
				}
			} else if (simulation.getEventState() == Simulation.EVENTS_UNTILTIME) {
				if (eventStack.get(0).eventTime >= debuggingTime) {
					doDebug();
				}
			}
		}
	}

	private void updateTime(long nextTime) {
		if (this.getExecuteRealtime()) {
			// Account for pausing the model/restarting
			if (previousInternalTime == -1) {
				previousInternalTime = nextTime;
				previousWallTime = System.currentTimeMillis();
			}

			// Calculate number of milliseconds between events
			long millisToWait = (long)((((nextTime - previousInternalTime) / Simulation.getSimTimeFactor()) *
								3600 * 1000) / simulation.getRealTimeFactor());

			long targetMillis = previousWallTime + millisToWait;
			long currentWallTime = 0;
			// cache the internal time and the current RealTimeFactor in case they
			// are written while doing the pause
			long prevIntTime = previousInternalTime;
			double realTimeFact = simulation.getRealTimeFactor();
			while ((currentWallTime = System.currentTimeMillis()) < targetMillis) {
				this.threadPause(20);
				long modelHours = (long)((currentWallTime - previousWallTime) * realTimeFact / 3600000.0d * Simulation.getSimTimeFactor());
				modelHours += prevIntTime;
				if (modelHours < nextTime)
					simulation.updateTime(modelHours / Simulation.getSimTimeFactor());

				// If realtime was disabled, break out
				if (!this.getExecuteRealtime() || previousInternalTime == -1)
					break;
			}
		}

		simulation.updateTime(nextTime / Simulation.getSimTimeFactor());
	}

	/**
	 * Called when a process has finished invoking a model method and unwinds
	 * the threadStack one level.
	 */
	void releaseProcess() {
		traceProcess(null, null);
		synchronized (lockObject) {
			assertNotWaitUntil();
			Process next = Process.currentProcess().getNextProcess();

			if (next != null) {
				next.interrupt();
			} else {
				// TODO: check for the switching of eventmanagers
				Process.currentProcess().getEventManager().eventManagerThread.interrupt();
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
			Process next = Process.currentProcess().getNextProcess();

			Process.currentProcess().clearFlag(Process.ACTIVE);
			if (next != null) {
				Process.currentProcess().setNextProcess(null);
				switchThread(next);
			} else {
				// TODO: check for the switching of eventmanagers
				switchThread(Process.currentProcess().getEventManager().eventManagerThread);
			}
			Process.currentProcess().setFlag(Process.ACTIVE);
			Process.currentProcess().setEventManager(this);
			if (Process.currentProcess().testFlag(Process.TERMINATE)) {
				throw new ThreadKilledException("Thread killed");
			}
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

	private void raw_scheduleWait(long waitLength, int eventPriority, Entity caller) {
		assertNotWaitUntil();
		if (!Process.currentProcess().getEventManager().isParentOf(this)) {
			System.out.format("Crossing eventManager boundary dst:%s src:%s\n",
					name, Process.currentProcess().getEventManager().name);
			long time = Process.currentProcess().getEventManager().currentTime() + waitLength;
			if (eventStack.size() > 0 && eventStack.get(0).eventTime > time)
				System.out.format("Next Event:%d This Event:%d\n", eventStack.get(0).eventTime, time);
		}

		long nextEventTime = calculateEventTime(Process.currentTime(), waitLength);

		Event temp = new Event(nextEventTime, eventPriority, caller, Process.currentProcess());
		Process.currentProcess().getEventManager().traceEvent(temp, STATE_WAITING);
		addEventToStack(temp);
		popThread();
	}

	private void raw_scheduleProcess(long waitLength, int eventPriority, Entity caller, String methodName, Object[] args) {
		assertNotWaitUntil();
		// Take a process from the pool
		Process newProcess = Process.allocate(this, caller, methodName, args);
		long eventTime = calculateEventTime(Process.currentTime(), waitLength);

		// Create an event for the new process at the present time, and place it on the event stack
		Event newEvent = new Event(eventTime, eventPriority, caller, newProcess);
		Process.currentProcess().getEventManager().traceSchedProcess(newEvent);
		addEventToStack(newEvent);
	}

	void scheduleProcess(long waitLength, int eventPriority, Entity caller, String methodName, Object[] args) {
		raw_scheduleProcess(waitLength, eventPriority, caller, methodName, args);
	}

	void scheduleSingleProcess(long waitLength, int eventPriority, Entity caller, String methodName, Object[] args) {
		long eventTime = calculateEventTime(Process.currentTime(), waitLength);

		synchronized (lockObject) {
			for (int i = 0; i < eventStack.size(); i++) {
				Event each = eventStack.get(i);
				// We passed where any duplicate could be, break out to the
				// insertion part
				if (each.eventTime > eventTime)
					break;

				// if we have an exact match, do not schedule another event
				if (each.eventTime == eventTime && each.priority == eventPriority && each.caller == caller && each.getClassMethod().endsWith(methodName)) {
					//System.out.println("Suppressed duplicate event:" + Process.currentProcess().getEventManager().currentLongTime);
					Process.currentProcess().getEventManager().traceSchedProcess(each);
					return;
				}
			}
			raw_scheduleProcess(waitLength, eventPriority, caller, methodName, args);
		}
	}

	/**
	 * Schedules a future event to occur with a given priority.  Lower priority
	 * events will be executed preferentially over higher priority.  This is
	 * by lower priority events being placed higher on the event stack.
	 * @param waitLength the length of time from now to schedule the event.
	 * @param eventPriority the priority of the scheduled event: 1 is the highest priority (default is priority 5)
	 */
	void scheduleWait(long waitLength, int eventPriority, Entity caller) {

		// Test for zero duration scheduled wait length
		if (waitLength == 0) {
			return;
		}

		// Test for negative duration schedule wait length
		if(waitLength < 0) {
			throw new ErrorException("Negative duration wait is invalid (wait length specified to be %d )", waitLength);
		}

		raw_scheduleWait(waitLength, eventPriority, caller);
	}

	void scheduleLastFIFO( Entity caller ) {
		raw_scheduleWait(0, PRIO_LASTFIFO, caller);
	}

	/**
	 * Schedules an event to happen as the last event at the current time in LIFO.
	 * Additional calls to scheduleLast will place a new event as the last event,
	 * in fron of other 'last' events.
	 */
	void scheduleLastLIFO( Entity caller ) {
		raw_scheduleWait(0, PRIO_LASTLIFO, caller);
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
			if (newEvent.eventTime < currentTime) {
				System.out.println("Time travel detected - whoops");
				throw new ErrorException("Going back in time");
			}

			int i;
			// skip all event that happen before the new event
			for (i = 0; i < eventStack.size(); i++) {
				if (eventStack.get(i).eventTime < newEvent.eventTime) {
					continue;
				} else {
					break;
				}
			}

			// skip all events at an equal time that have higher priority
			for (; i < eventStack.size(); i++) {
				// next stack event happens at a later time, i is the insertion index
				if (eventStack.get(i).eventTime > newEvent.eventTime) {
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
		Process process = Process.currentProcess();
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
	void waitUntil(Entity caller) {
		synchronized (lockObject) {
			if (!conditionalList.contains(Process.currentProcess())) {
				Process.currentProcess().getEventManager().traceWaitUntil(0);
				Process.currentProcess().setFlag(Process.COND_WAIT);
				conditionalList.add(Process.currentProcess());
			}
		}
		popThread();
	}

	void waitUntilEnded(Entity caller) {
		synchronized (lockObject) {
			if (!conditionalList.remove(Process.currentProcess())) {
				// Do not wait at all if we never actually were on the waitUntilStack
				// ie. we never called waitUntil
				return;
			} else {
				traceWaitUntil(1);
				Process.currentProcess().clearFlag(Process.COND_WAIT);
				scheduleLastFIFO(caller);
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
					interruptEvent.process.setNextProcess(Process.currentProcess());
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
		if (currentViewer != null)
			currentViewer.addRetiredEvent(retired.getData(reason), reason);

		traceEvent(retired, reason);
	}

	long currentTime() {
		return currentTime;
	}

	boolean getExecuteRealtime() {
		return executeRealTime;
	}

	void setExecuteRealTime(boolean useRealTime) {
		executeRealTime = useRealTime;
		if (useRealTime)
			previousInternalTime = -1;
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

	/**
	 * This method will create a new process from outside the simulation.
	 * For example, it is used when pressing F5 to start an avi capture.
	 */
	void startExternalProcess(Entity target, String methodName, Object[] arguments) {
		// Take a process from the pool
		Process newProcess = Process.allocate(this, target, methodName, arguments);

		// Create an event for the new process at the present time, and place it on the event stack
		Event newEvent = new Event(currentTime, PRIO_DEFAULT, target, newProcess);
		this.traceSchedProcess(newEvent);
		addEventToStack(newEvent);
	}

	/**
	 * Sets the value that is tested in the doProcess loop to determine if the
	 * next event should be executed.  If set to false, the eventManager will
	 * execute a threadWait() and wait until an interrupt is generated.  It is
	 * guaranteed in this state that there is an empty thread stack and the
	 * thread referenced in activeThread is the eventManager thread.
	 */
	void pause() {
		simulation.setEventState(Simulation.EVENTS_STOPPED);
	}

	/**
	 * Sets the value that is tested in the doProcess loop to determine if the
	 * next event should be executed.  Generates an interrupt of activeThread
	 * in case the eventManager thread has already been paused and needs to
	 * resume the event execution loop.  This prevents the model being resumed
	 * from an inconsistent state.
	 */
	void resume() {
		previousInternalTime = -1;
		if (simulation.getEventState() != Simulation.EVENTS_STOPPED)
			return;

		// cannot resume if viewing events
		if( currentViewer != null )
			return;

		synchronized( lockObject ) {
			simulation.setEventState(Simulation.EVENTS_RUNNING);
			eventManagerThread.interrupt();
		}
	}

	public void registerEventViewer( EventViewer ev ) {
		currentViewer = ev;
		wasPaused = (simulation.getEventState() == Simulation.EVENTS_STOPPED);
		pause();
	}

	public void unregisterEventViewer( EventViewer ev ) {
		currentViewer = null;
		if( !wasPaused )
			resume();
	}

	public void nextOneEvent() {
		simulation.setEventState(Simulation.EVENTS_RUNONE);
		startDebugging();
	}

	public void nextEventTime() {
		debuggingTime = eventStack.get(0).eventTime;
		simulation.setEventState(Simulation.EVENTS_TIMESTEP);
		startDebugging();
	}

	public void runToTime( double stopTime ) {
		// This is an ugly hack to make the runtotime functionality available before
		// the model has started....don't use this as an example
		if (simulation.getSimulationState() == Simulation.SIM_STATE_CONFIGURED)
			simulation.start();

		debuggingTime = ((long)(stopTime * Simulation.getSimTimeFactor()));
		simulation.setEventState(Simulation.EVENTS_UNTILTIME);
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
		if (!simulation.isTraceEnabled())
			return;

		traceRecord.addHeader(name, evt.eventTime);
		traceRecord.formatEventTrace(evt, reason);
		traceRecord.finish(simulation);
	}

	void traceProcess(Entity target, String methodName) {
		if (!simulation.isTraceEnabled())
			return;

		traceRecord.addHeader(name, currentTime);
		traceRecord.formatProcessTrace(target, methodName);
		traceRecord.finish(simulation);
	}

	private void traceSchedProcess(Event target) {
		if (!simulation.isTraceEnabled())
			return;

		traceRecord.addHeader(name, currentTime);
		traceRecord.formatSchedProcessTrace(target);
		traceRecord.finish(simulation);
	}

	private void traceWaitUntil(int reason) {
		if (!simulation.isTraceEnabled())
			return;

		traceRecord.addHeader(name, currentTime);
		traceRecord.formatWaitUntilTrace(reason);
		traceRecord.finish(simulation);
	}

	public String[] getViewerHeaders() {
		String[] headerNames = new String[10];

		headerNames[0] = "Event Time";
		headerNames[1] = "Simulation Time";
		headerNames[2] = "Priority";
		headerNames[3] = "Entity";
		headerNames[4] = "Region";
		headerNames[5] = "Description";
		headerNames[6] = "Class.Method";
		headerNames[7] = "File:Line";
		headerNames[8] = "Creation Time";
		headerNames[9] = "State";

		return headerNames;
	}

	public String[][] getViewerData() {
		synchronized (lockObject) {
			String[][] data = new String[eventStack.size()][];

			for (int i = 0; i < eventStack.size(); i++) {
				data[i] = eventStack.get(i).getData(EventManager.STATE_WAITING);
				if (i > 0 && eventStack.get(i).eventTime == eventStack.get(i - 1).eventTime) {
					data[i][0] = "";
					data[i][1] = "";
				}
			}
			return data;
		}
	}

	public String toString() {
		return name;
	}
}
