/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2020 JaamSim Software Inc.
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
import com.jaamsim.Statistics.TimeBasedFrequency;
import com.jaamsim.Statistics.TimeBasedStatistics;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

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

	//	Statistics
	private final TimeBasedStatistics stats;
	private final TimeBasedFrequency freq;
	protected int unitsSeized;    // number of units that have been seized
	protected int unitsReleased;  // number of units that have been released

	{
		trace.setHidden(false);
		attributeDefinitionList.setHidden(false);

		capacity = new SampleInput("Capacity", KEY_INPUTS, new SampleConstant(1.0));
		capacity.setUnitType(DimensionlessUnit.class);
		capacity.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(capacity);
	}

	public Resource() {
		stats = new TimeBasedStatistics();
		freq = new TimeBasedFrequency(0, 10);
	}

	@Override
	public void validate() {

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

		// Clear statistics
		stats.clear();
		stats.addValue(0.0d, 0);
		freq.clear();
		freq.addValue(0.0d,  0);
		unitsSeized = 0;
		unitsReleased = 0;
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
		return (int) capacity.getValue().getNextSample(simTime);
	}

	@Override
	public int getUnitsInUse() {
		return unitsInUse;
	}

	@Override
	public boolean canSeize(int n, DisplayEntity ent) {
		double simTime = getSimTime();
		return getAvailableUnits(simTime) >= n;
	}

	@Override
	public void seize(int n, DisplayEntity ent) {
		if (isTraceFlag()) trace(1, "seize(%s, %s)", n, ent);
		double simTime = getSimTime();
		if (getAvailableUnits(simTime) < n)
			error(ERR_CAPACITY, getCapacity(simTime), n);

		unitsInUse += n;
		unitsSeized += n;
		stats.addValue(simTime, unitsInUse);
		freq.addValue(simTime, unitsInUse);
	}

	@Override
	public void release(int m, DisplayEntity ent) {
		if (isTraceFlag()) trace(1, "release(%s, %s)", m, ent);
		int n = Math.min(m, unitsInUse);
		unitsInUse -= n;
		unitsReleased += n;
		double simTime = this.getSimTime();
		stats.addValue(simTime, unitsInUse);
		freq.addValue(simTime, unitsInUse);
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

	// *******************************************************************************************************
	// STATISTICS
	// *******************************************************************************************************

	@Override
	public void clearStatistics() {
		super.clearStatistics();
		double simTime = this.getSimTime();
		stats.clear();
		stats.addValue(simTime, unitsInUse);
		freq.clear();
		freq.addValue(simTime, unitsInUse);
		unitsSeized = 0;
		unitsReleased = 0;
	}

	// ******************************************************************************************************
	// OUTPUT METHODS
	// ******************************************************************************************************

	@Output(name = "Capacity",
	 description = "The total number of resource units that can be used.",
	    unitType = DimensionlessUnit.class,
	    sequence = 0)
	public int getPresentCapacity(double simTime) {
		return (int) capacity.getValue().getNextSample(simTime);
	}

	@Output(name = "UnitsInUse",
	 description = "The present number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	    sequence = 1)
	public int getUnitsInUse(double simTime) {
		return unitsInUse;
	}

	@Output(name = "AvailableUnits",
	 description = "The number of resource units that are not in use.",
	    unitType = DimensionlessUnit.class,
	    sequence = 2)
	public int getAvailableUnits(double simTime) {
		return getCapacity(simTime) - unitsInUse;
	}

	@Output(name = "UnitsSeized",
	 description = "The total number of resource units that have been seized.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 3)
	public int getUnitsSeized(double simTime) {
		return unitsSeized;
	}

	@Output(name = "UnitsReleased",
	 description = "The total number of resource units that have been released.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 4)
	public int getUnitsReleased(double simTime) {
		return unitsReleased;
	}

	@Output(name = "UnitsInUseAverage",
	 description = "The average number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 5)
	public double getUnitsInUseAverage(double simTime) {
		return stats.getMean(simTime);
	}

	@Output(name = "UnitsInUseStandardDeviation",
	 description = "The standard deviation of the number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 6)
	public double getUnitsInUseStandardDeviation(double simTime) {
		return stats.getStandardDeviation(simTime);
	}

	@Output(name = "UnitsInUseMinimum",
	 description = "The minimum number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 7)
	public int getUnitsInUseMinimum(double simTime) {
		return (int) stats.getMin();
	}

	@Output(name = "UnitsInUseMaximum",
	 description = "The maximum number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 8)
	public int getUnitsInUseMaximum(double simTime) {
		int ret = (int) stats.getMax();
		// A unit that is seized and released immediately
		// does not count as a non-zero maximum in use
		if (ret == 1 && freq.getBinTime(simTime, 1) == 0.0d)
			return 0;
		return ret;
	}

	@Output(name = "UnitsInUseTimes",
	 description = "The total time that the number of resource units in use was 0, 1, 2, etc.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 9)
	public double[] getUnitsInUseDistribution(double simTime) {
		return freq.getBinTimes(simTime);
	}

}
