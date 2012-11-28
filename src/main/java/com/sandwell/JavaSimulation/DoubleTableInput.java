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

import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation3D.InputAgent;

public class DoubleTableInput extends Input<ArrayList<DoubleVector>> {
	protected double minValue = Double.NEGATIVE_INFINITY;
	protected double maxValue = Double.POSITIVE_INFINITY;
	protected double sumValue = Double.NaN;

	public DoubleTableInput(String key, String cat, ArrayList<DoubleVector> def) {
		super(key, cat, def);
	}

	public void parse(StringVector input) throws InputErrorException {
		ArrayList<StringVector> temp = Util.splitStringVectorByBraces(input);

		value = new ArrayList<DoubleVector>(temp.size());
		for (StringVector each : temp) {

			DoubleVector vec;

			// If there is more than one value, and the last one is not a number, then assume it is a unit
			if( each.size() > 1 && !Tester.isDouble( each.get( each.size()-1 ) ) ) {

				// Determine the units
				Unit unit = Input.parseUnits(each.get(each.size()- 1));

				// Determine the default units
				Unit defaultUnit = Input.tryParseEntity( unitString.replaceAll("[()]", "").trim(), Unit.class );
				if( defaultUnit == null ) {
					throw new InputErrorException( "Could not determine default units " + unitString );
				}

				// Determine the conversion factor to the default units
				double conversionFactor = unit.getConversionFactorToUnit( defaultUnit );

				vec = Input.parseDoubleVector(each.subString(0,each.size()-2), minValue, maxValue, conversionFactor);
			}
			else {
				vec = Input.parseDoubleVector(each, minValue, maxValue);

				if( unitString.length() > 0 )
					InputAgent.logWarning( "Missing units.  Assuming %s.", unitString );

			}

			if( ! Double.isNaN(sumValue) ) {
				Input.assertSumTolerance(vec, sumValue, 0.001d);
			}

			value.add(vec);
		}

		this.updateEditingFlags();
	}

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	public void setValidSum(double sum) {
		sumValue = sum;
	}
}
