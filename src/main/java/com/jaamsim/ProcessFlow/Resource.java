/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProbabilityDistributions.Distribution;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Samples.TimeSeries;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

public class Resource extends DisplayEntity {

	@Keyword(description = "The number of equivalent resource units that are available.\n"
	                     + "If the capacity changes during the simulation run, the Resource will "
	                     + "attempt to use an increase in capacity as soon as it occurs. "
	                     + "However, a decrease in capacity will have no affect on entities that "
	                     + "have already seized Resource capacity.",
	         exampleList = {"3", "TimeSeries1", "this.attrib1"})
	private final SampleInput capacity;

	@Keyword(description = "If TRUE, the next entity to seize the resource will be chosen "
	                     + "strictly on the basis of priority and waiting time. If this entity "
	                     + "is unable to seize the resource because of other restrictions such as "
	                     + "an OperatingThreshold input or the unavailability of other resources "
	                     + "it needs to seize at the same time, then other entities with lower "
	                     + "priority or shorter waiting time will NOT be allowed to seize the "
	                     + "resource. If FALSE, the entities will be tested in the same order of "
	                     + "priority and waiting time, but the first entity that is able to seize "
	                     + "the resource will be allowed to do so.",
	         exampleList = {"TRUE"})
	private final BooleanInput strictOrder;

	private int unitsInUse;  // number of resource units that are being used at present
	private ArrayList<ResourceUser> userList;  // objects that seize this resource
	private int lastCapacity; // capacity for the resource

	//	Statistics
	protected double timeOfLastUpdate; // time at which the statistics were last updated
	protected double startOfStatisticsCollection; // time at which statistics collection was started
	protected int minUnitsInUse; // minimum observed number of units in use
	protected int maxUnitsInUse; // maximum observed number of units in use
	protected double unitSeconds;  // total time that units have been used
	protected double squaredUnitSeconds;  // total time for the square of the number of units in use
	protected int unitsSeized;    // number of units that have been seized
	protected int unitsReleased;  // number of units that have been released
	protected DoubleVector unitsInUseDist;  // entry at position n is the total time that n units have been in use

	{
		attributeDefinitionList.setHidden(false);

		capacity = new SampleInput("Capacity", "Key Inputs", new SampleConstant(1.0));
		capacity.setUnitType(DimensionlessUnit.class);
		capacity.setEntity(this);
		capacity.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(capacity);

		strictOrder = new BooleanInput("StrictOrder", "Key Inputs", false);
		this.addInput(strictOrder);
	}

	public Resource() {
		unitsInUseDist = new DoubleVector();
		userList = new ArrayList<>();
	}

	@Override
	public void validate() {

		boolean found = false;
		for (Entity ent : Entity.getClonesOfIterator(Entity.class, ResourceUser.class)) {
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
		startOfStatisticsCollection = 0.0;
		timeOfLastUpdate = 0.0;
		minUnitsInUse = 0;
		maxUnitsInUse = 0;
		unitSeconds = 0.0;
		squaredUnitSeconds = 0.0;
		unitsSeized = 0;
		unitsReleased = 0;
		unitsInUseDist.clear();

		// Prepare a list of the objects that seize this resource
		userList.clear();
		for (Entity ent : Entity.getClonesOfIterator(Entity.class, ResourceUser.class)) {
			ResourceUser ru = (ResourceUser) ent;
			if (ru.requiresResource(this))
				userList.add(ru);
		}
	}

	@Override
	public void startUp() {
		super.startUp();

		if (capacity.getValue() instanceof SampleConstant)
			return;

		// Track any changes in the Resource's capacity
		this.waitForCapacityChange();
	}

	/**
	 * Seize the given number of units from the resource.
	 * @param n = number of units to seize
	 */
	public void seize(int n) {
		this.updateStatistics(unitsInUse, unitsInUse+n);
		unitsInUse += n;
		unitsSeized += n;
		if (getAvailableUnits(getSimTime()) < 0) {
			error("Capacity of resource exceeded. Capacity: %s, units in use: %s.",
					getCapacity(getSimTime()), unitsInUse);
		}
	}

	/**
	 * Release the given number of units back to the resource.
	 * @param n = number of units to release
	 */
	public void release(int m) {
		int n = Math.min(m, unitsInUse);
		this.updateStatistics(unitsInUse, unitsInUse-n);
		unitsInUse -= n;
		unitsReleased += n;
	}

	/**
	 * Notify all the users of this Resource that the number of available units has increased.
	 */
	public void notifySeizeObjects() {

		// Is there capacity available?
		int cap = this.getCapacity(this.getSimTime());
		if (cap <= unitsInUse)
			return;

		// Prepare a sorted list of the users that have a waiting entity
		ArrayList<ResourceUser> list = new ArrayList<>(userList.size());
		for (ResourceUser ru : userList) {
			if (ru.hasWaitingEntity()) {
				list.add(ru);
			}
		}
		Collections.sort(list, userCompare);

		// Attempt to start the resource users in order of priority and wait time
		while (true) {

			// Find the first resource user that can seize its resources
			ResourceUser selection = null;
			for (ResourceUser ru : list) {
				if (ru.isReadyToStart()) {
					selection = ru;
					break;
				}

				// In strict-order mode, only the highest priority/longest wait time entity is
				// eligible to seize its resources
				if (strictOrder.getValue())
					return;
			}

			// If none of the resource users can seize its resources, then we are done
			if (selection == null)
				return;

			// Seize the resources
			selection.startNextEntity();

			// Is additional capacity available?
			if (cap <= unitsInUse)
				return;

			// If the selected object has no more entities, remove it from the list
			if (!selection.hasWaitingEntity()) {
				list.remove(selection);
			}
			// If it does have more entities, re-sort the list to account for the next entity
			else {
				Collections.sort(list, userCompare);
			}
		}
	}

	/**
	 * Sorts the users of the Resource by their priority and waiting time
	 */
	private static class UserCompare implements Comparator<ResourceUser> {
		@Override
		public int compare(ResourceUser ru1, ResourceUser ru2) {

			// Chose the object with the highest priority entity
			// (lowest numerical value, i.e. 1 is higher priority than 2)
			int ret = Integer.compare(ru1.getPriority(), ru2.getPriority());

			// If the priorities are the same, choose the one with the longest waiting time
			if (ret == 0) {
				return Double.compare(ru2.getWaitTime(), ru1.getWaitTime());
			}
			return ret;
		}
	}
	private UserCompare userCompare = new UserCompare();

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
			this.notifySeizeObjects();
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
		startOfStatisticsCollection = simTime;
		timeOfLastUpdate = simTime;
		minUnitsInUse = unitsInUse;
		maxUnitsInUse = unitsInUse;
		unitSeconds = 0.0;
		squaredUnitSeconds = 0.0;
		unitsSeized = 0;
		unitsReleased = 0;
		for (int i=0; i<unitsInUseDist.size(); i++) {
			unitsInUseDist.set(i, 0.0d);
		}
	}

