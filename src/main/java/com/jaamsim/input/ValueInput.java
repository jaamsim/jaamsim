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
package com.jaamsim.input;

import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class ValueInput extends Input<Double> {
	private Class<? extends Unit> unitType = DimensionlessUnit.class;
	private double minValue = Double.NEGATIVE_INFINITY;
	private double maxValue = Double.POSITIVE_INFINITY;

	public ValueInput(String key, String cat, Double def) {
		super(key, cat, def);
	}

	public void setUnitType(Class<? extends Unit> units) {
		unitType = units;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		DoubleVector temp = Input.parseDoubles(kw, minValue, maxValue, unitType);
		Input.assertCount(temp, 1);
		value = Double.valueOf(temp.get(0));
	}

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return NO_VALUE;

		StringBuilder tmp = new StringBuilder();
		if (defValue.doubleValue() == Double.POSITIVE_INFINITY) {
			tmp.append(POSITIVE_INFINITY);
		}
		else if (defValue.doubleValue() == Double.NEGATIVE_INFINITY) {
			tmp.append(NEGATIVE_INFINITY);
		}
		else {
			tmp.append(defValue.toString());
		}

		if (unitType != Unit.class) {
			tmp.append(SEPARATOR);
			tmp.append(Unit.getSIUnit(unitType));
		}

		return tmp.toString();
	}
}
