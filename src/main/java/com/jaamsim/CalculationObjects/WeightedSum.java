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

import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.DoubleListInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Keyword;

/**
 * The WeightedSum object returns a weighted sum of its input values.
 * @author Harry King
 *
 */
public class WeightedSum extends DoubleCalculation {

	@Keyword(desc = "The list of DoubleCalculations entities that are inputs to this calculation.",
	         example = "WeightedSum1 EntityList { Calc1  Calc2 }")
	private final EntityListInput<DoubleCalculation> entityListInput;

	@Keyword(desc = "The list of multaplicative factors to be applied to the value provide by the inputs.",
	         example = "WeightedSum1 CoefficientList { 2.0  1.5 }")
	private final DoubleListInput coefficientListInput;

	{
	entityListInput = new EntityListInput<DoubleCalculation>( DoubleCalculation.class, "EntityList", "Key Inputs", null);
		this.addInput( entityListInput, true);

		coefficientListInput = new DoubleListInput( "CoefficientList", "Key Inputs", null);
		this.addInput( coefficientListInput, true);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the number of entries in the CoeffientList matches the EntityList
		if( coefficientListInput.getValue().size() != entityListInput.getValue().size() ) {
			throw new InputErrorException( "The number of entries for CoefficientList and EntityList must be equal" );
		}
	}

	@Override
	public void update(double simtime) {
		double val = 0.0;

		// Calculate the weighted sum
		for(int i=0; i<entityListInput.getValue().size(); i++ ) {
			val += coefficientListInput.getValue().get(i) * entityListInput.getValue().get(i).getValue();
		}

		// Set the present value
		this.setValue( val );
		return;
	}
}
