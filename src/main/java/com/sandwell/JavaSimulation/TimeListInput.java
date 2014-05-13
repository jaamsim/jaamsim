/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2012 Ausenco Engineering Canada Inc.
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
import com.jaamsim.input.InputAgent;
import com.jaamsim.units.Unit;

public class TimeListInput extends ListInput<DoubleVector> {
	protected double minValue = Double.NEGATIVE_INFINITY;
	protected double maxValue = Double.POSITIVE_INFINITY;
	protected int[] validCounts; // valid list sizes not including units

	public TimeListInput(String key, String cat, DoubleVector def) {
		super(key, cat, def);
		validCounts = new int[] { };
	}

	private String unitString = "";
	public void setUnits(String units) {
		unitString = units;
	}

	@Override
	public void parse(StringVector input)
	throws InputErrorException {
		DoubleVector temp;

		// If there is more than one value, and the last one is not a number, then assume it is a unit
		if( input.size() > 1 && !Tester.isDouble( input.get( input.size()-1 )) ) {

			// Determine the units
			Unit unit = Input.parseUnits(input.get(input.size()- 1));

			// Determine the default units
			Unit defaultUnit = Input.tryParseEntity(unitString.replaceAll("[()]", "").trim(), Unit.class);
			if( defaultUnit == null ) {
				throw new InputErrorException("Could not determine default units %s", unitString);
			}

			if (defaultUnit.getClass() != unit.getClass())
				throw new InputErrorException( "Cannot convert from %s to %s", defaultUnit.getName(), unit.getName());

			// Determine the conversion factor to the default units
			double conversionFactor = unit.getConversionFactorToUnit(defaultUnit);

			// Parse and convert the values
			Input.assertCountRange(input.subString(0, input.size()-2), minCount, maxCount);
			Input.assertCount(input.subString(0,input.size()-2), validCounts);
			temp = Input.parseTimeVector(input.subString(0,input.size()-2), minValue, maxValue, conversionFactor);
		}
		else {
			// Parse the values
			Input.assertCountRange(input, minCount, maxCount);
			Input.assertCount(input, validCounts);
			temp = Input.parseTimeVector(input, minValue, maxValue);

			if(unitString.length() > 0)
				InputAgent.logWarning("Missing units.  Assuming %s.", unitString);
		}

		value = temp;
	}

	public void setValidRange(double min, double max) {
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
		if (!unitString.isEmpty()) {
			tmp.append(SEPARATOR);
			tmp.append(unitString);
		}

		return tmp.toString();
	}
}
