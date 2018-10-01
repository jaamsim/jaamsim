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
package com.jaamsim.resourceObjects;

import com.jaamsim.Graphics.DisplayEntity;

public interface Seizable {

	/**
	 * Returns the resource pool that contains this seizable resource unit.
	 * @return pool containing this resource unit
	 */
	public ResourcePool getResourcePool();

	/**
	 * Tests whether this seizable resource unit can be assigned to the specified entity.
	 * @param ent - entity that would seize this unit
	 * @return true if this unit can be seized
	 */
	public boolean canSeize(DisplayEntity ent);

	/**
	 * Assigns this unit to the specified entity.
	 * @param ent - entity that will seize this unit
	 */
	public void seize(DisplayEntity ent);

	/**
	 * Unassigns this unit from its present assignee.
	 */
	public void release();

	/**
	 * Returns the entity to which this unit is assigned.
	 * @return entity to which this unit is assigned
	 */
	public DisplayEntity getAssignment();

	/**
	 * Returns the priority for this unit to be used by the ResourcePool when choosing the next
	 * unit to be seized.
	 * @param ent - entity that would seize this unit
	 * @return priority for this unit
	 */
	public int getPriority(DisplayEntity ent);

	/**
	 * Returns the last time in clock ticks at which this unit was unassigned.
	 * @return last clock tick at which the unit was released
	 */
	public long getLastReleaseTicks();

}
