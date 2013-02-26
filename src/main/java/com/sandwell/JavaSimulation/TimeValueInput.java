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

import com.jaamsim.input.InputAgent;
import com.jaamsim.units.Unit;

public class TimeValueInput extends Input<TimeValue> {
	private double minValue;
	private double maxValue;

	public TimeValueInput(String key, String cat, TimeValue def) {
		this(key, cat, def, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	public TimeValueInput(String key, String cat, TimeValue def, double min, double max) {
		super(key, cat, def);
		minValue = min;
		maxValue = max;
	}

	@Override
	public void parse(StringVector input)
	throws InputErrorException {
		Input.assertCount(input, 1, 2, 12, 13);

		// If there are 2 or 13 entries, assume the last entry is a unit
		if( input.size() == 2 || input.size() == 13 ) {

			// Determine the units
			Unit unit = Input.parseUnits(input.get(input.size()- 1));

			// Determine the default units
			Unit defaultUnit = Input.tryParseEntity( unitString.replaceAll("[()]", "").trim(), Unit.class );
			if( defaultUnit == null ) {
				throw new InputErrorException( "Could not determine default units " + unitString );
			}

			// Determine the conversion factor to the default units
			double conversionFactor = unit.getConversionFactorToUnit( defaultUnit );

			// Parse and convert the values
			StringVector temp = input.subString(0,input.size()-2);
			value = Input.parseTimeValue(temp, minValue, maxValue, conversionFactor);
			if(value.isProbablity() && unit != defaultUnit) {
				throw new InputErrorException( "the only allowed unit for this ProbabilityDistribution(s) is '%s'", defaultUnit );
			}
		}
		else {
			// Parse the values
			value = Input.parseTimeValue(input, minValue, maxValue);

			if( unitString.length() > 0 )
				InputAgent.logWarning( "Missing units.  Assuming %s.", unitString );
		}
		this.updateEditingFlags();
	}

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return NO_VALUE;

		String def = defValue.getString();

		// no need for unit if infinity
		if (isInfinity(def))
			return def;

		StringBuilder tmp = new StringBuilder();
		tmp.append(def);

		if (!unitString.isEmpty()) {
			tmp.append(SEPARATOR);
			tmp.append(unitString);
		}

		return tmp.toString();
	}
}
