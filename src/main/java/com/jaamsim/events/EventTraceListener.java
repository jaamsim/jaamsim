/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
