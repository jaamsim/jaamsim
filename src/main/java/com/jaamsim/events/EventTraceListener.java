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
 * @param tick - time for the next event in clock ticks
 * @param priority - priority of the event being executed
 * @param t - holds the method to be executed by the event
 */
public void traceEvent(long tick, int priority, ProcessTarget t);

/**
 * Called when a future event is scheduled by a 'wait' method.
 * @param tick - time for the next event in clock ticks
 * @param priority - priority of the event to be executed
 * @param t - holds the method to be executed by the event
 */
public void traceWait(long tick, int priority, ProcessTarget t);

/**
 * Called when a future event is scheduled by a 'schedule' or 'scheduleProcessExternal' method.
 * @param tick - time for the next event in clock ticks
 * @param priority - priority of the event to be executed
 * @param t - holds the method to be executed by the event
 */
public void traceSchedProcess(long tick, int priority, ProcessTarget t);

/**
 * Called when a new process is started by a 'startProcess' method.
 * @param t - holds the method to be executed by the process
 */
public void traceProcessStart(ProcessTarget t);

/**
 * Called when a process finishes execution.
 */
public void traceProcessEnd();

/**
 * Called at the start of execution for a future event that has been re-scheduled for immediate
 * execution.
 * @param tick - time for the next event in clock ticks
 * @param priority - priority of the event being executed
 * @param t - holds the method to be executed by the event
 */
public void traceInterrupt(long tick, int priority, ProcessTarget t);

/**
 * Called when a future event is cancelled.
 * @param tick - time for the next event in clock ticks
 * @param priority - priority of the event being cancelled
 * @param t - holds the method to be executed by the cancelled event
 */
public void traceKill(long tick, int priority, ProcessTarget t);

/**
 * Called when a conditional event has been scheduled by a 'waitUntil' method.
 */
public void traceWaitUntil();

/**
 * Called when a conditional event has been scheduled by a 'scheduleUntil' method.
 * @param e - event processor
 * @param tick - present simulation time in clock ticks
 */
public void traceSchedUntil(ProcessTarget t);

/**
 * Called when a conditional event's condition is evaluated.
 * @param t - holds the method to be executed by the conditional event
 */
public void traceConditionalEval(ProcessTarget t);

/**
 * Called when the evaluation of conditional event's condition has finished.
 * @param wakeup - true if the conditional event is now scheduled after evaluating the condition
 * @param t - holds the method to be executed by the conditional event
 */
public void traceConditionalEvalEnded(boolean wakeup, ProcessTarget t);

}
