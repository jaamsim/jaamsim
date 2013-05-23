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
import com.jaamsim.input.OutputInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Keyword;

/**
 * DoubleCalculation is the super-class for all calculations that return a double.
 * @author Harry King
 *
 */
public abstract class DoubleCalculation extends CalculationEntity {

	@Keyword(description = "The value to be used as an input to the present calculation.",
	         example = "Calculation1 InputValue { Tank-1.FluidLevel }")
	protected final OutputInput<Double> inputValueInput;

	private double value;  // Present value for this calculation

	{
		inputValueInput = new OutputInput<Double>( Double.class, "InputValue", "Key Inputs", null);
		this.addInput( inputValueInput, true);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the InputValue keyword has been set (unless it is hidden by the sub-class)
		if( ! inputValueInput.getHidden() && inputValueInput.getValue() == null ) {
			throw new InputErrorException( "The InputValue keyword must be set." );
		}
	}

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

	@Output(name = "InputValue",
	 description = "The input to the calculation at the present time.")
	public Double getInputValue( double simTime ) {
		return inputValueInput.getOutputValue( simTime );
	}

	@Output(name = "Value",
	 description = "The result of the calcuation at the present time.")
	public Double getValue( double simTime ) {
		return value;
	}
}
