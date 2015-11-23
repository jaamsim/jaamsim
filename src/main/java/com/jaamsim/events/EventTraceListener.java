/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

public void traceEvent(EventManager e, long curTick, long tick, int priority, ProcessTarget t);

public void traceWait(EventManager e, long curTick, long tick, int priority, ProcessTarget t);

public void traceSchedProcess(EventManager e, long curTick, long tick, int priority, ProcessTarget t);

public void traceProcessStart(EventManager e, ProcessTarget t, long tick);
public void traceProcessEnd(EventManager e, long tick);

public void traceInterrupt(EventManager e, long curTick, long tick, int priority, ProcessTarget t);
public void traceKill(EventManager e, long curTick, long tick, int priority, ProcessTarget t);

public void traceWaitUntil(EventManager e, long tick);
public void traceWaitUntilEnded(EventManager e, long tick, ProcessTarget t);

}
