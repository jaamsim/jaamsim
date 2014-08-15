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

import com.jaamsim.Samples.SampleListInput;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * The WeightedSum object returns a weighted sum of its input values.
 * @author Harry King
 *
 */
public class WeightedSum extends DoubleCalculation {

	@Keyword(description = "The list of inputs to the weighted sum.\n" +
			"The inputs can be any entity that returns a number, such as a CalculationObject, ProbabilityDistribution, or a TimeSeries.",
	         example = "WeightedSum-1 InputValueList { Calc-1  Calc-2 }")
	private final SampleListInput inputValueList;

	@Keyword(description = "The list of multaplicative factors to be applied to the value provide by the inputs.",
	         example = "WeightedSum1 CoefficientList { 2.0  1.5 }")
	private final ValueListInput coefficientList;

	{
		inputValue.setHidden(true);

		inputValueList = new SampleListInput( "InputValueList", "Key Inputs", null);
		inputValueList.setUnitType(UserSpecifiedUnit.class);
		this.addInput( inputValueList);

		DoubleVector defList = new DoubleVector();
		defList.add(1.0);
		coefficientList = new ValueListInput( "CoefficientList", "Key Inputs", defList);
		coefficientList.setUnitType(DimensionlessUnit.class);
		this.addInput( coefficientList);
	}

	@Override
	protected void setUnitType(Class<? extends Unit> ut) {
		super.setUnitType(ut);
		inputValueList.setUnitType(ut);
		FrameBox.reSelectEntity();  // Update the units in the Output Viewer
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the number of entries in the CoeffientList matches the EntityList
		if( coefficientList.getValue().size() != inputValueList.getValue().size() ) {
			throw new InputErrorException( "The number of entries for CoefficientList and EntityList must be equal" );
		}
	}

	@Override
	protected double calculateValue(double simTime) {
		double val = 0.0;

		// Calculate the weighted sum
		for(int i=0; i<inputValueList.getValue().size(); i++ ) {
			val += coefficientList.getValue().get(i) * inputValueList.getValue().get(i).getNextSample(simTime);
		}

		return val;
	}

}
