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

import com.sandwell.JavaSimulation3D.InputAgent;

public class DoubleListInput extends ListInput<DoubleVector> {
	protected double minValue = Double.NEGATIVE_INFINITY;
	protected double maxValue = Double.POSITIVE_INFINITY;
	protected double sumValue = Double.NaN;
	protected int[] validCounts; // valid list sizes not including units

	public DoubleListInput(String key, String cat, DoubleVector def) {
		super(key, cat, def);
		validCounts = new int[] { };
	}

	public void parse(StringVector input)
	throws InputErrorException {
		Input.assertCountRange(input, minCount, maxCount);
		DoubleVector temp;

		// If there is more than one value, and the last one is not a number, then assume it is a unit
		if( input.size() > 1 && !Tester.isDouble( input.get( input.size()-1 ) ) ) {

			// Determine the units
			Unit unit = Input.parseEntity( input.get( input.size()- 1), Unit.class );

			// Determine the default units
			Unit defaultUnit = Input.tryParseEntity( unitString.replaceAll("[()]", "").trim(), Unit.class );
			if( defaultUnit == null ) {
				throw new InputErrorException( "Could not determine default units " + unitString );
			}

			// Determine the conversion factor to the default units
			double conversionFactor = unit.getConversionFactorToUnit( defaultUnit );

			// Parse and convert the values
			Input.assertCount(input.subString(0,input.size()-2), validCounts);
			temp = Input.parseDoubleVector(input.subString(0,input.size()-2), minValue, maxValue, conversionFactor);
		}
		else {
			// Parse the values
			Input.assertCount(input, validCounts);
			temp = Input.parseDoubleVector(input, minValue, maxValue);

			if( unitString.length() > 0 )
				InputAgent.logWarning( "Missing units.  Assuming %s.", unitString );
		}

		if( ! Double.isNaN(sumValue) ) {
			Input.assertSumTolerance(temp, sumValue, 0.001d);
		}
		value = temp;
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
}
