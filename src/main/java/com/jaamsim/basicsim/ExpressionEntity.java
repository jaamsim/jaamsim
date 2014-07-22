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
package com.jaamsim.basicsim;

import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;
import com.sandwell.JavaSimulation.Entity;

public class ExpressionEntity extends Entity {
	@Keyword(description = "The variable for which statistics will be collected.",
	         example = "Statistics1  SampleValue { 'this.obj.Attrib1' }")
	private final ExpressionInput sampleValue;

	{
		sampleValue = new ExpressionInput("Expression", "Key Inputs", null);
		sampleValue.setEntity(this);
		this.addInput(sampleValue);
	}

	public ExpressionEntity() {}

	@Output(name = "Value",
	 description = "The evaluated value of the expression.",
	    unitType = DimensionlessUnit.class,
	  reportable = true)
	public double evaluateExpression(double simTime) {
		if (sampleValue.getValue() == null)
			return 0.0d;

		try {
			// Evaluate the expression
			double ret = ExpEvaluator.evaluateExpression(sampleValue.getValue(), simTime, this, null).value;
			return ret;
		} catch(ExpEvaluator.Error e) {
			return 0.0d;
		}
	}
}
