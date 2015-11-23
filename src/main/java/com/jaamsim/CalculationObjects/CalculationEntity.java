/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.CalculationObjects;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.DimensionlessUnit;

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
		controller = new EntityInput<>( Controller.class, "Controller", "Key Inputs", null);
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
