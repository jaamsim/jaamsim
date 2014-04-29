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

/**
 * Holder class for event data used by the event monitor to schedule future
 * events.
 */
public class Event {
	long addedTick; // The tick at which this event was queued to execute
	long schedTick; // The tick at which this event will execute
	int priority;   // The schedule priority of this event

	final ProcessTarget target;

	/**
	 * Constructs a new event object.
	 * @param currentTick the current simulation tick
	 * @param scheduleTick the simulation tick the event is schedule for
	 * @param prio the event priority for scheduling purposes
	 * @param caller
	 * @param process
	 */
	Event(long currentTick, long scheduleTick, int prio, ProcessTarget target) {
		addedTick = currentTick;
		schedTick = scheduleTick;
		priority = prio;

		this.target = target;
	}

	/**
	 * Returns the tick from when this event was created.
	 * @return
	 */
	public long getCreationTick() {
		return addedTick;
	}

	/**
	 * Returns the tick this event was scheduled to execute at.
	 * @return
	 */
	public long getScheduledTick() {
		return schedTick;
	}

	/**
	 * Returns the priority this event was scheduled at.
	 * @return
	 */
	public int getScheduledPriority() {
		return priority;
	}

	public String getDesc() {
		return target.getDescription();
	}
}
