/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2020 JaamSim Software Inc.
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
package com.jaamsim.resourceObjects;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;

public interface ResourceProvider {

	/**
	 * Tests whether the specified number of resource units are available to be assigned to the
	 * specified entity.
	 * @param simTime - present simulation time
	 * @param n - number of resource units to seize
	 * @param ent - entity that would seize the units
	 * @return true if the units can be seized
	 */
	public boolean canSeize(double simTime, int n, DisplayEntity ent);

	/**
	 * Assigns the specified number of resource units to the specified entity.
	 * @param n - number of resource units to assign
	 * @param ent - entity seizing the units
	 */
	public void seize(int n, DisplayEntity ent);

	/**
	 * Unassigns the specified number of resource units from the specified entity.
	 * If the number of units assigned to the entity is less than the specified number to release,
	 * then as many units are released as possible.
	 * @param n - number of resource units to release
	 * @param ent - entity releasing the units
	 */
	public void release(int n, DisplayEntity ent);

	/**
	 * Returns a list of objects that can seize this resource for use by an entity.
	 * @return users that can seize this resource
	 */
	public ArrayList<ResourceUser> getUserList();

	/**
	 * Returns whether the resource units are assigned to users strictly strictly on the basis of
	 * priority and waiting time. If true and this entity is unable to seize the resource because
	 * of other restrictions, then other entities with lower priority or shorter waiting time will
	 * NOT be allowed to seize the resource. If false, the entities will be tested in the same
	 * order of priority and waiting time, but the first entity that is able to seize the resource
	 * will be allowed to do so.
	 * @return true if resources are assigned in strict order
	 */
	public boolean isStrictOrder();

	/**
	 * Returns the total number of resource units that can be assigned.
	 * @param simTime - present simulation time
	 * @return total number of resource units
	 */
	public int getCapacity(double simTime);

	/**
	 * Returns the number of resource units that are assigned at the present time.
	 * @return resource units that are assigned
	 */
	public int getUnitsInUse();

}
