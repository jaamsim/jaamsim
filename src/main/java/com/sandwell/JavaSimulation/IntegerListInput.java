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

public class IntegerListInput extends ListInput<IntegerVector> {
	private int minValue = Integer.MIN_VALUE;
	private int maxValue = Integer.MAX_VALUE;
	protected int[] validCounts; // valid list sizes not including units

	public IntegerListInput(String key, String cat, IntegerVector def) {
		super(key, cat, def);
		validCounts = new int[] { };
	}

	public void parse(StringVector input)
	throws InputErrorException {
		Input.assertCountRange(input, minCount, maxCount);
		Input.assertCount(input, validCounts);
		value = Input.parseIntegerVector(input, minValue, maxValue);
		this.updateEditingFlags();
	}

	public void setValidRange(int min, int max) {
		minValue = min;
		maxValue = max;
	}

	public void setValidCounts(int... list) {
		validCounts = list;
	}
}
