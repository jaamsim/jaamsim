/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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

import com.jaamsim.BasicObjects.DowntimeEntity;

public interface DowntimeUser {
	public String getName();

	/**
	 * Returns true if the specified maintenance activity applies to this user.
	 * @param down - planned or unplanned maintenance activity
	 */
	public boolean isDowntimeUser(DowntimeEntity down);

	/**
	 * Returns true if the specified maintenance activity can be started.
	 * @param down - planned or unplanned maintenance activity
	 */
	public boolean canStartDowntime(DowntimeEntity down);

	/**
	 * Notifies the entity that the specified maintenance activity is about to begin and that
	 * any work in progress should be halted.
	 * @param down - planned or unplanned maintenance activity
	 */
	public void prepareForDowntime(DowntimeEntity down);

	/**
	 * Notifies the entity that the specified maintenance activity has started.
	 * @param down - planned or unplanned maintenance activity
	 */
	public void startDowntime(DowntimeEntity down);

	/**
	 * Notifies the entity that the specified maintenance activity has ended.
	 * @param down - planned or unplanned maintenance activity
	 */
	public void endDowntime(DowntimeEntity down);
}
