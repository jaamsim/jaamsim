/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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


public interface EventTraceListener {

/**
 * Called at the start of execution for an event.
 * @param e - event processor
 * @param curTick - present simulation time in clock ticks
 * @param tick - time for the next event in clock ticks
 * @param priority - priority of the event being executed
 * @param t - holds the method to be executed by the event
 */
public void traceEvent(EventManager e, long curTick, long tick, int priority, ProcessTarget t);

/**
 * Called when a future event is scheduled by a 'wait' method.
 * @param e - event processor
 * @param curTick - present simulation time in clock ticks
 * @param tick - time for the next event in clock ticks
 * @param priority - priority of the event to be executed
 * @param t - holds the method to be executed by the event
 */
public void traceWait(EventManager e, long curTick, long tick, int priority, ProcessTarget t);

/**
 * Called when a future event is scheduled by a 'schedule' or 'scheduleProcessExternal' method.
 * @param e - event processor
 * @param curTick - present simulation time in clock ticks
 * @param tick - time for the next event in clock ticks
 * @param priority - priority of the event to be executed
 * @param t - holds the method to be executed by the event
 */
public void traceSchedProcess(EventManager e, long curTick, long tick, int priority, ProcessTarget t);

/**
 * Called when a new process is started by a 'startProcess' method.
 * @param e - event processor
 * @param t - holds the method to be executed by the process
 * @param tick - present simulation time in clock ticks
 */
public void traceProcessStart(EventManager e, ProcessTarget t, long tick);

/**
 * Called when a process finishes execution.
 * @param e - event processor
 * @param tick - present simulation time in clock ticks
 */
public void traceProcessEnd(EventManager e, long tick);

/**
 * Called at the start of execution for a future event that has been re-scheduled for immediate
 * execution.
 * @param e - event processor
 * @param curTick - present simulation time in clock ticks
 * @param tick - time for the next event in clock ticks
 * @param priority - priority of the event being executed
 * @param t - holds the method to be executed by the event
 */
public void traceInterrupt(EventManager e, long curTick, long tick, int priority, ProcessTarget t);

/**
 * Called when a future event is cancelled.
 * @param e - event processor
 * @param curTick - present simulation time in clock ticks
 * @param tick - time for the next event in clock ticks
 * @param priority - priority of the event being cancelled
 * @param t - holds the method to be executed by the cancelled event
 */
public void traceKill(EventManager e, long curTick, long tick, int priority, ProcessTarget t);

/**
 * Called when a conditional event has been scheduled by a 'scheduleUntil' or 'waitUntil' method.
 * @param e - event processor
 * @param tick - present simulation time in clock ticks
 */
public void traceWaitUntil(EventManager e, long tick);

/**
 * Called when a conditional event's condition has been satisfied and the event is scheduled for
 * execution.
 * @param e - event processor
 * @param tick - present simulation time in clock ticks
 * @param t - holds the method to be executed by the conditional event
 */
public void traceWaitUntilEnded(EventManager e, long tick, ProcessTarget t);

}
