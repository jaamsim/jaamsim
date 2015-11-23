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
package com.jaamsim.BasicObjects;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleExpInput;
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
	private final SampleExpInput sampleValue;

	private double minValue;
	private double maxValue;
	private double totalValue;
	private double totalSquaredValue;

	{
		stateAssignment.setHidden(true);

		unitType = new UnitTypeInput("UnitType", "Key Inputs", UserSpecifiedUnit.class);
		unitType.setRequired(true);
		this.addInput(unitType);

		sampleValue = new SampleExpInput("SampleValue", "Key Inputs", null);
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
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);

		// Update the statistics
		double val = sampleValue.getValue().getNextSample(getSimTime());
		minValue = Math.min(minValue, val);
		maxValue = Math.max(maxValue, val);
		totalValue += val;
		totalSquaredValue += val*val;

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
	  reportable = true)
	public double getSampleMinimum(double simTime) {
		return minValue;
	}

	@Output(name = "SampleMaximum",
	 description = "The largest value that was recorded.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true)
	public double getSampleMaximum(double simTime) {
		return maxValue;
	}

	@Output(name = "SampleAverage",
	 description = "The average of the values that were recorded.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true)
	public double getSampleAverage(double simTime) {
		return totalValue/this.getNumberAdded(simTime);
	}

	@Output(name = "SampleStandardDeviation",
	 description = "The standard deviation of the values that were recorded.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true)
	public double getSampleStandardDeviation(double simTime) {
		double num = this.getNumberAdded(simTime);
		double mean = totalValue/num;
		return Math.sqrt(totalSquaredValue/num - mean*mean);
	}

	@Output(name = "StandardDeviationOfTheMean",
	 description = "The estimated standard deviation of the sample mean.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true)
	public double getStandardDeviationOfTheMean(double simTime) {
		double num = this.getNumberAdded(simTime);
		return this.getSampleStandardDeviation(simTime)/Math.sqrt(num-1);
	}
}
