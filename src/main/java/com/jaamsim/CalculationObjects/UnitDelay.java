/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016-2022 JaamSim Software Inc.
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

import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class UnitDelay extends DoubleCalculation {

	@Keyword(description = "The initial value for the UnitDelay function.",
	         exampleList = {"5.5"})
	private final ValueInput initialValue;

	{
		initialValue = new ValueInput("InitialValue", KEY_INPUTS, 0.0d);
		initialValue.setUnitType(UserSpecifiedUnit.class);
		this.addInput(initialValue);
	}

	public UnitDelay() {}

	@Override
	protected void setUnitType(Class<? extends Unit> ut) {
		super.setUnitType(ut);
		initialValue.setUnitType(ut);
	}

	@Override
	public double getInitialValue() {
		return initialValue.getValue();
	}

	@Override
	protected double calculateValue(double simTime, double inputVal, double lastTime, double lastInputVal, double lastVal) {
		return inputVal;
	}

	@Override
	public double getNextSample(Entity thisEnt, double simTime) {
		return this.getLastValue();
	}

}
