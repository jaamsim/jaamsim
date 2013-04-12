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

public class DoubleListInput extends ListInput<DoubleVector> {
	protected double minValue = Double.NEGATIVE_INFINITY;
	protected double maxValue = Double.POSITIVE_INFINITY;
	protected double sumValue = Double.NaN;
	protected int[] validCounts; // valid list sizes not including units

	public DoubleListInput(String key, String cat, DoubleVector def) {
		super(key, cat, def);
		validCounts = new int[] { };
	}

	@Override
	public void parse(StringVector input)
	throws InputErrorException {

		// Parse the inputs
		DoubleVector temp = Input.parseDoubleVector( input, minValue, maxValue, unitString);

		// Test the input for validity
		// (If there is more than one value, and the last one is not a number, then assume it is a unit)
		StringVector numericInput = new StringVector(input);
		if( input.size() > 1 && !Tester.isDouble( input.get( input.size()-1 ) ) ) {
			numericInput.remove(numericInput.size()-1);
		}
		Input.assertCountRange( numericInput, minCount, maxCount);
		Input.assertCount( numericInput, validCounts);
		if( ! Double.isNaN(sumValue) ) {
			Input.assertSumTolerance( temp, sumValue, 0.001d);
		}

		// Inputs are valid
		value = temp;
		this.updateEditingFlags();
	}

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	public void setValidSum(double sum) {
		sumValue = sum;
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

		if (!unitString.isEmpty()) {
			tmp.append(SEPARATOR);
			tmp.append(unitString);
		}

		return tmp.toString();
	}
}
