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
 * BooleanCalculation is the super-class for all calculations that return a Boolean value.
 * @author Harry King
 *
 */
public abstract class BooleanCalculation extends CalculationEntity {

	private boolean value;  // Present value for this calculation

	public boolean getNextSample(double simTime) {
		if( this.getController() == null && ! updateInProgress ) {
			updateInProgress = true;
			this.update(simTime);
			updateInProgress = false;
		}
		return value;
	}

	/*
	 * Set the present value for this calculation.
	 */
	protected void setValue( boolean val ) {
		value = val;
	}

	@Output(name = "Value",
	 description = "The result of the calcuation at the present time.")
	public boolean getValue( double simTime ) {
		return value;
	}
}
