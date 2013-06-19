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

import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.units.Unit;

public class TimeSeries extends Entity {

	@Keyword(description = "A list of time series records for the form { yyyy MM dd hour value units }, " +
					"where hour is in decimal hours, value is the time series value for the given date " +
					"and time, and units is optional.  The dates must be given in increasing order.",
	         example = "TimeSeries1  Value { { 2010 1 1 0.0 0.5 m } { 2010 1 1 3.0 1.5 m } { 2010 1 1 6.0 1.2 m } }")
	private final TimeSeriesInput value;

	@Keyword(description = "The unit type for the time series (e.g. DistanceUnit, TimeUnit, MassUnit).  " +
			"If the UnitType keyword is specified, it must be specified before the Values keyword.",
     example = "TimeSeries1  UnitType { DistanceUnit }")
	private final UnitTypeInput unitType;

	@Keyword(description = "The format for the date and time (e.g. 'yyyy-MM-dd HH:mm:ss', yyyy/MM/dd).  " +
	                "Put single quotes around the format if it includes spaces.",
     example = "TimeSeries1  DateFormat { 'yyyy-MM-dd HH:mm' }")
	private final StringInput dateFormat;

	private int indexOfTime;  // The index of the time in the last call to getPresentValue()
	private double cycleTime; // The number of hours in the cycle for the time series

	{
		value = new TimeSeriesInput("Value", "Key Inputs", null);
		this.addInput(value, true);

		unitType = new UnitTypeInput( "UnitType", "Optional" );
		this.addInput( unitType, true );

		dateFormat = new StringInput("DateFormat", "Key Inputs", null);
		this.addInput(dateFormat, true);
	}

	public TimeSeries() { }

	@Override
	public void validate() {
		super.validate();

		if( value.getValue() == null || value.getValue().getTimeList().size() == 0 )
			throw new InputErrorException( "Time series Value must be specified" );
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		indexOfTime = 0;

		// Determine the cycle time as the last time rounded up to the nearest year
		DoubleVector timeList = value.getValue().getTimeList();
		cycleTime = Math.ceil( timeList.get( timeList.size() - 1 ) / 8760.0 ) * 8760.0;
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if ( in == unitType ) {
			if( value.getValue() != null )
				throw new InputErrorException( "UnitType must be specified before Value");

			value.setUnitType( unitType.getUnitType() );
		}
		if ( in == dateFormat ) {
			try {
				value.setDateFormat( dateFormat.getValue() );
			}
			catch ( IllegalArgumentException e ) {
				throw new InputErrorException( "Invalid date format " + dateFormat.getValue() );
			}
		}
	}

	@Output( name="PresentValue",
			 description="The time series value for the present time." )
	public double getPresentValue( double simTime ) {
		DoubleVector timeList = value.getValue().getTimeList();
		DoubleVector valueList = value.getValue().getValueList();

		// Determine the time in the cycle for the current simulation time
		int completedCycles = (int)Math.floor( getCurrentTime() / cycleTime );
		double timeInCycle = getCurrentTime() - ( completedCycles * cycleTime );

		// Perform linear search for the present time from indexOfTime
		for( int i = indexOfTime; i < timeList.size()-1; i++ ) {
			if( Tester.lessOrEqualCheckTolerance( timeList.get( i ), timeInCycle )
					&& Tester.lessCheckTolerance( timeInCycle, timeList.get( i+1 ) ) ) {
				indexOfTime = i;
				return valueList.get( indexOfTime );
			}
		}

		// If the time in the cycle is greater than the last time, return the last value
		if( Tester.greaterOrEqualCheckTolerance( timeInCycle, timeList.get( timeList.size() - 1 ) ) ) {
			indexOfTime = timeList.size() - 1;
			return valueList.get( indexOfTime );
		}

		// Perform linear search for the present time from 0
		for( int i = 0; i < indexOfTime; i++ ) {
			if( Tester.lessOrEqualCheckTolerance( timeList.get( i ), timeInCycle )
					&& Tester.lessCheckTolerance( timeInCycle, timeList.get( i+1 ) ) ) {
				indexOfTime = i;
				return valueList.get( indexOfTime );
			}
		}

		// No value was found for the present time, return 0
		return 0.0;
	}

	public Class<? extends Unit> getUnitType() {
		return unitType.getUnitType();
	}
}
