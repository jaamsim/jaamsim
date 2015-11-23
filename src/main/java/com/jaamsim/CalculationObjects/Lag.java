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

import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;

/**
 * The Lag block is a standard control system component whose output is equal to integral( input - output ) / LagTime.
 * This operation has the effect of delaying and smoothing the input signal over the time scale given by LagTime.
 * @author Harry King
 *
 */
public class Lag extends DoubleCalculation {

	@Keyword(description = "The time constant for this operation: output = integral( input - output) / LagTime.",
	         example = "Lag-1 LagTime { 15 s }")
	private final ValueInput lagTime;

	private double lastUpdateTime;  // The time at which the last update was performed
	private double integral; // The present value for the integral
	private double presentValue;  // The present output value

	{
		controllerRequired = true;

		lagTime = new ValueInput( "LagTime", "Key Inputs", 1.0d);
		lagTime.setValidRange(1.0e-10, Double.POSITIVE_INFINITY);
		lagTime.setUnitType(TimeUnit.class);
		this.addInput( lagTime);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		integral = 0.0;
		presentValue = 0.0;
		lastUpdateTime = 0.0;
	}

	@Override
	public double calculateValue(double simTime) {
		double dt = simTime - lastUpdateTime;
		double error = this.getInputValue(simTime) - presentValue;
		return ( integral + error*dt ) / lagTime.getValue();
	}

	@Override
	public void update(double simTime) {
		super.update(simTime);
		double dt = simTime - lastUpdateTime;
		double error = this.getInputValue(simTime) - presentValue;
		integral += error * dt;
		presentValue = this.getValue();
		lastUpdateTime = simTime;
	}

	@Output(name = "Error",
	 description = "The value for InputValue - OutputValue.")
	public double getError( double simTime ) {
		return this.getInputValue(simTime) - presentValue;
	}

}
