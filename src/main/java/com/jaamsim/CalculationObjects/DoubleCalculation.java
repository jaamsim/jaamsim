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

/**
 * DoubleCalculation is the super-class for all calculations that return a double.
 * @author Harry King
 *
 */
public abstract class DoubleCalculation extends CalculationEntity {

	private double value;  // Present value for this calculation

	@Override
	public void earlyInit() {
		super.earlyInit();
		value = 0.0;
	}

	/*
	 * Return the present value for this calculation.
	 */
	public double getValue() {
		return value;
	}

	/*
	 * Set the present value for this calculation.
	 */
	protected void setValue( double val ) {
		value = val;
	}

	@Output(name = "Value",
	 description = "The result of the calcuation at the present time.")
	public double getValue( double simTime ) {
		return value;
	}
}
