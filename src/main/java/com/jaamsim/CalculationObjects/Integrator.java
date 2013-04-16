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

import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Keyword;

/**
 * The Integrator returns the integral of the input values.
 * @author Harry King
 *
 */
public class Integrator extends DoubleCalculation {

	@Keyword(desc = "The initial value for the integral at time = 0.",
	         example = "Integrator1 InitialValue { 5.5 }")
	private final DoubleInput initialValueInput;

	private double lastUpdateTime;  // The time at which the last update was performed

	{
		initialValueInput = new DoubleInput( "InitialValue", "Key Inputs", 0.0d);
		this.addInput( initialValueInput, true);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		this.setValue( initialValueInput.getValue() );
		lastUpdateTime = 0.0;
	}

	@Override
	public void update() {

		// Calculate the elapsed time
		double t = 3600.0 * this.getCurrentTime();  // convert from hours to seconds
		double dt = t - lastUpdateTime;
		lastUpdateTime = t;

		// Set the present value
		this.setValue( this.getInputValue(t) * dt  +  this.getValue() );
		return;
	}
}
