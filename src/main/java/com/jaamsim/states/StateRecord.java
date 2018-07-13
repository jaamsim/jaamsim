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
	public final String name;
	private long initTicks;
	long totalTicks;
	long completedCycleTicks;
	long currentCycleTicks;
	long startTick;
	public final boolean working;

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

	public long getStartTick() {
		return startTick;
	}

	public long getInitTicks() {
		return initTicks;
	}

	@Override
	public String toString() {
		return name;
	}
}
