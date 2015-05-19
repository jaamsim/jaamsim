/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.BasicObjects;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;

/**
 * Collects basic statistical information on the entities that are received.
 * @author Harry King
 *
 */
public class Statistics extends LinkedComponent {

	@Keyword(description = "The variable for which statistics will be collected.",
	         exampleList = {"'this.obj.attrib1'"})
	private final ExpressionInput sampleValue;

	private double minValue;
	private double maxValue;
	private double totalValue;
	private double totalSquaredValue;

	{
		stateAssignment.setHidden(true);

		sampleValue = new ExpressionInput("SampleValue", "Key Inputs", null);
		sampleValue.setEntity(this);
		sampleValue.setRequired(true);
		this.addInput(sampleValue);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		this.clearStatistics();
	}

	@Override
	public void addEntity( DisplayEntity ent ) {
		super.addEntity(ent);

		// Update the statistics
		double val = this.getVariableValue(this.getSimTime());
		minValue = Math.min(minValue, val);
		maxValue = Math.max(maxValue, val);
		totalValue += val;
		totalSquaredValue += val*val;

		// Pass the entity to the next component
		this.sendToNextComponent(ent);
	}

	private double getVariableValue(double simTime) {
		try {
			// Evaluate the expression
			double ret = ExpEvaluator.evaluateExpression(sampleValue.getValue(), simTime, this).value;
			return ret;
		} catch(ExpError e) {
			throw new ErrorException(e);
		}
	}

	/**
	 * Clear queue statistics
	 */
	@Override
	public void clearStatistics() {
		minValue = Double.POSITIVE_INFINITY;
		maxValue = Double.NEGATIVE_INFINITY;
		totalValue = 0.0;
		totalSquaredValue = 0.0;
	}

	// ******************************************************************************************************
	// OUTPUT METHODS
	// ******************************************************************************************************

	@Output(name = "SampleMinimum",
	 description = "The smallest value that was recorded.",
	    unitType = DimensionlessUnit.class,
	  reportable = true)
	public double getSampleMinimum(double simTime) {
		return minValue;
	}

	@Output(name = "SampleMaximum",
	 description = "The largest value that was recorded.",
	    unitType = DimensionlessUnit.class,
	  reportable = true)
	public double getSampleMaximum(double simTime) {
		return maxValue;
	}

	@Output(name = "SampleAverage",
	 description = "The average of the values that were recorded.",
	    unitType = DimensionlessUnit.class,
	  reportable = true)
	public double getSampleAverage(double simTime) {
		return totalValue/this.getNumberAdded(simTime);
	}

	@Output(name = "SampleStandardDeviation",
	 description = "The standard deviation of the values that were recorded.",
	    unitType = DimensionlessUnit.class,
	  reportable = true)
	public double getSampleStandardDeviation(double simTime) {
		double num = this.getNumberAdded(simTime);
		double mean = totalValue/num;
		return Math.sqrt(totalSquaredValue/num - mean*mean);
	}

	@Output(name = "StandardDeviationOfTheMean",
	 description = "The estimated standard deviation of the sample mean.",
	    unitType = DimensionlessUnit.class,
	  reportable = true)
	public double getStandardDeviationOfTheMean(double simTime) {
		double num = this.getNumberAdded(simTime);
		return this.getSampleStandardDeviation(simTime)/Math.sqrt(num-1);
	}
}
