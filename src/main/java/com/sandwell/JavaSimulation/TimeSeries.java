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
import com.jaamsim.input.OutputHandle;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class TimeSeries extends Entity implements TimeSeriesProvider {

	@Keyword(description = "A list of time series records with default format { 'yyyy MM dd HH:mm' value units }, where\n" +
					"yyyy is the year\n" +
					"MM is the month (1-12)\n" +
					"dd is the day of the month\n" +
					"HH is the hour of day (0-23)\n" +
					"mm is the minutes (0-59)\n" +
					"value is the time series value for the given date and time\n" +
					"units is the optional units for the value\n" +
					"The date and times must be given in increasing order.",
	         example = "TimeSeries1  Value { { '2010 1 1 0:00' 0.5 m } { '2010 1 1 3:00' 1.5 m } { '2010 1 1 6:00' 1.2 m } }")
	private final TimeSeriesDataInput value;

	@Keyword(description = "The unit type for the time series (e.g. DistanceUnit, TimeUnit, MassUnit).  " +
			"If the UnitType keyword is specified, it must be specified before the Value keyword.",
     example = "TimeSeries1  UnitType { DistanceUnit }")
	private final UnitTypeInput unitType;

	@Keyword(description = "Defines when the time series will repeat from the start.",
            example = "TimeSeries1  CycleTime { 8760.0 h }")
	private final ValueInput cycleTime;

	private int indexOfCurrentTime;  // The index of the time in the last call to getValueForTime()

	{
		unitType = new UnitTypeInput( "UnitType", "Key Inputs", UserSpecifiedUnit.class );
		this.addInput( unitType, true );

		value = new TimeSeriesDataInput("Value", "Key Inputs", null);
		value.setUnitType(UserSpecifiedUnit.class);
		this.addInput(value, true);

		cycleTime = new ValueInput( "CycleTime", "Key Inputs", Double.POSITIVE_INFINITY );
		cycleTime.setUnitType(TimeUnit.class);
		this.addInput( cycleTime, true );
	}

	public TimeSeries() { }

	@Override
	public void validate() {
		super.validate();

		if( unitType.getValue() == null )
			throw new InputErrorException( "UnitType must be specified first" );

		if( value.getValue() == null || value.getValue().getTimeList().size() == 0 )
			throw new InputErrorException( "Time series Value must be specified" );

		if( this.getCycleTimeInHours() < value.getValue().getTimeList().get( value.getValue().getTimeList().size() -1 ) )
			throw new InputErrorException( "CycleTime must be larger than the last time in the series" );
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		indexOfCurrentTime = 0;
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if (in == unitType) {
			value.setUnitType( unitType.getUnitType() );
			this.getOutputHandle("PresentValue").setUnitType( unitType.getUnitType() );
			return;
		}
	}

	@Override
	public OutputHandle getOutputHandle(String outputName) {
		OutputHandle out = super.getOutputHandle(outputName);
		if( out.getUnitType() == UserSpecifiedUnit.class )
			out.setUnitType( unitType.getUnitType() );
		return out;
	}

	@Output( name="PresentValue",
			 description="The time series value for the present time.",
			 unitType = UserSpecifiedUnit.class)
	@Override
	public double getNextSample(double simTime) {
		return this.getValueForTimeHours(simTime / 3600.0);
	}

	/**
	 * Return the value for the given simulation time in hours
	 */
	@Override
	public double getValueForTimeHours( double time ) {
		DoubleVector timeList = this.getTimeList();
		DoubleVector valueList = this.getValueList();

		// Update the index within the series for the current time
		indexOfCurrentTime = this.getIndexForTimeHours(getCurrentTime(), indexOfCurrentTime);

		// Determine the time in the cycle for the given time
		double timeInCycle = time;
		if (this.getCycleLength() < Double.POSITIVE_INFINITY) {
			int completedCycles = (int)Math.floor( time / this.getCycleTimeInHours() );
			timeInCycle -= completedCycles * this.getCycleTimeInHours();
		}

		// Perform linear search for time from indexOfTime
		for( int i = indexOfCurrentTime; i < timeList.size()-1; i++ ) {
			if( Tester.lessOrEqualCheckTimeStep( timeList.get( i ), timeInCycle )
					&& Tester.lessCheckTimeStep( timeInCycle, timeList.get( i+1 ) ) ) {
				return valueList.get( i );
			}
		}

		// If the time in the cycle is greater than the last time, return the last value
		if( Tester.greaterOrEqualCheckTimeStep( timeInCycle, timeList.get( timeList.size() - 1 ) ) ) {
			return valueList.get( valueList.size() - 1 );
		}

		// Perform linear search for time from 0
		for( int i = 0; i < indexOfCurrentTime; i++ ) {
			if( Tester.lessOrEqualCheckTimeStep( timeList.get( i ), timeInCycle )
					&& Tester.lessCheckTimeStep( timeInCycle, timeList.get( i+1 ) ) ) {
				return valueList.get( i );
			}
		}

		// No value was found for time, return 0
		return 0.0;
	}

	/**
	 * Return the index for the given simulation time in hours
	 */
	public int getIndexForTimeHours( double time, int startIndex ) {
		DoubleVector timeList = value.getValue().getTimeList();

		// Determine the time in the cycle for the given time
		double timeInCycle = time;
		if (this.getCycleLength() < Double.POSITIVE_INFINITY) {
			int completedCycles = (int)Math.floor( time / this.getCycleTimeInHours() );
			timeInCycle -= completedCycles * this.getCycleTimeInHours();
		}

		// Perform linear search for time from startIndex
		for( int i = startIndex; i < timeList.size()-1; i++ ) {
			if( Tester.lessOrEqualCheckTimeStep( timeList.get( i ), timeInCycle )
					&& Tester.lessCheckTimeStep( timeInCycle, timeList.get( i+1 ) ) ) {
				return i;
			}
		}

		// If the time in the cycle is greater than the last time, return the last value
		if( Tester.greaterOrEqualCheckTimeStep( timeInCycle, timeList.get( timeList.size() - 1 ) ) ) {
			return timeList.size() - 1;
		}

		// Perform linear search for time from 0
		for( int i = 0; i < startIndex; i++ ) {
			if( Tester.lessOrEqualCheckTimeStep( timeList.get( i ), timeInCycle )
					&& Tester.lessCheckTimeStep( timeInCycle, timeList.get( i+1 ) ) ) {
				return i;
			}
		}

		// No value was found for time
		this.error( "getIndexForTime( "+time+", "+startIndex+" )", "No record was found for the given time.", "" );
		return -1;
	}

	/**
	 * Return the first time that the value will be updated, after the given time.
	 */
	@Override
	public double getNextChangeTimeAfterHours( double time ) {

		// Collect parameters for the current time
		int startIndex = this.getIndexForTimeHours(time,indexOfCurrentTime)+1;
		double cycleTime = this.getCycleTimeInHours();

		// Determine how many cycles through the time series have been completed
		int completedCycles = (int)Math.floor( time / cycleTime );

		// If this is the last point in the cycle, need to cycle around to get the next point
		if( startIndex > this.getTimeList().size() - 1 ) {

			// If the series does not cycle, the value will never change
			if( cycleTime == Double.POSITIVE_INFINITY ) {
				return Double.POSITIVE_INFINITY;
			}
			else {
				double cycleOffset = 0.0;
				if( cycleTime != Double.POSITIVE_INFINITY ) {
					cycleOffset = (completedCycles+1)*cycleTime;
				}

				return this.getTimeList().get(0) + cycleOffset;
			}
		}

		// No cycling required, return the next value
		double cycleOffset = 0.0;
		if( cycleTime != Double.POSITIVE_INFINITY ) {
			cycleOffset = (completedCycles)*cycleTime;
		}
		return this.getTimeList().get(startIndex) + cycleOffset;
	}

	public DoubleVector getTimeList() {
		return value.getValue().getTimeList();
	}

	public DoubleVector getValueList() {
		return value.getValue().getValueList();
	}

	public double getCycleTimeInHours() {
		return cycleTime.getValue() / 3600;
	}

	double getCycleLength() {
		return cycleTime.getValue();
	}

	@Override
	public double getMaxTimeValueInHours() {

		if( this.getCycleTimeInHours() != Double.POSITIVE_INFINITY )
			return this.getCycleTimeInHours();

		return this.getTimeList().get( this.getTimeList().size()-1 );
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType.getUnitType();
	}

	@Override
	public double getMaxValue() {
		return value.getValue().getMaxValue();
	}

	public double getMinValue() {
		return value.getValue().getMinValue();
	}
}
