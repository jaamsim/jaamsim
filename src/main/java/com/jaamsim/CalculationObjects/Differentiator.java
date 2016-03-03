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

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.TimeUnit;

/**
 * The differentiator returns the derivative of the input signal with respect to time.
 * @author Harry King
 *
 */
public class Differentiator extends DoubleCalculation {

	@Keyword(description = "The time scale for the derivative:  derivative = DerivativeTime * dx/dt\n" +
			"The input can be a number or an entity that returns a number, such as a CalculationObject, ProbabilityDistribution, or a TimeSeries.",
	         example = "Differentiator-1 DerivativeTime { 5 s }")
	protected final SampleInput derivativeTime;

	private double lastUpdateTime;  // The time at which the last update was performed
	private double lastInputValue;  // The input value for the last update

	{
		derivativeTime = new SampleInput( "DerivativeTime", "Key Inputs", new SampleConstant(TimeUnit.class, 1.0) );
		derivativeTime.setUnitType(TimeUnit.class);
		derivativeTime.setEntity(this);
		this.addInput( derivativeTime);
	}

	@Override
	public void earlyInit() {
		lastUpdateTime = 0.0;
	}

	@Override
	protected double calculateValue(double simTime, double inputVal, double lastTime, double lastInputVal, double lastVal) {

		// Calculate the elapsed time
		double dt = simTime - lastUpdateTime;
		if( dt <= 0 )
			return 0;

		// Calculate the derivative
		double scale = derivativeTime.getValue().getNextSample(simTime);
		return ( this.getInputValue(simTime) - lastInputValue ) * scale/dt;
	}

	@Override
	public void update(double simTime) {
		super.update(simTime);
		lastInputValue = this.getInputValue(simTime);
		lastUpdateTime = simTime;
	}
}
