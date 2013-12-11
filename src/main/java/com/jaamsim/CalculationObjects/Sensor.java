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
import com.jaamsim.input.OutputHandle;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringInput;

/**
 * The Sensor connect the output from some other object to the DoubleCalculation components.
 * @author Harry King
 *
 */
public class Sensor extends DoubleCalculation {

	@Keyword(description = "The entity to read the output from.",
	         example = "Sensor1 Entity { StockPile2 }")
	private final EntityInput<Entity> entityInput;

	@Keyword(description = "The name of the output to read.",
	         example = "Sensor1 OutputName { 'Contents' }")
	private final StringInput outputNameInput;

	{
		inputValueInput.setHidden(true);

		entityInput = new EntityInput<Entity>( Entity.class, "Entity", "Key Inputs", null);
		this.addInput(entityInput, true);

		outputNameInput = new StringInput( "OutputName", "Key Inputs", null);
		this.addInput(outputNameInput, true);
	}

	@Override
	public void validate() {
		super.validate();

		// Check that the entity has been set
		if ( entityInput.getValue() == null ) {
			throw new InputErrorException( "The Entity keyword must be set." );
		}

		// Check that the output has been set
		if ( outputNameInput.getValue() == null ) {
			throw new InputErrorException( "The OutputName keyword must be set." );
		}

		// Check that the output is a double or a Double
		Entity ent = entityInput.getValue();
		String name = outputNameInput.getValue();
		OutputHandle out = ent.getOutputHandle(name);
		if (!out.isNumericValue())
			throw new InputErrorException( "The OutputName keyword must specify a floating point output." );
	}

	@Override
	public void update(double simTime) {
		double val = 0.0;
		Entity ent = entityInput.getValue();
		String name = outputNameInput.getValue();

		OutputHandle out = ent.getOutputHandle(name);
		if (out.isNumericValue())
			val = out.getValueAsDouble(simTime, 0.0d);

		// Set the present value
		this.setValue( val );
	}
}