	public void updateStatistics( int oldValue, int newValue) {

		minUnitsInUse = Math.min(newValue, minUnitsInUse);
		maxUnitsInUse = Math.max(newValue, maxUnitsInUse);

		// Add the necessary number of additional bins to the queue length distribution
		int n = newValue + 1 - unitsInUseDist.size();
		for( int i=0; i<n; i++ ) {
			unitsInUseDist.add(0.0);
		}

		double simTime = this.getSimTime();
		double dt = simTime - timeOfLastUpdate;
		if( dt > 0.0 ) {
			unitSeconds += dt * oldValue;
			squaredUnitSeconds += dt * oldValue * oldValue;
			unitsInUseDist.addAt(dt,oldValue);  // add dt to the entry at index queueSize
			timeOfLastUpdate = simTime;
		}
	}

	// ******************************************************************************************************
	// OUTPUT METHODS
	// ******************************************************************************************************

	@Output(name = "Capacity",
	 description = "The total number of resource units that can be used.",
	    unitType = DimensionlessUnit.class,
	    sequence = 0)
	public int getCapacity(double simTime) {
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
		double dt = simTime - timeOfLastUpdate;
		double totalTime = simTime - startOfStatisticsCollection;
		if( totalTime > 0.0 ) {
			return (unitSeconds + dt*unitsInUse)/totalTime;
		}
		return 0.0;
	}

	@Output(name = "UnitsInUseStandardDeviation",
	 description = "The standard deviation of the number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 6)
	public double getUnitsInUseStandardDeviation(double simTime) {
		double dt = simTime - timeOfLastUpdate;
		double mean = this.getUnitsInUseAverage(simTime);
		double totalTime = simTime - startOfStatisticsCollection;
		if( totalTime > 0.0 ) {
			return Math.sqrt( (squaredUnitSeconds + dt*unitsInUse*unitsInUse)/totalTime - mean*mean );
		}
		return 0.0;
	}

	@Output(name = "UnitsInUseMinimum",
	 description = "The minimum number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 7)
	public int getUnitsInUseMinimum(double simTime) {
		return minUnitsInUse;
	}

	@Output(name = "UnitsInUseMaximum",
	 description = "The maximum number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 8)
	public int getUnitsInUseMaximum(double simTime) {
		// A unit that is seized and released immediately
		// does not count as a non-zero maximum in use
		if( maxUnitsInUse == 1 && unitsInUseDist.get(1) == 0.0 )
			return 0;
		return maxUnitsInUse;
	}

	@Output(name = "UnitsInUseTimes",
	 description = "The total time that the number of resource units in use was 0, 1, 2, etc.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 9)
	public DoubleVector getUnitsInUseDistribution(double simTime) {
		DoubleVector ret = new DoubleVector(unitsInUseDist);
		double dt = simTime - timeOfLastUpdate;
		if(ret.size() == 0)
			ret.add(0.0);
		ret.addAt(dt, unitsInUse);  // adds dt to the entry at index unitsInUse
		return ret;
	}

}
