/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2018 JaamSim Software Inc.
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
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.DimensionlessUnit;

/**
 * CalculationEntity is the super-class for all Calculation Objects.
 * @author Harry King
 *
 */
public abstract class CalculationEntity extends DisplayEntity implements Controllable {

	@Keyword(description = "The Controller object that signals the updating of the calculation.",
	         exampleList = {"Controller1"})
	protected final EntityInput<Controller> controller;

	@Keyword(description = "The sequence number used by the Controller to determine the order "
	                     + "in which calculations are performed. A calculation with a lower value "
	                     + "is executed before one with a higher value.",
	         exampleList = {"2.1"})
	private final SampleInput sequenceNumber;

	{
		controller = new EntityInput<>(Controller.class, "Controller", KEY_INPUTS, null);
		controller.setRequired(true);
		this.addInput(controller);

		sequenceNumber = new SampleInput("SequenceNumber", KEY_INPUTS, 0.0);
		sequenceNumber.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		sequenceNumber.setUnitType(DimensionlessUnit.class);
		this.addInput(sequenceNumber);
	}

	@Override
	public Controller getController() {
		return controller.getValue();
	}

	@Override
	public double getSequenceNumber() {
		return sequenceNumber.getNextSample(this, 0.0d);
	}
}
