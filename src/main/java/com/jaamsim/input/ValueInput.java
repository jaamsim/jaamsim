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
package com.jaamsim.input;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.ui.GUIFrame;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class ValueInput extends Input<Double> {
	private Class<? extends Unit> unitType = DimensionlessUnit.class;
	private double minValue = Double.NEGATIVE_INFINITY;
	private double maxValue = Double.POSITIVE_INFINITY;

	public ValueInput(String key, String cat, Double def) {
		super(key, cat, def);
	}

	public void setUnitType(Class<? extends Unit> units) {

		if (units == unitType)
			return;

		this.setValid(false);
		unitType = units;
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		DoubleVector temp = Input.parseDoubles(thisEnt.getJaamSimModel(), kw, minValue, maxValue, unitType);
		Input.assertCount(temp, 1);
		value = Double.valueOf(temp.get(0));
		this.setValid(true);
	}

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	@Override
	public String getValidInputDesc() {
		if (unitType == UserSpecifiedUnit.class) {
			return Input.VALID_VALUE_UNIT;
		}
		if (unitType == DimensionlessUnit.class) {
			return Input.VALID_VALUE_DIMLESS;
		}
		return String.format(Input.VALID_VALUE, unitType.getSimpleName());
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return "";

		StringBuilder tmp = new StringBuilder();
		if (defValue.doubleValue() == Double.POSITIVE_INFINITY) {
			tmp.append(POSITIVE_INFINITY);
		}
		else if (defValue.doubleValue() == Double.NEGATIVE_INFINITY) {
			tmp.append(NEGATIVE_INFINITY);
		}
		else {
			tmp.append(defValue/GUIFrame.getJaamSimModel().getDisplayedUnitFactor(unitType));
		}

		if (unitType != Unit.class) {
			tmp.append(SEPARATOR);
			tmp.append(GUIFrame.getJaamSimModel().getDisplayedUnit(unitType));
		}

		return tmp.toString();
	}
}
