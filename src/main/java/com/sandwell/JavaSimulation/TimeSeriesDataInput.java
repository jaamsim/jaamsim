/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class TimeSeriesDataInput extends Input<TimeSeriesData> {
	private Class<? extends Unit> unitType;

	private double maxValue = Double.POSITIVE_INFINITY;
	private double minValue = Double.NEGATIVE_INFINITY;

	public TimeSeriesDataInput(String key, String cat, TimeSeriesData def) {
		super(key, cat, def);
		unitType = DimensionlessUnit.class;
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {

		if (unitType == UserSpecifiedUnit.class)
			throw new InputErrorException(INP_ERR_UNITUNSPECIFIED);

		long startingYearOffset = -1;
		long lastTime = -1;

		DoubleVector times = new DoubleVector(kw.numArgs()/4);
		DoubleVector values = new DoubleVector(kw.numArgs()/4);

		// Determine records in the time series
		// Records have form: (e.g.) yyyy-MM-dd HH:mm value units
		// where units are optional
		StringVector each = new StringVector();
		for (int i=0; i < kw.numArgs(); i++) {

			//skip over opening brace if present
			if (kw.getArg(i).equals("{") )
				continue;

			each.clear();

			// Load one record into 'each' containing an individual timeseries record
			for (int j = i; j < kw.numArgs(); j++, i++){
				if (kw.getArg(j).equals("}"))
					break;

				each.add(kw.getArg(j));
			}

			// Check the number of entries in the record
			Input.assertCountRange(each, 2, 3);

			long recordus = Input.parseRFC8601DateTime(each.get(0));
			// Make sure the times are in increasing order
			if (recordus <= lastTime)
				throw new InputErrorException( "The times must be given in increasing order on " + each.get(0));

			lastTime = recordus;

			// set the offset to the number of whole years from the first record
			if (startingYearOffset == -1) {
				startingYearOffset = recordus / Input.usPerYr;
				startingYearOffset *= Input.usPerYr;
			}

			long usOffset = recordus - startingYearOffset;

			each.remove(0);  // We've finished parsing the date at this point
			DoubleVector v = Input.parseDoubles(each, minValue, maxValue, unitType);

			times.add(usOffset / 3.6e9d); // convert to hours 3600 secs * 1e6 us
			values.add(v.get(0));
		}

		// Set the value to a new time series data object
		value = new TimeSeriesData( times, values );
	}

	public void setUnitType(Class<? extends Unit> u) {
		unitType = u;
	}
}
