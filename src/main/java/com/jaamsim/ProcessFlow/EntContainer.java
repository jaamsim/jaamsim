/*
 * JaamSim Discrete Event Simulation
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
package com.jaamsim.ProcessFlow;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.states.StateUser;

/**
 * Provides a common interface for objects that store entities.
 * @author Harry King
 *
 */
public interface EntContainer extends StateUser {

	/**
	 * Adds the specified entity to the container.
	 */
	public void addEntity(DisplayEntity ent);

	/**
	 * Removes the first entity of the specified type in the container.
	 * If the type is null, then the first entity is removed regardless of type.
	 * string.
	 * @param type - entity type
	 * @return entity that was removed
	 */
	public DisplayEntity removeEntity(String type);

	/**
	 * Returns the number of entities of the specified type in the container.
	 * If the type is null, then all entities are counted.
	 * @param type - entity type
	 * @return number of entities of the specified type
	 */
	public int getCount(String type);

	/**
	 * Returns whether the container has any entities of the specified type.
	 * If the type is null, then all entities are considered.
	 * @param type - entity type
	 * @return true if there is at least one entity of the specified type
	 */
	public boolean isEmpty(String type);

}
