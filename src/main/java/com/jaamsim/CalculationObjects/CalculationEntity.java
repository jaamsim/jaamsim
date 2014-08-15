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

import com.jaamsim.input.EntityInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.DimensionlessUnit;
import com.sandwell.JavaSimulation3D.DisplayEntity;

/**
 * CalculationEntity is the super-class for all Calculation Objects.
 * @author Harry King
 *
 */
public abstract class CalculationEntity extends DisplayEntity {

	@Keyword(description = "The Controller that controls the updating of the calculation.\n" +
			"If a Controller is not specified, the calculation is performed asynchronously, on demand.",
	         example = "Calculation1 Controller { PLC1 }")
	private final EntityInput<Controller> controller;

	@Keyword(description = "The sequence number used by the Controller to determine the order in which calculations are performed." +
			"  A calculation with a lower value is executed before the ones with higher values.",
	         example = "Calculation1 SequenceNumber { 2.1 }")
	private final ValueInput sequenceNumber;

	protected boolean calculationInProgress = false;  // TRUE if a value calculation has been started, but not completed yet
	protected boolean controllerRequired = false;  // TRUE if the Controller keyword must be set

	{
		controller = new EntityInput<Controller>( Controller.class, "Controller", "Key Inputs", null);
		this.addInput( controller);

		sequenceNumber = new ValueInput( "SequenceNumber", "Key Inputs", 0.0);
		sequenceNumber.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		sequenceNumber.setUnitType(DimensionlessUnit.class);
		this.addInput( sequenceNumber);
	}

	@Override
	public void validate() {
		super.validate();
		if( controllerRequired && controller.getValue() == null )
			throw new InputErrorException( "The keyword Controller must be set." );
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		calculationInProgress = false;
	}

	public double getSequenceNumber() {
		return sequenceNumber.getValue();
	}

	/*
	 * Calculate the current value for this object.
	 */
	public abstract void update(double simTime);

	public Controller getController() {
		return controller.getValue();
	}
}
