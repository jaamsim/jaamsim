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
package com.jaamsim.CalculationObjects;

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * The Integrator returns the integral of the input values.
 * @author Harry King
 *
 */
public class Integrator extends DoubleCalculation {

	@Keyword(description = "The initial value for the integral at time = 0.",
	         exampleList = {"5.5 m/s", "[InputValue1].Value"})
	private final SampleInput initialValue;

	{
		initialValue = new SampleInput("InitialValue", KEY_INPUTS, new SampleConstant(0.0));
		initialValue.setUnitType(UserSpecifiedUnit.class);
		this.addInput(initialValue);
	}

	public Integrator() {}

	@Override
	protected void setUnitType(Class<? extends Unit> ut) {
		super.setUnitType(ut);

		outUnitType = Unit.getMultUnitType(ut, TimeUnit.class);
		if (outUnitType == null)
			outUnitType = DimensionlessUnit.class;
		initialValue.setUnitType(outUnitType);
	}

	@Override
	public double getInitialValue() {
		return initialValue.getValue().getNextSample(getSimTime());
	}

	@Override
	protected double calculateValue(double simTime, double inputVal, double lastTime, double lastInputVal, double lastVal) {

		// Calculate the elapsed time
		double dt = simTime - lastTime;
		if (dt <= 0.0)
			return lastVal;

		// Calculate the integral
		return lastVal + 0.5*(lastInputVal + inputVal)*dt;
	}

}
