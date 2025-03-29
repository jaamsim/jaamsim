/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017-2022 JaamSim Software Inc.
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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

	private final ReentrantLock evtLock; // Object used as global lock for synchronization

	private final EventTree eventTree;
	private final AtomicReference<ThreadEntry> runningProc;
	private final AtomicLong currentTick;
	private volatile boolean executeEvents;
	private boolean disableSchedule;

	private final ArrayList<ConditionalEvent> condEvents;

	private long nextTick; // The next tick to execute events at
	private long targetTick; // the largest time we will execute events for (run to time)
	private boolean oneEvent; // execute a single event
	private boolean oneSimTime; // execute all the events at the next simulation time

	private double ticksPerSecond; // The number of discrete ticks per simulated second
	private double secsPerTick;    // The length of time in seconds each tick represents

	private final AtomicReference<RealTimeState> rt;

	private EventTimeListener timelistener;
	private EventTraceListener trcListener;

	/**
	 * Allocates a new EventManager with the given parent and name
	 *
	 * @param name the name this EventManager should use
	 */
	public EventManager(String name) {
		// Basic initialization
		this.name = name;
		evtLock = new ReentrantLock();

		// Initialize and event lists and timekeeping variables
		currentTick = new AtomicLong(0);
		nextTick = 0;
		oneEvent = false;
		oneSimTime = false;

		setTickLength(1e-6d);

		eventTree = new EventTree();
		condEvents = new ArrayList<>();

		runningProc = new AtomicReference<>(null);
		executeEvents = false;
		disableSchedule = false;

		rt = new AtomicReference<>(null);
		setTimeListener(null);
	}

	public final void setTimeListener(EventTimeListener l) {
		evtLock.lock();
		try {
			if (l != null)
				timelistener = l;
			else
				timelistener = new NoopListener();
		}
		finally {
			evtLock.unlock();
		}
	}

	public final void setTraceListener(EventTraceListener l) {
		evtLock.lock();
		try {
			trcListener = l;
		}
		finally {
			evtLock.unlock();
		}
	}

	public void clear() {
		evtLock.lock();
		try {
			currentTick.set(0);
			nextTick = 0;
			oneEvent = false;
			oneSimTime = false;
			targetTick = Long.MAX_VALUE;
			RealTimeState state = rt.get();
			if (state != null)
				state.realTimeMillis = -1l;

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
		finally {
			evtLock.unlock();
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

	/**
	 * Executes the event logic from the ProcessTarget
	 *
	 * @param t
	 * @return true if the execution thread should exit the event loop
	 */
	private boolean executeTarget(ProcessTarget t) {
		try {
			// If the event has a captured process, pass control to it
			Thread p = t.getProcess();
			if (p != null) {
				pushProcess(t);
				return false;
			}

			// Execute the method
			t.process();

			// Notify the event manager that the process has been completed
			if (trcListener != null) {
				disableSchedule();
				trcListener.traceProcessEnd();
				enableSchedule();
			}

			ThreadEntry te = runningProc.get().next;
			if (te != null) {
				te.cond.signal();
				runningProc.set(te);
				return true;
			}
			return false;
		}
		catch (Throwable e) {
			// This is how kill() is implemented for sleeping processes.
			if (e instanceof ThreadKilledException)
				return true;

			// Tear down any threads waiting for this to finish
			ThreadEntry entries = runningProc.get().next;
			while (entries != null) {
				entries.dieFlag.set(true);
				entries.cond.signal();
				entries = entries.next;
			}
			executeEvents = false;
			runningProc.set(null);
			timelistener.handleError(e);
			return true;
		}
	}

	final Condition getWaitCondition() {
		return evtLock.newCondition();
	}

	/**
	 * Main event execution method the eventManager, this is the only entrypoint
	 * for Process objects taken out of the pool.
	 */
	final void execute() {
		evtLock.lock();
		try {
			if (runningProc.get().proc != Thread.currentThread()) {
				System.out.println("Invalid Process Entering EventManager:" + Thread.currentThread());
				return;
			}

			// This occurs in the startProcess or interrupt case where we start
			// a process with a target already assigned
			if (runningProc.get().target != null) {
				// This should always be true here, send a message out if not
				if (executeTarget(runningProc.get().target))
					return;

				System.out.println("Startprocess thread should always exit:" + Thread.currentThread());
				return;
			}

			// If we are resuming execution, have the first thread call the timeRunning callback and enable scheduling
			if (runningProc.get().start) {
				enableSchedule();
				timelistener.timeRunning();
			}

			// Loop continuously
			while (true) {
				EventNode nextNode = eventTree.getNextNode();
				if (nextNode == null ||
				    currentTick.get() >= targetTick) {
					executeEvents = false;
				}

				if (!executeEvents) {
					runningProc.set(null);
					timelistener.timeRunning();
					return;
				}

				// If the next event is at the current tick, execute it
				if (nextNode.schedTick == currentTick.get()) {
					// Remove the event from the future events
					Event nextEvent = nextNode.head;
					ProcessTarget nextTarget = nextEvent.target;
					if (trcListener != null) {
						disableSchedule();
						trcListener.traceEvent(nextNode.schedTick, nextNode.priority, nextTarget);
						enableSchedule();
					}

					removeEvent(nextEvent);

					if (oneEvent) {
						oneEvent = false;
						executeEvents = false;
					}

					if (executeTarget(nextTarget))
						return;

					continue;
				}

				// If the next event would require us to advance the time, check the
				// conditonal events
				if (eventTree.getNextNode().schedTick > nextTick) {
					if (condEvents.size() > 0) {
						evaluateConditions();
						if (!executeEvents) continue;
					}

					// If a conditional event was satisfied, we will have a new event at the
					// beginning of the eventStack for the current tick, go back to the
					// beginning, otherwise fall through to the time-advance
					nextTick = eventTree.getNextNode().schedTick;
					if (nextTick == currentTick.get())
						continue;

					// If 'Next Time' button was clicked, then stop without advancing time
					if (oneSimTime) {
						executeEvents = false;
						oneSimTime = false;
						continue;
					}
				}

				// Advance to the next event time
				if (rt.get() != null && this.waitForNextRealTick())
					continue;

				// advance time
				if (targetTick < nextTick)
					currentTick.set(targetTick);
				else
					currentTick.set(nextTick);

				timelistener.tickUpdate(currentTick.get());
			}
		}
		finally {
			evtLock.unlock();
		}
	}

	public final long getTicks() {
		return currentTick.get();
	}

	public final boolean isRunning() {
		return runningProc.get() != null;
	}

	private void evaluateConditions() {
		// Protecting the conditional evaluate() callbacks and the traceWaitUntilEnded callback
		disableSchedule();
		try {
			for (int i = 0; i < condEvents.size();) {
				ConditionalEvent c = condEvents.get(i);
				if (trcListener != null)
					trcListener.traceConditionalEval(c.target);
				boolean bool = c.c.evaluate();
				if (trcListener != null)
					trcListener.traceConditionalEvalEnded(bool, c.target);
				if (bool) {
					condEvents.remove(i);
					EventNode node = getEventNode(currentTick.get(), 0);
					Event evt = getEvent();
					evt.node = node;
					evt.target = c.target;
					evt.handle = c.handle;
					if (evt.handle != null) {
						// no need to check the handle.isScheduled as we just unscheduled it above
						// and we immediately switch it to this event
						evt.handle.event = evt;
					}
					node.addEvent(evt, true);
					continue;
				}
				i++;
			}
		}
		catch (Throwable e) {
			executeEvents = false;
			runningProc.set(null);
			timelistener.handleError(e);
		}

		enableSchedule();
	}

	public final void setExecuteRealTime(boolean useRealTime, double factor) {
		if (!useRealTime) {
			rt.set(null);
			return;
		}

		RealTimeState s = new RealTimeState(factor);
		rt.set(s);
	}

	private static class RealTimeState {
		final double realTimeFactor;
		long realTimeMillis;
		long realTimeTick;

		RealTimeState(double factor) {
			realTimeFactor = factor;
			realTimeMillis = -1;
			realTimeTick = -1;
		}
	}
	/**
	 * Introduce a pause if needed for real time.
	 * @return true if we introduced a real-time wait
	 */
	private boolean waitForNextRealTick() {
		RealTimeState state = rt.get();
		if (state == null)
			return false;

		long curMS = System.currentTimeMillis();
		if (state.realTimeMillis == -1l) {
			state.realTimeTick = currentTick.get();
			state.realTimeMillis = curMS;
		}

		double simElapsedsec = ((curMS - state.realTimeMillis) * state.realTimeFactor) / 1000.0d;
		long simElapsedTicks = secondsToNearestTick(simElapsedsec);

		long realTick = state.realTimeTick + simElapsedTicks;
		if (realTick < nextTick && realTick < targetTick) {
			// Update the displayed simulation time
			currentTick.set(realTick);
			timelistener.tickUpdate(currentTick.get());
			//Halt the thread for 20ms and then reevaluate the loop
			try { runningProc.get().cond.awaitNanos(20000000); } catch( InterruptedException e ) {}
			return true;
		}

		return false;
	}

	/**
	 * Pass control to a thread when starting or interrupting a event, wait on
	 * the condition in the currently running ThreadEntry.
	 */
	private void pushProcess(ProcessTarget t) {
		Thread proc = t.getProcess();
		ThreadEntry te = runningProc.get();
		if (proc == null) {
			runningProc.set(new ThreadEntry(this, this.allocateThread(), te, t));
		}
		else {
			runningProc.set(new ThreadEntry(this, proc, te, null));
			((WaitTarget)t).eventWake();
		}

		/*
		 * Halt the thread and only wake up by being interrupted.
		 *
		 * The infinite loop is _absolutely_ necessary to prevent
		 * spurious wakeups from waking us early....which causes the
		 * model to get into an inconsistent state causing crashes.
		 */
		while (true) {
			te.cond.awaitUninterruptibly();
			if (te.dieFlag.get())
				throw new ThreadKilledException("Thread killed");

			if (runningProc.get().proc == Thread.currentThread())
				break;

			System.out.println("Spurious wakeup in EventManager eventStack." + Thread.currentThread());
		}
	}

	/**
	 * Capture a running thread in a wait or conditional wait using the condition in the
	 * WaitTarget argument. Signal the next thread in the stack to continue running or allocate
	 * a new thread if the event thread is being captured.
	 */
	private void captureProcess(WaitTarget t) {
		// if we don't wake a new process, take one from the pool
		ThreadEntry next = runningProc.get().next;
		if (next == null) {
			next = new ThreadEntry(this, false);
		}
		else {
			next.cond.signal();
		}

		runningProc.set(next);
		/*
		 * Halt the thread and only wake up by being interrupted.
		 *
		 * The infinite loop is _absolutely_ necessary to prevent
		 * spurious wakeups from waking us early....which causes the
		 * model to get into an inconsistent state causing crashes.
		 */
		while (true) {
			t.cond.awaitUninterruptibly();
			if (t.dieFlag.get())
				throw new ThreadKilledException("Thread killed");

			if (runningProc.get().proc == Thread.currentThread())
				break;

			System.out.println("Spurious wakeup in EventManager wait." + Thread.currentThread());
		}
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
		long nextEventTime = currentTick.get() + waitLength;
		if (nextEventTime < 0)
			nextEventTime = Long.MAX_VALUE;

		return nextEventTime;
	}

	/**
	 * Pause the execution of the current Process and schedule it to wake up at a future
	 * time in the controlling EventManager,
	 * @param ticks the number of ticks in the future to wake at
	 * @param priority the priority of the scheduled wakeup event
	 * @param fifo break ties with previously scheduled events using FIFO/LIFO ordering
	 * @param handle an optional handle to hold onto the scheduled event
	 * @throws ProcessError if called outside of a Process context
	 */
	public static final void waitTicks(long ticks, int priority, boolean fifo, EventHandle handle) {
		EventManager.current()._waitTicks(ticks, priority, fifo, handle);
	}

	/**
	 * Pause the execution of the current Process and schedule it to wake up at a future
	 * time in the controlling EventManager,
	 * @param secs the number of seconds in the future to wake at
	 * @param priority the priority of the scheduled wakeup event
	 * @param fifo break ties with previously scheduled events using FIFO/LIFO ordering
	 * @param handle an optional handle to hold onto the scheduled event
	 * @throws ProcessError if called outside of a Process context
	 */
	public static final void waitSeconds(double secs, int priority, boolean fifo, EventHandle handle) {
		EventManager evt = EventManager.current();
		long ticks = evt.secondsToNearestTick(secs);
		evt._waitTicks(ticks, priority, fifo, handle);
	}

	/**
	 * Schedules a future event to occur with a given priority.  Lower priority
	 * events will be executed preferentially over higher priority.  This is
	 * by lower priority events being placed higher on the event stack.
	 * @param ticks the number of discrete ticks from now to schedule the event.
	 * @param priority the priority of the scheduled event: 1 is the highest priority (default is priority 5)
	 */
	private void _waitTicks(long ticks, int priority, boolean fifo, EventHandle handle) {
		assertCanSchedule();
		long nextEventTime = calculateEventTime(ticks);
		WaitTarget t = new WaitTarget(this);
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

		if (trcListener != null) {
			disableSchedule();
			trcListener.traceWait(nextEventTime, priority, t);
			enableSchedule();
		}
		node.addEvent(evt, fifo);
		captureProcess(t);
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
		EventManager.current()._waitUntil(cond, handle);
	}

	/**
	 * Used to achieve conditional waits in the simulation.  Adds the calling
	 * thread to the conditional stack, then wakes the next waiting thread on
	 * the thread stack.
	 */
	private void _waitUntil(Conditional cond, EventHandle handle) {
		assertCanSchedule();
		WaitTarget t = new WaitTarget(this);
		ConditionalEvent evt = new ConditionalEvent(cond, t, handle);
		if (handle != null) {
			if (handle.isScheduled())
				throw new ProcessError("Tried to waitUntil using a handle already in use");
			handle.event = evt;
		}
		condEvents.add(evt);
		if (trcListener != null) {
			disableSchedule();
			trcListener.traceWaitUntil();
			enableSchedule();
		}
		captureProcess(t);
	}

	public static final void scheduleUntil(ProcessTarget t, Conditional cond, EventHandle handle) {
		EventManager.current()._schedUntil(t, cond, handle);
	}

	private void _schedUntil(ProcessTarget t, Conditional cond, EventHandle handle) {
		assertCanSchedule();
		ConditionalEvent evt = new ConditionalEvent(cond, t, handle);
		if (handle != null) {
			if (handle.isScheduled())
				throw new ProcessError("Tried to scheduleUntil using a handle already in use");
			handle.event = evt;
		}
		condEvents.add(evt);
		if (trcListener != null) {
			disableSchedule();
			trcListener.traceSchedUntil(t);
			enableSchedule();
		}
	}

	public static final void startProcess(ProcessTarget t) {
		EventManager.current()._startProcess(t);
	}

	private void _startProcess(ProcessTarget t) {
		assertCanSchedule();

		if (trcListener != null) {
			disableSchedule();
			trcListener.traceProcessStart(t);
			enableSchedule();
		}

		pushProcess(t);
	}

	/**
	 * Remove an event from the eventList, must hold the lockObject.
	 * @param idx
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
		BaseEvent base = handle.event;
		ProcessTarget t = base.target;
		handle.event = null;
		base.handle = null;
		if (base instanceof Event) {
			removeEvent((Event)base);
		}
		else {
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
		EventManager.current()._killEvent(handle);
	}

	/**
	 *	Removes an event from the pending list without executing it.
	 */
	private void _killEvent(EventHandle handle) {
		assertCanSchedule();

		// no handle given, or Handle was not scheduled, nothing to do
		if (handle == null || handle.event == null)
			return;

		if (trcListener != null) {
			disableSchedule();
			trcKill(handle.event);
			enableSchedule();
		}
		ProcessTarget t = rem(handle);

		t.kill();
	}

	private void trcKill(BaseEvent event) {
		if (event instanceof Event) {
			EventNode node = ((Event)event).node;
			trcListener.traceKill(node.schedTick, node.priority, event.target);
		}
		else {
			trcListener.traceKill(-1, -1, event.target);
		}
	}

	/**
	 * Interrupts the event held in the EventHandle and immediately runs the ProcessTarget.
	 * If the handle does not currently hold a scheduled event, this method simply returns.
	 * @throws ProcessError if called outside of a Process context
	 */
	public static final void interruptEvent(EventHandle handle) {
		EventManager.current()._interruptEvent(handle);
	}

	/**
	 *	Removes an event from the pending list and executes it.
	 */
	private void _interruptEvent(EventHandle handle) {
		assertCanSchedule();

		// no handle given, or Handle was not scheduled, nothing to do
		if (handle == null || handle.event == null)
			return;

		if (trcListener != null) {
			disableSchedule();
			trcInterrupt(handle.event);
			enableSchedule();
		}
		ProcessTarget t = rem(handle);
		pushProcess(t);
	}

	private void trcInterrupt(BaseEvent event) {
		if (event instanceof Event) {
			EventNode node = ((Event)event).node;
			trcListener.traceInterrupt(node.schedTick, node.priority, event.target);
		}
		else {
			trcListener.traceInterrupt(-1, -1, event.target);
		}
	}

	private static class ThreadEntry {
		final ThreadEntry next;
		final Thread proc;
		final Condition cond;
		final AtomicBoolean dieFlag;
		final ProcessTarget target;
		final boolean start;

		ThreadEntry(EventManager evt, boolean startup) {
			next = null;
			proc = evt.allocateThread();
			cond = evt.getWaitCondition();
			dieFlag = new AtomicBoolean(false);
			target = null;
			start = startup;
		}

		ThreadEntry(EventManager evt, Thread p, ThreadEntry nxt, ProcessTarget t) {
			next = nxt;
			proc = p;
			cond = evt.getWaitCondition();
			dieFlag = new AtomicBoolean(false);
			target = t;
			start = false;
		}
	}

	public void scheduleProcessExternal(long waitLength, int eventPriority, boolean fifo, ProcessTarget t, EventHandle handle) {
		evtLock.lock();
		try {
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
			// FIXME: this is the only callback that does not occur in Process context, disable for now
			//if (trcListener != null)
			//	trcListener.traceSchedProcess(this, currentTick.get(), schedTick, eventPriority, t);
			node.addEvent(evt, fifo);

			// During real-time waits an event can be inserted becoming the next event to execute
			// If nextTick is not updated, we can fall through the entire time update code and not
			// execute this event, leading to the state machine becoming broken
			if (nextTick > eventTree.getNextNode().schedTick)
				nextTick = eventTree.getNextNode().schedTick;
		}
		finally {
			evtLock.unlock();
		}
	}

	/**
	 * Schedule a future event in the controlling EventManager for the current Process.
	 *
	 * @param waitLength the number of ticks in the future to schedule this event
	 * @param eventPriority the priority of the scheduled event
	 * @param fifo break ties with previously scheduled events using FIFO/LIFO ordering
	 * @param t the process target to run when the event is executed
	 * @param handle an optional handle to hold onto the scheduled event
	 * @throws ProcessError if called outside of a Process context
	 */
	public static final void scheduleTicks(long waitLength, int eventPriority, boolean fifo, ProcessTarget t, EventHandle handle) {
		EventManager.current()._scheduleTicks(waitLength, eventPriority, fifo, t, handle);
	}

	/**
	 * Schedule a future event in the controlling EventManager for the current Process.
	 *
	 * @param secs the number of seconds in the future to schedule this event
	 * @param eventPriority the priority of the scheduled event
	 * @param fifo break ties with previously scheduled events using FIFO/LIFO ordering
	 * @param t the process target to run when the event is executed
	 * @param handle an optional handle to hold onto the scheduled event
	 *
	 * @throws ProcessError if called outside of a Process context
	 */
	public static final void scheduleSeconds(double secs, int eventPriority, boolean fifo, ProcessTarget t, EventHandle handle) {
		EventManager evt = EventManager.current();
		long ticks = evt.secondsToNearestTick(secs);
		evt._scheduleTicks(ticks, eventPriority, fifo, t, handle);
	}

	private void _scheduleTicks(long waitLength, int eventPriority, boolean fifo, ProcessTarget t, EventHandle handle) {
		assertCanSchedule();
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
		if (trcListener != null) {
			disableSchedule();
			trcListener.traceSchedProcess(schedTick, eventPriority, t);
			enableSchedule();
		}
		node.addEvent(evt, fifo);
	}

	/**
	 * Sets the value that is tested in the doProcess loop to determine if the
	 * next event should be executed.  If set to false, the eventManager will
	 * execute a threadWait() and wait until an interrupt is generated.  It is
	 * guaranteed in this state that there is an empty thread stack and the
	 * thread referenced in activeThread is the eventManager thread.
	 */
	public final void pause() {
		executeEvents = false;
	}

	public final void resumeSeconds(double simTime) {
		this.resumeTicks(this.secondsToNearestTick(simTime), false, false);
	}

	public final void resumeSeconds(double simTime, boolean doOneEvent, boolean doOneTime) {
		this.resumeTicks(this.secondsToNearestTick(simTime), doOneEvent, doOneTime);
	}

	/**
	 * Sets the value that is tested in the doProcess loop to determine if the
	 * next event should be executed.  Generates an interrupt of activeThread
	 * in case the eventManager thread has already been paused and needs to
	 * resume the event execution loop.  This prevents the model being resumed
	 * from an inconsistent state.
	 * @param targetTicks - clock ticks at which to pause
	 */
	public final void resumeTicks(long targetTicks, boolean doOneEvent, boolean doOneTime) {
		evtLock.lock();

		if (doOneEvent)
			oneEvent = true;

		if (doOneTime)
			oneSimTime = true;

		try {

			// Ignore the pause time if it has already been reached
			if (currentTick.get() < targetTicks)
				targetTick = targetTicks;
			else
				targetTick = Long.MAX_VALUE;

			RealTimeState state = rt.get();
			if (state != null)
				state.realTimeMillis = -1;;
			if (executeEvents)
				return;

			executeEvents = true;
			runningProc.set(new ThreadEntry(this, true));
		}
		finally {
			evtLock.unlock();
		}
	}

	@Override
	public String toString() {
		return name;
	}

	private Thread allocateThread() {
		return Process.allocate(this);
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
	 * Returns whether or not a future event can be scheduled from the present thread.
	 * @return true if a future event can be scheduled
	 */
	public static final boolean canSchedule() {
		return hasCurrent() && EventManager.current().scheduleEnabled();
	}

	/**
	 * Returns the controlling EventManager for the current Process.
	 * @throws ProcessError if called outside of a Process context
	 */
	public static final EventManager current() {
		try {
			return ((Process)Thread.currentThread()).evt();
		}
		catch (ClassCastException e) {
			throw new ProcessError("Non-process thread called Process.current()");
		}
	}

	/**
	 * Returns the current simulation tick for the current Process.
	 * @throws ProcessError if called outside of a Process context
	 */
	public static final long simTicks() {
		return EventManager.current().currentTick.get();
	}

	/**
	 * Returns the current simulation time in seconds for the current Process.
	 * @throws ProcessError if called outside of a Process context
	 */
	public static final double simSeconds() {
		return EventManager.current().getSeconds();
	}

	public final double getSeconds() {
		return currentTick.get() * secsPerTick;
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

	/**
	 * Apppend EventData objects to the provided list for all pending events.
	 * @param events List to append EventData objects to
	 */
	public final void getEventDataList(ArrayList<EventData> events) {
		// Unsynchronized for use by the Event Viewer
		EventDataBuilder lb = new EventDataBuilder(events);
		eventTree.runOnAllNodes(lb);
	}

	private static class EventDataBuilder implements EventNode.Runner {
		final ArrayList<EventData> eventDataList;

		EventDataBuilder(ArrayList<EventData> events) {
			eventDataList = events;
		}

		@Override
		public void runOnNode(EventNode node) {
			Event evt = node.head;
			while (evt != null) {
				long ticks = evt.node.schedTick;
				int pri = evt.node.priority;
				String desc = evt.target.getDescription();
				eventDataList.add(new EventData(ticks, pri, desc));
				evt = evt.next;
			}
		}
	}

	public final void getConditionalDataList(ArrayList<String> events) {
		for (ConditionalEvent cond : condEvents) {
			events.add(cond.target.getDescription());
		}
	}

	private void disableSchedule() {
		disableSchedule = true;
	}

	private void enableSchedule() {
		disableSchedule = false;
	}

	private void assertCanSchedule() {
		if (disableSchedule)
			throw new ProcessError("Event Control attempted from inside a user callback");
	}

	private boolean scheduleEnabled() {
		return !disableSchedule;
	}
}
