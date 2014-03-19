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

import com.jaamsim.input.Keyword;
import com.jaamsim.input.OutputInput;
import com.sandwell.JavaSimulation.InputErrorException;

/**
 * The Sensor connect the output from some other object to the DoubleCalculation components.
 * @author Harry King
 *
 */
public class Sensor extends DoubleCalculation {

	@Keyword(description = "The Entity and Output that provides the input to the Sensor.",
	         example = "Sensor-1 SensedOutput { StockPile-2 Contents }")
	private final OutputInput<Double> sensedOutput;

	{
		inputValue.setHidden(true);

		sensedOutput = new OutputInput<Double>( Double.class, "SensedOutput", "Key Inputs", null);
		this.addInput(sensedOutput);
	}

	@Override
	public void validate() {
		super.validate();

		// Check that the entity has been set
		if ( sensedOutput.getValue() == null ) {
			throw new InputErrorException( "The SensedOutput keyword must be set." );
		}
	}

	@Override
	public double calculateValue(double simTime) {
		return sensedOutput.getOutputValueAsDouble(simTime, 0.0);
	}
}
