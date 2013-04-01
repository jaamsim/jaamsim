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
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation3D.DisplayEntity;

/**
 * CalculationEntity is the super-class for all Calculation Objects.
 * @author Harry King
 *
 */
public abstract class CalculationEntity extends DisplayEntity {

	@Keyword(desc = "The Controller that controls the updating of the calculation.",
	         example = "Calculation1 Controller { PLC1 }")
	private final EntityInput<Controller> controllerInput;

	@Keyword(desc = "The sequence number used by the Controller to determine the order in which calculations are performed." +
			"  A calculation with a lower value is executed before the ones with higher values.",
	         example = "Calculation1 SequenceNumber { 2.1 }")
	private final DoubleInput sequenceNumberInput;

	{
		controllerInput = new EntityInput<Controller>( Controller.class, "Controller", "Key Inputs", null);
		this.addInput( controllerInput, true);

		sequenceNumberInput = new DoubleInput( "SequenceNumber", "Key Inputs", 0.0);
		sequenceNumberInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput( sequenceNumberInput, true);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that controller has been specified
		if( controllerInput.getValue() == null ) {
			throw new InputErrorException( "The keyword Controller must be set." );
		}
	}

	public double getSequenceNumber() {
		return sequenceNumberInput.getValue();
	}

	/*
	 * Calculate the current value for this object.
	 */
	public abstract void update();

	public Controller getController() {
		return controllerInput.getValue();
	}
}
