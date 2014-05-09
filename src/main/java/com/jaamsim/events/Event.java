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
	EventNode node;
	ProcessTarget target;
	EventHandle handle;
	Event next;

	/**
	 * Constructs a new event object.
	 * @param currentTick the current simulation tick
	 * @param scheduleTick the simulation tick the event is schedule for
	 * @param prio the event priority for scheduling purposes
	 * @param caller
	 * @param process
	 */
	Event(EventNode node, ProcessTarget target) {
		this.node = node;
		this.target = target;
		this.handle = null;
	}

	/**
	 * Returns the tick this event was scheduled to execute at.
	 * @return
	 */
	public long getScheduledTick() {
		return node.schedTick;
	}

	/**
	 * Returns the priority this event was scheduled at.
	 * @return
	 */
	public int getScheduledPriority() {
		return node.priority;
	}

	public String getDesc() {
		return target.getDescription();
	}
}
