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
package com.jaamsim.ProcessFlow;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Collects basic statistical information on the entities that are received.
 * @author Harry King
 *
 */
public class Statistics extends LinkedComponent {

	@Keyword(description = "The unit type for the variable whose statistic will be collected.",
	         exampleList = {"DistanceUnit"})
	private final UnitTypeInput unitType;

	@Keyword(description = "The variable for which statistics will be collected.",
	         exampleList = {"'this.obj.attrib1'"})
	private final SampleInput sampleValue;

	private double minValue;
	private double maxValue;
	private double totalValue;
	private double totalSquaredValue;
	private double lastValue;
	private double lastUpdateTime;
	private double totalTimeValue;
	private double totalSquaredTimeValue;
	private double firstSampleTime;

	{
		stateAssignment.setHidden(true);

		unitType = new UnitTypeInput("UnitType", "Key Inputs", UserSpecifiedUnit.class);
		unitType.setRequired(true);
		this.addInput(unitType);

		sampleValue = new SampleInput("SampleValue", "Key Inputs", null);
		sampleValue.setUnitType(UserSpecifiedUnit.class);
		sampleValue.setEntity(this);
		sampleValue.setRequired(true);
		this.addInput(sampleValue);
	}

	public Statistics() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == unitType) {
			Class<? extends Unit> ut = unitType.getUnitType();
			sampleValue.setUnitType(ut);
			return;
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		minValue = Double.POSITIVE_INFINITY;
		maxValue = Double.NEGATIVE_INFINITY;
		totalValue = 0.0;
		totalSquaredValue = 0.0;

		lastValue = 0.0;
		lastUpdateTime = 0.0;
		totalTimeValue = 0.0;
		totalSquaredTimeValue = 0.0;
		firstSampleTime = 0.0;
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);
		double simTime = this.getSimTime();

		// Update the statistics
		double val = sampleValue.getValue().getNextSample(simTime);
		minValue = Math.min(minValue, val);
		maxValue = Math.max(maxValue, val);
		totalValue += val;
		totalSquaredValue += val*val;

		// Calculate the time average
		if (this.getNumberAdded(simTime) == 1L) {
			firstSampleTime = simTime;
		}
		else {
			double weightedVal = lastValue * (simTime - lastUpdateTime);
			totalTimeValue += weightedVal;
			totalSquaredTimeValue += lastValue*weightedVal;
		}
		lastValue = val;
		lastUpdateTime = simTime;

		// Pass the entity to the next component
		this.sendToNextComponent(ent);
	}

	@Override
	public void clearStatistics() {
		super.clearStatistics();

		minValue = Double.POSITIVE_INFINITY;
		maxValue = Double.NEGATIVE_INFINITY;
		totalValue = 0.0;
		totalSquaredValue = 0.0;

		totalTimeValue = 0.0;
		lastValue = 0.0;
		lastUpdateTime = 0.0;
		totalSquaredTimeValue = 0.0;
		firstSampleTime = 0.0;
	}

	@Override
	public Class<? extends Unit> getUserUnitType() {
		return unitType.getUnitType();
	}

	// ******************************************************************************************************
	// OUTPUT METHODS
	// ******************************************************************************************************

	@Output(name = "SampleMinimum",
	 description = "The smallest value that was recorded.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 0)
	public double getSampleMinimum(double simTime) {
		return minValue;
	}

	@Output(name = "SampleMaximum",
	 description = "The largest value that was recorded.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 1)
	public double getSampleMaximum(double simTime) {
		return maxValue;
	}

	@Output(name = "SampleAverage",
	 description = "The average of the values that were recorded.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 2)
	public double getSampleAverage(double simTime) {
		return totalValue/this.getNumberAdded(simTime);
	}

	@Output(name = "SampleStandardDeviation",
	 description = "The standard deviation of the values that were recorded.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 3)
	public double getSampleStandardDeviation(double simTime) {
		double num = this.getNumberAdded(simTime);
		double mean = totalValue/num;
		return Math.sqrt(totalSquaredValue/num - mean*mean);
	}

	@Output(name = "StandardDeviationOfTheMean",
	 description = "The estimated standard deviation of the sample mean.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 4)
	public double getStandardDeviationOfTheMean(double simTime) {
		double num = this.getNumberAdded(simTime);
		return this.getSampleStandardDeviation(simTime)/Math.sqrt(num-1);
	}

	@Output(name = "TimeAverage",
	 description = "The average of the values recorded, weighted by the duration of each value.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 5)
	public double getTimeAverage(double simTime) {
		long num = this.getNumberAdded(simTime);
		if (num == 0L)
			return 0.0d;
		if (num == 1L)
			return lastValue;
		double dt = simTime - lastUpdateTime;
		return (totalTimeValue + lastValue*dt)/(simTime - firstSampleTime);
	}

	@Output(name = "TimeStandardDeviation",
	 description = "The standard deviation of the values recorded, weighted by the duration of each value.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 6)
	public double getTimeStandardDeviation(double simTime) {
		long num = this.getNumberAdded(simTime);
		if (num <= 1L)
			return 0.0d;
		double mean = this.getTimeAverage(simTime);
		double dt = simTime - lastUpdateTime;
		double meanOfSquare = (totalSquaredTimeValue + lastValue*lastValue*dt)/(simTime - firstSampleTime);
		return Math.sqrt(meanOfSquare - mean*mean);
	}

}
