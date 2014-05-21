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
	protected final SampleExpInput derivativeTime;

	private double lastUpdateTime;  // The time at which the last update was performed
	private double lastInputValue;  // The input value for the last update

	{
		controllerRequired = true;

		derivativeTime = new SampleExpInput( "DerivativeTime", "Key Inputs", new SampleConstant(TimeUnit.class, 1.0) );
		derivativeTime.setUnitType(TimeUnit.class);
		derivativeTime.setEntity(this);
		this.addInput( derivativeTime);
	}

	@Override
	protected boolean repeatableInputs() {
		return super.repeatableInputs()
				&& ! (derivativeTime.getValue() instanceof Distribution);
	}

	@Override
	public void earlyInit() {
		lastUpdateTime = 0.0;
	}

	@Override
	protected double calculateValue(double simTime) {

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
