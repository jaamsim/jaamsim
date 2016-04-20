/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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


public interface StateEntityListener {

	/**
	 * Returns true if this object is monitoring the specified entity's state.
	 * @param ent - the specified entity
	 * @return true if the specified entity is being monitored
	 */
	public boolean isWatching(StateEntity ent);

	/**
	 * Indicates that the specified entity has changed state.
	 * @param ent - the specified entity
	 * @param prev - old state for the specified entity
	 * @param next - new state for the specified entity
	 */
	public void updateForStateChange(StateEntity ent, StateRecord prev, StateRecord next);
}
