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

import java.util.ArrayList;

import com.jaamsim.input.InputAgent;
import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation3D.Clock;

public class TimeSeriesInput extends Input<TimeSeriesData> {

	private int[] validCounts;
	private Class<? extends Entity> unitType;
	private double maxValue = Double.POSITIVE_INFINITY;
	private double minValue = Double.NEGATIVE_INFINITY;

	public TimeSeriesInput(String key, String cat, TimeSeriesData def) {
		super(key, cat, def);
		validCounts = new int[] { 5, 6 };
		unitType = null;
	}

	@Override
	public void parse(StringVector input) throws InputErrorException {
		DoubleVector times = new DoubleVector();
		DoubleVector values = new DoubleVector();

		double lastTime = -1.0;

		// Determine records in the time series
		// Records have form: yyyy MM dd hour value units
		// where hour is in decimal hours and units are optional
		ArrayList<StringVector> temp = Util.splitStringVectorByBraces( input );

		// Determine the starting year
		int startingYear = 0;
		if( temp.size() > 0 )
			startingYear = Input.parseInteger( temp.get( 0 ).get( 0 ) );

		// Loop through records in the time series
		for (StringVector each : temp) {

			// Check the number of entries in the record
			Input.assertCount( each, validCounts );

			// Parse the date and time from the record
			int year = Input.parseInteger( each.get( 0 ) );
			int month = Input.parseInteger( each.get( 1 ), 1, 12 );
			int day = Input.parseInteger( each.get( 2 ) );
			double hour = Input.parseDouble( each.get( 3 ), 0.0, 24.0 );

			// Determine the simulation time for the date and time
			double t = Clock.calcTimeForYear_Month_Day_Hour( year - startingYear + 1, month, day, hour );

			// Make sure the times are in increasing order
			if( t > lastTime ) {
				times.add( t );
				lastTime = t;
			}
			else {
				throw new InputErrorException( "The times must be given in increasing order" );
			}

			// If there are more than five values, and the last one is not a number, then assume it is a unit
			if( each.size() > 5 && !Tester.isDouble( each.get( each.size()-1 ) ) ) {

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

				if( unitType != null )
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
}
