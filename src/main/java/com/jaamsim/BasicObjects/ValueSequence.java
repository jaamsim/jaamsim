/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class ValueSequence extends DisplayEntity implements SampleProvider {

	@Keyword(description = "The unit type for the generated values.",
	         exampleList = {"DistanceUnit"})
	protected final UnitTypeInput unitType;

	@Keyword(description = "The sequence of numbers to be generated. Note that the appropriate "
	                     + "unit for the numbers must be entered in the last position.",
	         exampleList = {"10.2  12.4  7.2  m"})
	private final ValueListInput valueList;

	private int index = -1;

	{
		unitType = new UnitTypeInput("UnitType", "Key Inputs", UserSpecifiedUnit.class);
		unitType.setRequired(true);
		this.addInput(unitType);

		valueList = new ValueListInput("ValueList", "Key Inputs", null);
		valueList.setUnitType(UserSpecifiedUnit.class);
		valueList.setRequired(true);
		this.addInput(valueList);
	}

	public ValueSequence() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		index = -1;
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == unitType) {
			valueList.setUnitType(getUnitType());
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
		return valueList.getValue().sum()/valueList.getListSize();
	}

	@Override
	public double getMinValue() {
		return valueList.getValue().getMin();
	}

	@Override
	public double getMaxValue() {
		return valueList.getValue().getMax();
	}

	@Output(name = "Index",
	 description = "The position of the last value returned in the list.",
	    unitType = DimensionlessUnit.class,
	    sequence = 0)
	public int getIndexOfSample(double simTime) {
		return index+1;
	}

	@Output(name = "Value",
	 description = "The last value returned from the sequence. When used in an "
	             + "expression, this output returns a new value every time the expression "
	             + "is evaluated.",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 1)
	@Override
	public double getNextSample(double simTime) {

		if (valueList.getValue() == null)
			return Double.NaN;

		// If called from a model thread, increment the index to be selected
		if (EventManager.hasCurrent())
			index = (index + 1) % valueList.getListSize();

		// Trap an index that is out of range. Note that index can exceed the size of the list
		// if the ValueList keyword is edited in the middle of a run
		if (index < 0 || index >= valueList.getListSize())
			return Double.NaN;

		return valueList.getValue().get(index);
	}

}
