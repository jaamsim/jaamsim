/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation;

import java.util.ArrayList;

import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;

public class DoubleTableInput extends Input<ArrayList<DoubleVector>> {
	protected double minValue = Double.NEGATIVE_INFINITY;
	protected double maxValue = Double.POSITIVE_INFINITY;
	protected double sumValue = Double.NaN;

	public DoubleTableInput(String key, String cat, ArrayList<DoubleVector> def) {
		super(key, cat, def);
	}

	private String unitString = "";
	public void setUnits(String units) {
		unitString = units;
	}

	@Override
	public void parse(StringVector input) throws InputErrorException {
		ArrayList<StringVector> temp = InputAgent.splitStringVectorByBraces(input);
		ArrayList<DoubleVector> tab = new ArrayList<DoubleVector>(temp.size());
		for (StringVector each : temp) {
			DoubleVector vec = Input.parseDoubleVector(each, minValue, maxValue, unitString);
			if (!Double.isNaN(sumValue))
				Input.assertSumTolerance(vec, sumValue, 0.001d);

			tab.add(vec);
		}
		value = tab;
	}

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	public void setValidSum(double sum) {
		sumValue = sum;
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return NO_VALUE;

		if (defValue.size() == 0)
			return NO_VALUE;

		StringBuilder tmp = new StringBuilder();
		for (DoubleVector each: defValue) {

			// blank space between items
			if (tmp.length() > 0)
				tmp.append(SEPARATOR);

			if (each == null) {
				tmp.append(NO_VALUE);
				continue;
			}
			if (each.size() == 0) {
				tmp.append(NO_VALUE);
				continue;
			}

			tmp.append("{");
			tmp.append(SEPARATOR);
			for (int i = 0; i < each.size(); i++) {
				tmp.append(each.get(i));
				tmp.append(SEPARATOR);
			}
			if (!unitString.isEmpty()) {
				tmp.append(unitString);
				tmp.append(SEPARATOR);
			}
			tmp.append("}");
		}
		return tmp.toString();
	}
}
