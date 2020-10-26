/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2021 JaamSim Software Inc.
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

public interface EventTimeListener {

	/**
	 * Called when the simulation time is advanced by the next event.
	 * @param tick - present simulation time in clock ticks
	 */
	public void tickUpdate(long tick);

	/**
	 * Called when the simulation run is started, resumed, or paused.
	 */
	public void timeRunning();

	/**
	 * Called when a runtime error is encountered.
	 * @param t - error condition
	 */
	public void handleError(Throwable t);

}
