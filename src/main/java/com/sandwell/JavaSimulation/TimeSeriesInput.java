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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.jaamsim.input.InputAgent;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;
import com.sandwell.JavaSimulation3D.Clock;

public class TimeSeriesInput extends Input<TimeSeriesData> {

	private int[] validCounts;
	private Class<? extends Entity> unitType;
	private SimpleDateFormat dateFormat;
	private double maxValue = Double.POSITIVE_INFINITY;
	private double minValue = Double.NEGATIVE_INFINITY;

	public TimeSeriesInput(String key, String cat, TimeSeriesData def) {
		super(key, cat, def);
		validCounts = new int[] { 2, 3 };
		unitType = null;
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

		// Determine records in the time series
		// Records have form: (e.g.) yyyy-MM-dd HH:mm value units
		// where units are optional
		ArrayList<StringVector> temp = Util.splitStringVectorByBraces( input );

		// Determine the starting year
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
		int startingYear = 0;
		if( temp.size() > 0 ) {

			try {
				Date startingDate = dateFormat.parse( temp.get( 0 ).get( 0 ) );
				calendar.setTime( startingDate );
				startingYear = calendar.get(Calendar.YEAR);
			}
			catch ( ParseException e ) {
				throw new InputErrorException("Invalid date " + temp.get( 0 ).get( 0 ) );
			}
		}

		DoubleVector times = new DoubleVector(temp.size());
		DoubleVector values = new DoubleVector(temp.size());
		// Loop through records in the time series
		for (StringVector each : temp) {

			// Check the number of entries in the record
			Input.assertCount( each, validCounts );

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

			// If there are more than two values, and the last one is not a number, then assume it is a unit
			if( each.size() > 2 && !Tester.isDouble( each.get( each.size()-1 ) ) ) {

				// Check that a unit type was specified
				if( unitType == null )
					throw new InputErrorException( "UnitType was not specified" );

				// Determine the units
				Unit unit = Input.parseUnits( each.get( each.size()- 1 ) );

				// Check that the unit is of the stored unit type
				if( unit.getClass() != unitType )
					throw new InputErrorException( unit + " is not a " + unitType.getSimpleName() );

				// Determine the conversion factor to SI
				double conversionFactor = unit.getConversionFactorToSI();

				// Parse the value from the record
				values.add( Input.parseDouble( each.get( each.size()-2 ), minValue, maxValue, conversionFactor) );
			}
			else {
				values.add( Input.parseDouble( each.get( each.size()-1 ), minValue, maxValue ) );

				if( unitType != DimensionlessUnit.class )
					InputAgent.logWarning( "Missing units.  Assuming SI." );
			}
		}

		// Set the value to a new time series data object
		value = new TimeSeriesData( times, values );

		this.updateEditingFlags();
	}

	public void setUnitType( Class<? extends Entity> u ) {
		unitType = u;
	}

	public void setDateFormat( String str ) {
		dateFormat.applyPattern( str );
	}
}
