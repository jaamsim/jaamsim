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

import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.Keyword;

/**
 * The BooleanConstant return a constant Boolean value.
 * @author Harry King
 *
 */
public class ConstantBoolean extends BooleanCalculation {

	@Keyword(description = "The Boolean constant value to be returned by this object.",
	         example = "ConstantBoolean1 Value { TRUE  FALSE }")
	private final BooleanInput valueInput;

	{
		valueInput = new BooleanInput( "Value", "Key Inputs", true);
		this.addInput( valueInput, true);
	}

	@Override
	public void update(double simTime) {

		// Set the present value
		this.setValue( valueInput.getValue() );
		return;
	}
}
