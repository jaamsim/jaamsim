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

import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Keyword;

/**
 * The differentiator returns the derivative of the input signal with respect to time.
 * @author Harry King
 *
 */
public class Differentiator extends DoubleCalculation {

	@Keyword(desc = "The entity whose output value is to be differentiated.",
	         example = "Differentiator1 Entity { Calc1 }")
	private final EntityInput<DoubleCalculation> entityInput;

	private double lastUpdateTime;  // The time at which the last update was performed
	private double lastInputValue;  // The input value for the last update

	{
		entityInput = new EntityInput<DoubleCalculation>( DoubleCalculation.class, "Entity", "Key Inputs", null);
		this.addInput( entityInput, true);
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
	public void update() {

		// Calculate the elapsed time
		double dt = this.getCurrentTime() - lastUpdateTime;

		// Set the present value
		if( dt > 0.0 ) {
			this.setValue( ( entityInput.getValue().getValue() - lastInputValue ) / dt );
		}

		// Record values needed for the next update
		lastUpdateTime = this.getCurrentTime();
		lastInputValue = entityInput.getValue().getValue();
		return;
	}
}
