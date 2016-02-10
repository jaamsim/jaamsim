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
package com.jaamsim.CalculationObjects;

import com.jaamsim.ProbabilityDistributions.Distribution;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

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
	protected final SampleInput scale;

	{
		DoubleVector defList = new DoubleVector();
		defList.add(0.0);
		coefficientList = new ValueListInput( "CoefficientList", "Key Inputs", defList);
		coefficientList.setUnitType(DimensionlessUnit.class);
		this.addInput( coefficientList);

		scale = new SampleInput( "Scale", "Key Inputs", new SampleConstant(UserSpecifiedUnit.class, 1.0d));
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
