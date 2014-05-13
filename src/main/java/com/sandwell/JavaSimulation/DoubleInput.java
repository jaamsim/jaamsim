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

public class DoubleInput extends Input<Double> {
	private double minValue = Double.NEGATIVE_INFINITY;
	private double maxValue = Double.POSITIVE_INFINITY;

	public DoubleInput(String key, String cat, Double def) {
		super(key, cat, def);
	}

	private String unitString = "";
	public void setUnits(String units) {
		unitString = units;
	}

	public String getUnits() {
		return unitString;
	}

	@Override
	public void parse(StringVector input)
	throws InputErrorException {
		value = Input.parseDouble( input, minValue, maxValue, unitString);
	}

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return NO_VALUE;

		if (defValue.doubleValue() == Double.POSITIVE_INFINITY)
			return POSITIVE_INFINITY;

		if (defValue.doubleValue() == Double.NEGATIVE_INFINITY)
			return NEGATIVE_INFINITY;

		StringBuilder tmp = new StringBuilder(defValue.toString());

		if (!unitString.isEmpty()) {
			tmp.append(SEPARATOR);
			tmp.append(unitString);
		}

		return tmp.toString();
	}
}
