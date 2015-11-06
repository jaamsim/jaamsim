/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.BasicObjects;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class ExpressionEntity extends DisplayEntity implements SampleProvider {

	@Keyword(description = "The unit type for the returned expression values.",
	         exampleList = {"DistanceUnit"})
	protected final UnitTypeInput unitType;

	@Keyword(description = "The expression to be evaluated.",
	         exampleList = {"'[Queue1].QueueLength + [Queue2].QueueLength'"})
	private final SampleExpInput sampleValue;

	{
		unitType = new UnitTypeInput("UnitType", "Key Inputs", UserSpecifiedUnit.class);
		unitType.setRequired(true);
		this.addInput(unitType);

		sampleValue = new SampleExpInput("Expression", "Key Inputs", null);
		sampleValue.setUnitType(UserSpecifiedUnit.class);
		sampleValue.setEntity(this);
		sampleValue.setRequired(true);
		this.addInput(sampleValue);
	}

	public ExpressionEntity() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == unitType) {
			sampleValue.setUnitType(getUnitType());
			FrameBox.reSelectEntity();  // Update the units in the Output Viewer
			return;
		}
	}

	@Override
	public Class<? extends Unit> getUserUnitType() {
		return unitType.getUnitType();
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType.getUnitType();
	}

	@Override
	public double getMeanValue(double simTime) {
		return 0;
	}

	@Override
	public double getMinValue() {
		return Double.NEGATIVE_INFINITY;
	}

	@Override
	public double getMaxValue() {
		return Double.POSITIVE_INFINITY;
	}

	@Override
	@Output(name = "Value",
	 description = "The evaluated value of the expression.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true)
	public double getNextSample(double simTime) {

		if (sampleValue.getValue() == null)
			return 0.0d;

		return sampleValue.getValue().getNextSample(simTime);
	}

}
