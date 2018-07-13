/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018 JaamSim Software Inc.
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
package com.jaamsim.states;

public class StateRecord {
	private final String name;
	private final boolean working;
	private long initTicks;
	private long totalTicks;
	private long completedCycleTicks;
	private long currentCycleTicks;
	private long startTick;  // clock ticks at which the entity was last set to this state

	StateRecord(String state, boolean work) {
		name = state;
		working = work;
	}

	public void addTicks(long ticks) {
		totalTicks += ticks;
		currentCycleTicks += ticks;
	}

	public void finishWarmUp() {
		initTicks = totalTicks;
		totalTicks = 0L;
		completedCycleTicks = 0L;
	}

	public void finishCycle() {
		completedCycleTicks += currentCycleTicks;
		currentCycleTicks = 0L;
	}

	public void clearStats() {
		totalTicks = 0L;
		completedCycleTicks = 0L;
	}

	public void clearCurrentCycleStats() {
		currentCycleTicks = 0L;
	}

	public String getName() {
		return name;
	}

	public boolean isWorking() {
		return working;
	}

	public void setStartTick(long tick) {
		startTick = tick;
	}

	public long getStartTick() {
		return startTick;
	}

	public long getInitTicks() {
		return initTicks;
	}

	public long getTotalTicks() {
		return totalTicks;
	}

	public long getCurrentCycleTicks() {
		return currentCycleTicks;
	}

	public long getCompletedCycleTicks() {
		return completedCycleTicks;
	}

	@Override
	public String toString() {
		return name;
	}
}
