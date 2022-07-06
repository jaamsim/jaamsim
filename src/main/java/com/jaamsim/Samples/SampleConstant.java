/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2020 JaamSim Software Inc.
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
package com.jaamsim.Samples;

import java.util.ArrayList;

import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.Input;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class SampleConstant implements SampleProvider {
	private Class<? extends Unit> unitType;
	private boolean integerValue;
	private final double val;

	public SampleConstant(Class<? extends Unit> unitType, double val) {
		this.unitType = unitType;
		this.val = val;
		integerValue = false;
	}

	public SampleConstant(double val) {
		this(DimensionlessUnit.class, val);
	}

	public SampleConstant(int val) {
		this(DimensionlessUnit.class, val);
		integerValue = true;
	}

	void setUnitType(Class<? extends Unit> ut) {
		unitType = ut;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType;
	}

	@Override
	public double getNextSample(double simTime) {
		return val;
	}

	@Override
	public double getMeanValue(double simTime) {
		return val;
	}

	public String getValueString(JaamSimModel simModel) {
		if (integerValue)
			return Integer.toString((int) val);

		StringBuilder tmp = new StringBuilder();
		tmp.append(Double.toString(val/simModel.getDisplayedUnitFactor(unitType)));
		if (unitType != DimensionlessUnit.class)
			tmp.append(Input.SEPARATOR).append(simModel.getDisplayedUnit(unitType));
		return tmp.toString();
	}

	@Override
	public String toString() {
		if (integerValue)
			return Integer.toString((int) val);

		StringBuilder tmp = new StringBuilder();
		tmp.append(Double.toString(val));
		if (unitType != DimensionlessUnit.class)
			tmp.append(Input.SEPARATOR).append(Unit.getSIUnit(unitType));
		return tmp.toString();
	}

	public void getValueTokens(ArrayList<String> toks) {

		if (integerValue) {
			toks.add(Integer.toString((int) val));
			return;
		}

		toks.add(Double.toString(val));
		if (unitType != DimensionlessUnit.class)
			toks.add(Unit.getSIUnit(unitType));
		return;
	}
}
