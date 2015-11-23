/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProbabilityDistributions.Distribution;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

public class Resource extends DisplayEntity {

	@Keyword(description = "The number of equivalent resource units that are available.\n" +
			"The input can be a constant value, a time series, or an expression.",
	         exampleList = {"3", "TimeSeries1", "this.attrib1"})
	private final SampleExpInput capacity;

	private int unitsInUse;  // number of resource units that are being used at present
	private ArrayList<Seize> seizeList;  // Seize objects that require this resource

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

		capacity = new SampleExpInput("Capacity", "Key Inputs", new SampleConstant(1.0));
		capacity.setUnitType(DimensionlessUnit.class);
		capacity.setEntity(this);
		capacity.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(capacity);
	}

	public Resource() {
		unitsInUseDist = new DoubleVector();
		seizeList = new ArrayList<>();
	}

	@Override
	public void validate() {

		boolean found = false;
		for (Seize ent : Entity.getClonesOfIterator(Seize.class)) {
			if( ent.requiresResource(this) )
				found = true;
		}
		if (!found)
			throw new InputErrorException( "At least one Seize object must use this resource." );

		if( capacity.getValue() instanceof Distribution )
			throw new InputErrorException( "The Capacity keyword cannot accept a probability distribution.");
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		unitsInUse = 0;

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

		// Prepare a list of the Seize objects that use this resource
		seizeList.clear();
		for (Seize ent : Entity.getClonesOfIterator(Seize.class)) {
			if( ent.requiresResource(this) )
				seizeList.add(ent);
		}
	}

	/**
	 * Return the number of units that are available for use at the present time
	 * @return
	 */
	public int getAvailableUnits() {
		return (int) capacity.getValue().getNextSample(this.getSimTime()) - unitsInUse;
	}

	/**
	 * Seize the given number of units from the resource.
	 * @param n = number of units to seize
	 */
	public void seize(int n) {
		this.updateStatistics(unitsInUse, unitsInUse+n);
		unitsInUse += n;
		unitsSeized += n;
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
	 * Notify all the Seize object that the number of available units of this Resource has increased.
	 */
	public void notifySeizeObjects() {

		// Notify the Seize object(s) that can use the released units
		int cap = (int) capacity.getValue().getNextSample(this.getSimTime());
		while( cap > unitsInUse ) {

			// Pick the Seize object that has waited the longest
			Seize selection = null;
			double maxTime = 0;
			for(Seize s : seizeList) {
				Queue que = s.getQueue();
				if( que.getCount() > 0 ) {
					if( selection == null || que.getQueueTime() > maxTime ) {
						selection = s;
						maxTime = que.getQueueTime();
					}
				}
			}

			// Ensure that the selected Seize object is able to start
			if (selection == null || !selection.isReadyToStart())
				return;
			selection.startAction();
		}
	}

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

	@Output(name = "UnitsSeized",
	 description = "The total number of resource units that have been seized.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 0)
	public int getUnitsSeized(double simTime) {
		return unitsSeized;
	}

	@Output(name = "UnitsReleased",
	 description = "The total number of resource units that have been released.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 1)
	public int getUnitsReleased(double simTime) {
		return unitsReleased;
	}

	@Output(name = "UnitsInUse",
	 description = "The present number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	    sequence = 2)
	public int getUnitsInUse(double simTime) {
		return unitsInUse;
	}

	@Output(name = "UnitsInUseAverage",
	 description = "The average number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	  sequence = 3)
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
	  sequence = 4)
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
	    sequence = 5)
	public int getUnitsInUseMinimum(double simTime) {
		return minUnitsInUse;
	}

	@Output(name = "UnitsInUseMaximum",
	 description = "The maximum number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 6)
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
	    sequence = 7)
	public DoubleVector getUnitsInUseDistribution(double simTime) {
		DoubleVector ret = new DoubleVector(unitsInUseDist);
		double dt = simTime - timeOfLastUpdate;
		if(ret.size() == 0)
			ret.add(0.0);
		ret.addAt(dt, unitsInUse);  // adds dt to the entry at index unitsInUse
		return ret;
	}

}
