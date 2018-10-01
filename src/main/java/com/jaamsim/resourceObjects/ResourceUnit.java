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

import com.jaamsim.BasicObjects.DowntimeEntity;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProcessFlow.StateUserEntity;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Keyword;

public class ResourceUnit extends StateUserEntity implements Seizable {

	@Keyword(description = "The name of the ResourcePool from which this ResourceUnit can be "
	                     + "selected.",
	         exampleList = {"ResourcePool1"})
	private final EntityInput<ResourcePool> resourcePool;

	private DisplayEntity presentAssignment;  // entity to which this unit is assigned

	{
		resourcePool = new EntityInput<>(ResourcePool.class, "ResourcePool", KEY_INPUTS, null);
		resourcePool.setRequired(true);
		this.addInput(resourcePool);
	}

	public ResourceUnit() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		presentAssignment = null;
	}

	@Override
	public ResourcePool getResourcePool() {
		return resourcePool.getValue();
	}

	/**
	 * Tests whether the specified entity is eligible to seize this unit.
	 * @param ent - entity to be tested
	 * @return true if the entity is eligible
	 */
	public boolean isAllowed(DisplayEntity ent) {
		return true;
	}

	@Override
	public boolean canSeize(DisplayEntity ent) {
		return (presentAssignment == null && isAllowed(ent));
	}

	@Override
	public void seize(DisplayEntity ent) {
		if (!canSeize(ent)) {
			error("Unit is already in use: assignment=%s, entity=%s", presentAssignment, ent);
		}
		presentAssignment = ent;

		// Set the new state
		setBusy(true);
		setPresentState();
	}

	@Override
	public void release() {
		presentAssignment = null;

		// Set the new state
		setBusy(false);
		setPresentState();
	}

	@Override
	public DisplayEntity getAssignment() {
		return presentAssignment;
	}

	@Override
	public void thresholdChanged() {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean canStartDowntime(DowntimeEntity down) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void prepareForDowntime(DowntimeEntity down) {
		// TODO Auto-generated method stub
	}

	@Override
	public void startDowntime(DowntimeEntity down) {
		// TODO Auto-generated method stub
	}

	@Override
	public void endDowntime(DowntimeEntity down) {
		// TODO Auto-generated method stub
	}

}
