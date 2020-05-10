/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017 JaamSim Software Inc.
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

public class EventData {

	public final long ticks;
	public final int priority;
	public final String description;

	public EventData(long tk, int pri, String desc) {
		ticks = tk;
		priority = pri;
		description = desc;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj == this) return true;
		if (!(obj instanceof EventData)) return false;
		EventData data = (EventData) obj;
		return (ticks == data.ticks) && (priority == data.priority)
				&& description.equals(data.description);
	}

	@Override
	public String toString() {
		return String.format("[%s, %s, %s]", ticks, priority, description);
	}

}
