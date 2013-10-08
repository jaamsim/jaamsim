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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;
import com.sandwell.JavaSimulation3D.Clock;

public class TimeSeriesDataInput extends Input<TimeSeriesData> {
	private Class<? extends Unit> unitType;
	private SimpleDateFormat dateFormat;
	private double maxValue = Double.POSITIVE_INFINITY;
	private double minValue = Double.NEGATIVE_INFINITY;

	public TimeSeriesDataInput(String key, String cat, TimeSeriesData def) {
		super(key, cat, def);
		unitType = DimensionlessUnit.class;

		dateFormat = new SimpleDateFormat( "yyyy MM dd HH:mm" );

		// Set the time zone to GMT so calendar calculations
		// do not get disrupted by daylight savings
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	@Override
	public void parse(StringVector input) throws InputErrorException {

		if (unitType == UserSpecifiedUnit.class)
			throw new InputErrorException(INP_ERR_UNITUNSPECIFIED);

		double lastTime = -1.0;

		// Determine the starting year
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
		int startingYear = -1;

		DoubleVector times = new DoubleVector(input.size()/4);
		DoubleVector values = new DoubleVector(input.size()/4);

		// Determine records in the time series
		// Records have form: (e.g.) yyyy-MM-dd HH:mm value units
		// where units are optional
		StringVector each = new StringVector();
		for (int i=0; i < input.size(); i++) {

			//skip over opening brace if present
			if (input.get(i).equals("{") )
				continue;

			each.clear();

			//iterate until closing brace, or end of entry
			for (int j = i; j < input.size(); j++, i++){
				if (input.get(j).equals("}"))
					break;

				each.add(input.get(j));
			}

			// each now contains a time series record
			// Is this the first time series record?
			if( startingYear == -1 ) {
				try {
					Date startingDate = dateFormat.parse( each.get( 0 ) );
					calendar.setTime( startingDate );
					startingYear = calendar.get(Calendar.YEAR);
				}
				catch ( ParseException e ) {
					throw new InputErrorException("Invalid date " + each.get( 0 ) );
				}
			}

			// Check the number of entries in the record
			if (unitType == DimensionlessUnit.class)
				Input.assertCount(each, 2);
			else
				Input.assertCount(each, 3);

			// Parse the date and time from the record
			Date date;
			try {
				date = dateFormat.parse( each.get( 0 ) );
				calendar.setTime( date );
			}
			catch ( ParseException e ) {
				throw new InputErrorException( "Invalid date " + each.get( 0 ) );
			}
			int year = calendar.get(Calendar.YEAR);
			int month = calendar.get(Calendar.MONTH);
			int day = calendar.get(Calendar.DAY_OF_MONTH);
			int hour = calendar.get(Calendar.HOUR_OF_DAY);
			int minute = calendar.get(Calendar.MINUTE);
			int second = calendar.get(Calendar.SECOND);
			double decHour = hour + (minute/60.0) + (second/3600.0);

			// Determine the simulation time for the date and time (assuming no leap years)
			double t = Clock.calcTimeForYear_Month_Day_Hour( year - startingYear + 1, month + 1, day, decHour );

			// Make sure the times are in increasing order
			if( t > lastTime ) {
				times.add( t );
				lastTime = t;
			}
			else {
				throw new InputErrorException( "The times must be given in increasing order on " + each.get(0) );
			}

			each.remove(0);  // We've finished parsing the date at this point
			DoubleVector v = Input.parseDoubles(each, minValue, maxValue, unitType);
			values.add(v.get(0));
		}

		// Set the value to a new time series data object
		value = new TimeSeriesData( times, values );
	}

	public void setUnitType(Class<? extends Unit> u) {
		unitType = u;
	}

	public void setDateFormat( String str ) {
		dateFormat.applyPattern( str );
	}
}
