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
package com.jaamsim.input;


public class IntegerInput extends Input<Integer> {
	private int minValue = Integer.MIN_VALUE;
	private int maxValue = Integer.MAX_VALUE;

	public IntegerInput(String key, String cat, Integer def) {
		super(key, cat, def);
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);
		value = Input.parseInteger(kw.getArg(0), minValue, maxValue);
	}

	public void setValidRange(int min, int max) {
		minValue = min;
		maxValue = max;
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return NO_VALUE;

		if (defValue.intValue() == Integer.MAX_VALUE)
			return POSITIVE_INFINITY;

		if (defValue.intValue() == Integer.MIN_VALUE)
			return NEGATIVE_INFINITY;

		StringBuilder tmp = new StringBuilder(defValue.toString());

		return tmp.toString();
	}
}
