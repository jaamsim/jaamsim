/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.CalculationObjects;

import com.jaamsim.ProbabilityDistributions.Distribution;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;
import com.sandwell.JavaSimulation.DoubleVector;

/**
 * The Polynomial entity returns a user-defined polynomial function of its input value.
 * @author Harry King
 *
 */
public class Polynomial extends DoubleCalculation {

	@Keyword(description = "The list of coefficients for the polynomial function.\n" +
			"For example, inputs c0, c1, c2 give a polynomial P(x) = scale * [ c0 + c1*(x/scale) + c2*(x/scale)^2 ]",
	         example = "Polynomial-1 CoefficientList { 2.0  1.5 }")
	private final ValueListInput coefficientList;

	@Keyword(description = "The scale to apply to the input value.\n" +
			"The input can be a number or an entity that returns a number, such as a CalculationObject, ProbabilityDistribution, or a TimeSeries.",
	         example = "Polynomial-1 Scale { 5.0 m }")
	protected final SampleExpInput scale;

	{
		DoubleVector defList = new DoubleVector();
		defList.add(0.0);
		coefficientList = new ValueListInput( "CoefficientList", "Key Inputs", defList);
		coefficientList.setUnitType(DimensionlessUnit.class);
		this.addInput( coefficientList);

		scale = new SampleExpInput( "Scale", "Key Inputs", new SampleConstant(UserSpecifiedUnit.class, 1.0d));
		scale.setUnitType(UserSpecifiedUnit.class);
		scale.setEntity(this);
		this.addInput( scale);
	}

	@Override
	protected boolean repeatableInputs() {
		return super.repeatableInputs()
				&& ! (scale.getValue() instanceof Distribution);
	}

	@Override
	protected void setUnitType(Class<? extends Unit> ut) {
		super.setUnitType(ut);
		scale.setUnitType(ut);
		FrameBox.reSelectEntity();  // Update the units in the Output Viewer
	}

	@Override
	protected double calculateValue(double simTime) {

		double x = this.getInputValue(simTime) / scale.getValue().getNextSample(simTime);
		double pow = 1.0;
		double val = 0.0;
		for(int i=0; i<coefficientList.getValue().size(); i++ ) {
			val += coefficientList.getValue().get(i) * pow;
			pow *= x;
		}

		return val;
	}

}
