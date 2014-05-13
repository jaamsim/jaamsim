/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2011 Ausenco Engineering Canada Inc.
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

import com.jaamsim.input.Input;

public class IntegerListInput extends ListInput<IntegerVector> {
	private int minValue = Integer.MIN_VALUE;
	private int maxValue = Integer.MAX_VALUE;
	protected int[] validCounts; // valid list sizes not including units

	public IntegerListInput(String key, String cat, IntegerVector def) {
		super(key, cat, def);
		validCounts = new int[] { };
	}

	@Override
	public void parse(StringVector input)
	throws InputErrorException {
		Input.assertCountRange(input, minCount, maxCount);
		Input.assertCount(input, validCounts);
		value = Input.parseIntegerVector(input, minValue, maxValue);
	}

	public void setValidRange(int min, int max) {
		minValue = min;
		maxValue = max;
	}

	public void setValidCounts(int... list) {
		validCounts = list;
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return NO_VALUE;

		if (defValue.size() == 0)
			return NO_VALUE;

		StringBuilder tmp = new StringBuilder();
		tmp.append(defValue.get(0));
		for (int i = 1; i < defValue.size(); i++) {
			tmp.append(SEPARATOR);
			tmp.append(defValue.get(i));
		}

		return tmp.toString();
	}
}
