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

import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.ListInput;

public class ValueListInput extends ListInput<DoubleVector> {
	private Class<? extends Unit> unitType = DimensionlessUnit.class;
	private double minValue = Double.NEGATIVE_INFINITY;
	private double maxValue = Double.POSITIVE_INFINITY;
	private double sumValue = Double.NaN;
	private double sumTolerance = 1e-10d;
	private int[] validCounts = null; // valid list sizes not including units

	public ValueListInput(String key, String cat, DoubleVector def) {
		super(key, cat, def);
	}

	public void setUnitType(Class<? extends Unit> units) {
		unitType = units;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		DoubleVector temp = Input.parseDoubles(kw, minValue, maxValue, unitType);
		Input.assertCount(temp, validCounts);
		Input.assertCountRange(temp, minCount, maxCount);
		if (!Double.isNaN(sumValue))
			Input.assertSumTolerance(temp, sumValue, sumTolerance);

		value = temp;
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	public void setValidSum(double sum, double tol) {
		sumValue = sum;
		sumTolerance = tol;
	}

	public void setValidCounts(int... list) {
		validCounts = list;
	}

	@Override
	public String getDefaultString() {
		if (defValue == null || defValue.size() == 0) {
			StringBuilder tmp = new StringBuilder(NO_VALUE);
			if (unitType != Unit.class) {
				tmp.append(SEPARATOR);
				tmp.append(Unit.getSIUnit(unitType));
			}
			return tmp.toString();
		}

		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < defValue.size(); i++) {
			if (i > 0)
				tmp.append(SEPARATOR);
			tmp.append(defValue.get(i));
		}

		if (unitType != Unit.class) {
			tmp.append(SEPARATOR);
			tmp.append(Unit.getSIUnit(unitType));
		}

		return tmp.toString();
	}
}
