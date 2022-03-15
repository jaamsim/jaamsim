/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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
package com.jaamsim.BasicObjects;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueListInput;
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
		unitType = new UnitTypeInput("UnitType", KEY_INPUTS, UserSpecifiedUnit.class);
		unitType.setRequired(true);
		unitType.setCallback(unitTypeCallback);
		this.addInput(unitType);

		valueList = new ValueListInput("ValueList", KEY_INPUTS, null);
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

	static final InputCallback unitTypeCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((ValueSequence)ent).updateUnitType();
		}
	};

	void updateUnitType() {
		valueList.setUnitType(getUnitType());
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
