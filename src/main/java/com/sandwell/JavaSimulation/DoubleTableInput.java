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

import com.jaamsim.input.InputAgent;
import com.jaamsim.units.Unit;

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

				if (defaultUnit.getClass() != unit.getClass())
					throw new InputErrorException( "Cannot convert from %s to %s", defaultUnit.getName(), unit.getName());
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
