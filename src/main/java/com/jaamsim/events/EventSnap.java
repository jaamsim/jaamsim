/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2026 JaamSim Software Inc.
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

/**
 * An immutable record of one pending event, captured for checkpointing.
 * <p>
 * Unlike EventData, which stringifies the target for display in the Event Viewer,
 * EventSnap retains the ProcessTarget and EventHandle references so that the event
 * can be identified against the object that owns it and later re-created.
 */
public class EventSnap {

	/** Value used for 'ticks' when the event is conditional rather than scheduled. */
	public static final long NO_TICK = -1L;

	/** Value used for 'priority' when the event is conditional rather than scheduled. */
	public static final int NO_PRIORITY = -1;

	public final long ticks;
	public final int priority;
	public final ProcessTarget target;
	public final EventHandle handle;

	/** True if the event is a conditional event rather than a scheduled one. */
	public final boolean conditional;

	/**
	 * True if the target is a captured Process, i.e. a thread parked inside
	 * waitTicks, waitSeconds, or waitUntil. The continuation for such an event is a
	 * live Java call stack that cannot be written to a checkpoint file.
	 */
	public final boolean captured;

	EventSnap(long tk, int pri, ProcessTarget targ, EventHandle hand, boolean cond) {
		ticks = tk;
		priority = pri;
		target = targ;
		handle = hand;
		conditional = cond;
		captured = (targ instanceof WaitTarget);
	}

	/**
	 * Returns the class name of the target, which is the first thing needed to
	 * decide how the target can be re-created.
	 */
	public String getTargetClassName() {
		if (target == null)
			return "null";
		return target.getClass().getName();
	}

	public String getDescription() {
		if (target == null)
			return "null";
		return target.getDescription();
	}

	@Override
	public String toString() {
		if (conditional)
			return String.format("[conditional, %s]", getDescription());
		return String.format("[%s, %s, %s]", ticks, priority, getDescription());
	}

}
