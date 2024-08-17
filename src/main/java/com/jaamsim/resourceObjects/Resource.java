/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2024 JaamSim Software Inc.
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
import com.jaamsim.ProbabilityDistributions.Distribution;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Samples.TimeSeries;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.DimensionlessUnit;

public class Resource extends AbstractResourceProvider {

	@Keyword(description = "The number of equivalent resource units that are available. "
	                     + "Only an integer number of resource units can be specified. "
	                     + "A decimal value will be truncated to an integer.\n"
	                     + "If the capacity changes during the simulation run, the Resource will "
	                     + "attempt to use an increase in capacity as soon as it occurs. "
	                     + "However, a decrease in capacity will have no affect on entities that "
	                     + "have already seized Resource capacity.",
	         exampleList = {"3", "TimeSeries1", "this.attrib1"})
	private final SampleInput capacity;

	private int unitsInUse;  // number of resource units that are being used at present
	private int lastCapacity; // capacity for the resource

	{
		attributeDefinitionList.setHidden(false);

		capacity = new SampleInput("Capacity", KEY_INPUTS, 1);
		capacity.setUnitType(DimensionlessUnit.class);
		capacity.setIntegerValue(true);
		capacity.setValidRange(0, Double.POSITIVE_INFINITY);
		capacity.setOutput(true);
		this.addInput(capacity);
	}

	public Resource() {}

	@Override
	public void validate() {
		super.validate();

		boolean found = false;
		for (Entity ent : getJaamSimModel().getClonesOfIterator(Entity.class, ResourceUser.class)) {
			ResourceUser ru = (ResourceUser) ent;
			if (ru.requiresResource(this))
				found = true;
		}
		if (!found)
			throw new InputErrorException( "At least one object must seize this resource." );

		if( capacity.getValue() instanceof Distribution )
			throw new InputErrorException( "The Capacity keyword cannot accept a probability distribution.");
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		unitsInUse = 0;
		lastCapacity = this.getCapacity(0.0d);
	}

	@Override
	public void startUp() {
		super.startUp();

		if (capacity.getValue() instanceof SampleConstant)
			return;

		// Track any changes in the Resource's capacity
		this.waitForCapacityChange();
	}

	@Override
	public int getCapacity(double simTime) {
		return (int) capacity.getNextSample(this, simTime);
	}

	@Override
	public int getUnitsInUse() {
		return unitsInUse;
	}

	@Override
	public boolean canSeize(double simTime, int n, DisplayEntity ent) {
		return getAvailableUnits(simTime) >= n;
	}

	@Override
	public void seize(int n, DisplayEntity ent) {
		super.seize(n, ent);
		double simTime = getSimTime();
		if (getAvailableUnits(simTime) < n)
			error(ERR_CAPACITY, getAvailableUnits(simTime), n);

		unitsInUse += n;
		collectStatistics(simTime, unitsInUse);
	}

	@Override
	public void release(int m, DisplayEntity ent) {
		int n = Math.min(m, unitsInUse);
		super.release(n, ent);
		unitsInUse -= n;
		double simTime = this.getSimTime();
		collectStatistics(simTime, unitsInUse);
	}

	/**
	 * Returns true if the saved capacity differs from the present capacity
	 * @return true if the capacity has changed
	 */
	boolean isCapacityChanged() {
		return this.getCapacity(getSimTime()) != lastCapacity;
	}

	/**
	 * Loops from one capacity change to the next.
	 */
	void waitForCapacityChange() {

		// Set the present capacity
		lastCapacity = this.getCapacity(getSimTime());

		// Wait until the state is ready to change
		if (capacity.getValue() instanceof TimeSeries) {
			TimeSeries ts = (TimeSeries)capacity.getValue();
			long simTicks = getSimTicks();
			long durTicks = ts.getNextChangeAfterTicks(simTicks) - simTicks;
			this.scheduleProcessTicks(durTicks, 10, true, updateForCapacityChangeTarget, null); // FIFO
		}
		else {
			EventManager.scheduleUntil(updateForCapacityChangeTarget, capacityChangeConditional, null);
		}
	}

	/**
	 * Responds to a change in capacity.
	 */
	void updateForCapacityChange() {
		if (isTraceFlag()) trace(0, "updateForCapacityChange");

		// Select the resource users to notify
		if (this.getCapacity(getSimTime()) > lastCapacity) {
			ArrayList<ResourceProvider> resList = new ArrayList<>(1);
			resList.add(this);
			Resource.notifyResourceUsers(resList);
		}

		// Wait for the next capacity change
		this.waitForCapacityChange();
	}

	// Conditional for isCapacityChanged()
	class CapacityChangeConditional extends Conditional {
		@Override
		public boolean evaluate() {
			return Resource.this.isCapacityChanged();
		}
	}
	private final Conditional capacityChangeConditional = new CapacityChangeConditional();

	// Target for updateForCapacityChange()
	class UpdateForCapacityChangeTarget extends ProcessTarget {
		@Override
		public String getDescription() {
			return Resource.this.getName() + ".updateForCapacityChange";
		}

		@Override
		public void process() {
			Resource.this.updateForCapacityChange();
		}
	}
	private final ProcessTarget updateForCapacityChangeTarget = new UpdateForCapacityChangeTarget();

}
