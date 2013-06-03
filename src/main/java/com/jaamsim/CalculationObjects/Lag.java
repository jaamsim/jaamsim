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

import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.Keyword;

/**
 * The Lag block is a standard control system component whose output is equal to integral( input - output ) / LagTime.
 * This operation has the effect of delaying and smoothing the input signal over the time scale given by LagTime.
 * @author Harry King
 *
 */
public class Lag extends DoubleCalculation {

	@Keyword(description = "The time constant for this operation: output = integral( input - output) / LagTime.",
	         example = "Lag-1 LagTime { 15 s }")
	private final ValueInput lagTimeInput;

	private double lastUpdateTime;  // The time at which the last update was performed
	private double error;  // inputValue - outputValue
	private double integral; // The present value for the integral

	{
		lagTimeInput = new ValueInput( "LagTime", "Key Inputs", 1.0d);
		lagTimeInput.setValidRange(1.0e-10, Double.POSITIVE_INFINITY);
		lagTimeInput.setUnitType(TimeUnit.class);
		this.addInput( lagTimeInput, true);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		integral = 0.0;
		lastUpdateTime = 0.0;
	}

	@Override
	public void update(double simTime) {

		// Calculate the elapsed time
		double dt = simTime - lastUpdateTime;
		lastUpdateTime = simTime;

		// Set the present value
		error = inputValueInput.getOutputValue(simTime) - this.getValue();
		integral += error * dt;
		this.setValue( integral / lagTimeInput.getValue() );
		return;
	}

	@Output(name = "Error",
	 description = "The value for InputValue - OutputValue.")
	public Double getError( double simTime ) {
		return error;
	}
}
