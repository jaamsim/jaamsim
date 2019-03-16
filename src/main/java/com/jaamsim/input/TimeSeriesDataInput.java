/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2019 JaamSim Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.input;

import java.util.ArrayList;

import com.jaamsim.Samples.TimeSeriesData;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class TimeSeriesDataInput extends Input<TimeSeriesData> {
	private Class<? extends Unit> unitType;
	private double tickLength;  // simulation clock tick length used to convert times into ticks
	private double maxValue = Double.POSITIVE_INFINITY;
	private double minValue = Double.NEGATIVE_INFINITY;

	public TimeSeriesDataInput(String key, String cat, TimeSeriesData def) {
		super(key, cat, def);
		unitType = DimensionlessUnit.class;
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {

		boolean braceOpened = false;

		if (unitType == UserSpecifiedUnit.class)
			throw new InputErrorException(INP_ERR_UNITUNSPECIFIED);

		long startingYearOffset = -1;
		long lastTime = -1;

		DoubleVector times = new DoubleVector(kw.numArgs()/4);
		DoubleVector values = new DoubleVector(kw.numArgs()/4);

		// Determine records in the time series
		// Records have form: (e.g.) yyyy-MM-dd HH:mm value units
		// where units are optional
		ArrayList<String> each = new ArrayList<>();
		for (int i=0; i < kw.numArgs(); i++) {

			//skip over opening brace if present
			if (kw.getArg(i).equals("{") ) {
				braceOpened = true;
				continue;
			}

			each.clear();

			// Load one record into 'each' containing an individual timeseries record
			for (int j = i; j < kw.numArgs(); j++, i++){
				if (kw.getArg(j).equals("}")) {
					braceOpened = false;
					break;
				}

				if (!braceOpened)
					throw new InputErrorException("Expected an opening brace ( { ). Received: %s", kw.getArg(j));

				each.add(kw.getArg(j));
			}

			// Time input in RFC8601 date/time format
			long recordus;
			if (Input.isRFC8601DateTime(each.get(0))) {
				Input.assertCountRange(each, 2, 3);
				recordus = Input.parseRFC8601DateTime(each.get(0));
				each.remove(0);
			}
			// Time input in number/unit format
			else {
				// Parse the unit portion of the time input
				Input.assertCountRange(each, 3, 4);
				String unitName = Parser.removeEnclosure("[", each.get(1), "]");
				TimeUnit unit = Input.tryParseUnit(unitName, TimeUnit.class);
				if (unit == null)
					throw new InputErrorException(INP_ERR_NOUNITFOUND, each.get(1), "TimeUnit");

				// Parse the numeric portion of the time input
				double factor = unit.getConversionFactorToSI();
				recordus = (long) (Input.parseDouble(each.get(0), 0.0, Double.POSITIVE_INFINITY, factor)*1e6);
				each.remove(0);
				each.remove(0);
			}

			// Make sure the times are in increasing order
			if (recordus <= lastTime)
				throw new InputErrorException("The times must be given in increasing order.");

			lastTime = recordus;

			// set the offset to the number of whole years from the first record
			if (startingYearOffset == -1) {
				startingYearOffset = recordus / Input.usPerYr;
				startingYearOffset *= Input.usPerYr;
			}

			long usOffset = recordus - startingYearOffset;

			// Value portion of the record
			KeywordIndex valKw = new KeywordIndex("", each, null);
			DoubleVector v = Input.parseDoubles(thisEnt.getJaamSimModel(), valKw, minValue, maxValue, unitType);

			// Store the time and value for this record
			times.add( usOffset/(1.0e6*tickLength) );
			values.add(v.get(0));
		}

		if (braceOpened)
			throw new InputErrorException("Final closing brace ( } ) is missing.");

		// Confirm that the first entry is for time zero
		if (times.get(0) != 0.0d)
			throw new InputErrorException("First entry must be for zero simulation time.");

		// Set the value to a new time series data object
		value = new TimeSeriesData( times, values );
	}

	public void setUnitType(Class<? extends Unit> u) {
		if (u != unitType)
			this.reset();
		unitType = u;
	}

	public void setTickLength(double val) {
		tickLength = val;
	}

	public double getTickLength() {
		return tickLength;
	}

	@Override
	public boolean useExpressionBuilder() {
		return true;
	}

}
