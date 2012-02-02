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

public class IntegerInput extends Input<Integer> {
	private int minValue;
	private int maxValue;

	public IntegerInput(String key, String cat, Integer def) {
		super(key, cat, def);
	}

	public void parse(StringVector input)
	throws InputErrorException {
		Input.assertCount(input, 1);
		value = Input.parseInteger(input.get(0), minValue, maxValue);
		this.updateEditingFlags();
	}

	public void setValidRange(int min, int max) {
		minValue = min;
		maxValue = max;
	}
}
