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

import java.util.Arrays;

import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.input.TimeSeriesDataInput;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class TimeSeries extends DisplayEntity implements TimeSeriesProvider {

	@Keyword(description = "A list of time series records with format { 'YYYY-MM-DD hh:mm:ss' value units }, where\n" +
					"YYYY is the year\n" +
					"MM is the month (01-12)\n" +
					"DD is the day of the month\n" +
					"hh is the hour of day (00-23)\n" +
					"mm is the minutes (00-59)\n" +
					"ss is the seconds (00-59)\n" +
					"value is the time series value for the given date and time\n" +
					"units is the optional units for the value\n" +
					"The date and times must be given in increasing order.",
	         example = "TimeSeries1  Value { { '2010-01-01 00:00:00' 0.5 m } { '2010-01-01 03:00:00' 1.5 m } { '2010-01-01 06:00:00' 1.2 m } }")
	private final TimeSeriesDataInput value;

	@Keyword(description = "The unit type for the time series (e.g. DistanceUnit, TimeUnit, MassUnit).  " +
			"If the UnitType keyword is specified, it must be specified before the Value keyword.",
     example = "TimeSeries1  UnitType { DistanceUnit }")
	private final UnitTypeInput unitType;

	@Keyword(description = "Defines when the time series will repeat from the start.",
            example = "TimeSeries1  CycleTime { 8760.0 h }")
	private final ValueInput cycleTime;

	{
		unitType = new UnitTypeInput( "UnitType", "Key Inputs", UserSpecifiedUnit.class );
		this.addInput( unitType );

		value = new TimeSeriesDataInput("Value", "Key Inputs", null);
		value.setUnitType(UserSpecifiedUnit.class);
		this.addInput(value);

		cycleTime = new ValueInput( "CycleTime", "Key Inputs", Double.POSITIVE_INFINITY );
		cycleTime.setUnitType(TimeUnit.class);
		this.addInput( cycleTime );
	}

	public TimeSeries() { }

	@Override
	public void validate() {
		super.validate();

		if( unitType.getValue() == null )
			throw new InputErrorException( "UnitType must be specified first" );

		if( value.getValue() == null || value.getValue().timeList.length == 0 )
			throw new InputErrorException( "Time series Value must be specified" );

		double[] tList = value.getValue().timeList;
		if (this.getCycleTimeInHours() < tList[tList.length - 1])
			throw new InputErrorException( "CycleTime must be larger than the last time in the series" );
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

	@Output(name = "PresentValue",
	        description = "The time series value for the present time.",
	        unitType = UserSpecifiedUnit.class)
	@Override
	public final double getNextSample(double simTime) {
		return this.getValueForTimeHours(simTime / 3600.0);
	}

	/**
	 * Return the value for the given simulation time in hours
	 */
	private double getValueForTimeHours( double time ) {
		double[] valueList = value.getValue().valueList;
		return valueList[ getIndexForTimeHours( time ) ];
	}

	/**
	 * Return the index for the given simulation time in hours
	 */
	public int getIndexForTimeHours( double time ) {
		double[] timeList = value.getValue().timeList;

		// Determine the time in the cycle for the given time
		double timeInCycle = time;
		if (this.getCycleLength() < Double.POSITIVE_INFINITY) {
			int completedCycles = (int)Math.floor( time / this.getCycleTimeInHours() );
			timeInCycle -= completedCycles * this.getCycleTimeInHours();
			if( Tester.equalCheckTolerance(timeInCycle, this.getCycleTimeInHours()) ) {
				timeInCycle = 0;
			}
		}

		// If the time in the cycle is greater than the last time, return the last value
		if( Tester.greaterOrEqualCheckTimeStep( timeInCycle, timeList[ timeList.length - 1 ] ) ) {
			return timeList.length - 1;
		}
		else {
			// Otherwise, find the index with a binary search
			int index = Arrays.binarySearch(timeList, timeInCycle);

			// If the returned index is greater or equal to zero,
			// then an exact match was found
			if( index >= 0 ) {
				return index;
			}
			else {
				// If the returned index is negative,
				// then index = -(insertion index)-1
				// or (insertion index) = -(index+1) = -index-1
				// If the time at the insertion index is within one tick,
				// then return it
				if( Tester.equalCheckTimeStep( timeInCycle, timeList[-index - 1] ) )
					return -index - 1;
				else
					// Otherwise, return the index before the insertion index
					if( index == -1 )
						throw new ErrorException( this + " does not have a value at time " + time );
					else
						return -index - 2;
			}
		}
	}

	@Override
	public double getNextTimeAfter(double simTime) {
		return getNextChangeTimeAfterHours(simTime / 3600.0d) * 3600.0d;
	}

	/**
	 * Return the first time that the value will be updated, after the given time.
	 */
	@Override
	public double getNextChangeTimeAfterHours( double time ) {

		// Collect parameters for the current time
		int startIndex = this.getIndexForTimeHours(time)+1;
		double cycleTime = this.getCycleTimeInHours();

		// Determine how many cycles through the time series have been completed
		int completedCycles = (int)Math.floor( time / cycleTime );

		// Tolerance check for essentially through a cycle
		double timeInCycle = time - (completedCycles * this.getCycleTimeInHours());
		if( Tester.equalCheckTolerance(timeInCycle, this.getCycleTimeInHours()) ) {
			completedCycles++;
		}

		double[] timeList = value.getValue().timeList;
		// If this is the last point in the cycle, need to cycle around to get the next point
		if( startIndex > timeList.length - 1 ) {

			// If the series does not cycle, the value will never change
			if( cycleTime == Double.POSITIVE_INFINITY ) {
				return Double.POSITIVE_INFINITY;
			}
			else {
				double cycleOffset = 0.0;
				if( cycleTime != Double.POSITIVE_INFINITY ) {
					cycleOffset = (completedCycles+1)*cycleTime;
				}

				return timeList[0] + cycleOffset;
			}
		}

		// No cycling required, return the next value
		double cycleOffset = 0.0;
		if( cycleTime != Double.POSITIVE_INFINITY ) {
			cycleOffset = (completedCycles)*cycleTime;
		}
		return timeList[startIndex] + cycleOffset;
	}

	public double getCycleTimeInHours() {
		return cycleTime.getValue() / 3600;
	}

	double getCycleLength() {
		return cycleTime.getValue();
	}

	@Override
	public double getMaxTimeValue() {
		if (this.getCycleLength() < Double.POSITIVE_INFINITY)
			return this.getCycleLength();

		double[] tList = value.getValue().timeList;
		return tList[ tList.length-1 ] * 3600.0d;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType.getUnitType();
	}

	@Override
	public double getMaxValue() {
		return value.getValue().getMaxValue();
	}

	@Override
	public double getMinValue() {
		return value.getValue().getMinValue();
	}

	@Override
	public double getMeanValue(double simTime) {
		return this.getNextSample(simTime);
	}
}
