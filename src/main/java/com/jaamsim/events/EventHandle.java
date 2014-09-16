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
 * An EventHandle provides a means to remember a future scheduled event in order
 * to manage it's execution.  Examples of this control would be killing the event
 * or executing it earlier than otherwise scheduled.
 */
public class EventHandle {
	BaseEvent event = null;

	public EventHandle() {}

	/**
	 * Returns true if this handle is currently tracking a future event.
	 */
	public final boolean isScheduled() {
		return event != null;
	}
}
