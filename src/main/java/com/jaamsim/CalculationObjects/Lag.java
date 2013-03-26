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
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Keyword;


/**
 * The Lag block is a standard control system component whose output is equal to integral( input - output ) / LagTime.
 * This operation has the effect of delaying and smoothing the input signal over the time scale given by LagTime.
 * @author Harry King
 *
 */
public class Lag extends DoubleCalculation {

	@Keyword(desc = "The entity whose output value is the input to this calculation.",
	         example = "Lag-1 Entity { Calc-1 }")
	private final EntityInput<DoubleCalculation> entityInput;

	@Keyword(desc = "The time constant for this operation: output = integral( input - output) / LagTime.",
	         example = "Lag-1 LagTime { 15 s }")
	private final DoubleInput lagTimeInput;

	private double lastUpdateTime;  // The time at which the last update was performed
	private double error;  // inputValue - outputValue
	private double integral; // The present value for the integral

	{
		entityInput = new EntityInput<DoubleCalculation>( DoubleCalculation.class, "Entity", "Key Inputs", null);
		this.addInput( entityInput, true);

		lagTimeInput = new DoubleInput( "LagTime", "Key Inputs", 1.0d);
		lagTimeInput.setValidRange(1.0e-10, Double.POSITIVE_INFINITY);
		lagTimeInput.setUnits("s");
		this.addInput( lagTimeInput, true);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the Entity keyword has been set
		if( entityInput.getValue() == null ) {
			throw new InputErrorException( "The Entity keyword must be set." );
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		integral = 0.0;
		lastUpdateTime = 0.0;
	}

	@Override
	public void update() {

		// Calculate the elapsed time
		double t = 3600.0 * this.getCurrentTime();  // convert from hours to seconds
		double dt = t - lastUpdateTime;
		lastUpdateTime = t;

		// Set the present value
		error = entityInput.getValue().getValue() - this.getValue();
		integral += error * dt;
		this.setValue( integral / lagTimeInput.getValue() );
		return;
	}

	@Output(name = "Error",
	 description = "The value for InputValue - OutputValue.")
	public double getError( double simTime ) {
		return error;
	}
}
