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

/**
 * The differentiator returns the derivative of the input signal with respect to time.
 * @author Harry King
 *
 */
public class Differentiator extends DoubleCalculation {

	private double lastUpdateTime;  // The time at which the last update was performed
	private double lastInputValue;  // The input value for the last update

	@Override
	public void update(double simtime) {

		// Calculate the elapsed time
		double t = 3600.0 * simtime;  // convert from hours to seconds
		double dt = t - lastUpdateTime;

		// Set the present value
		double val = inputValueInput.getOutputValue(simtime);
		if( dt > 0.0 ) {
			this.setValue( ( val - lastInputValue ) / dt );
		}

		// Record values needed for the next update
		lastUpdateTime = t;
		lastInputValue = val;
		return;
	}
}
