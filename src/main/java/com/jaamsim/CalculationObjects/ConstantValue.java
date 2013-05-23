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
 * The ConstantValue object simply returns a constant value.
 * @author Harry King
 *
 */
public class ConstantValue extends DoubleCalculation {

	@Keyword(description = "The numerical value returned by the object.",
	         example = "ConstantValue1 Value { 5.0 }")
	private final DoubleInput valueInput;

	{
		inputValueInput.setHidden(true);

		valueInput = new DoubleInput( "Value", "Key Inputs", 0.0);
		this.addInput( valueInput, true);
	}

	@Override
	public void update(double simtime) {
		this.setValue( valueInput.getValue() );
	}
}
