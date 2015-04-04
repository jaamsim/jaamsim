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
package com.jaamsim.Samples;

import java.util.Arrays;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.TimeSeriesDataInput;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

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

		if (value.getTickLength() != Simulation.getTickLength())
			throw new InputErrorException("A new value was entered for the Simulation keyword TickLength " +
					"after the TimeSeries data had been loaded.%n" +
					"The configuration file must be saved and reloaded before the simulation can be executed.");

		if( value.getValue() == null || value.getValue().ticksList.length == 0 )
			throw new InputErrorException( "Time series Value must be specified" );

		long[] ticksList = value.getValue().ticksList;
		if (getTicks(cycleTime.getValue()) < ticksList[ticksList.length - 1])
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
	public Class<? extends Unit> getUserUnitType() {
		return unitType.getUnitType();
	}

	private long getTicks(double simTime) {
		return FrameBox.secondsToTicks(simTime);
	}

	private double getSimTime(long ticks) {
		if (ticks == Long.MAX_VALUE)
			return Double.POSITIVE_INFINITY;
		return FrameBox.ticksToSeconds(ticks);
	}

	/**
	 * Returns the value for time series at the given simulation time.
	 * @param ticks - simulation time in clock ticks.
	 */
	@Override
	public double getValueForTicks(long ticks) {
		return value.getValue().valueList[ getIndexForTicks(ticks) ];
	}

	/**
	 * Returns the index in the time series for the given simulation time.
	 * @param ticks - simulation time in clock ticks.
	 * @return index in the times series for the given simulation time.
	 */
	private int getIndexForTicks(long ticks) {

		long[] ticksList = value.getValue().ticksList;
		if (ticks == Long.MAX_VALUE)
			return ticksList.length - 1;

		// Find the time within the present cycle
		long ticksInCycle = ticks % getTicks(cycleTime.getValue());

		// If the time in the cycle is greater than the last time, return the last value
		if (ticksInCycle >= ticksList[ticksList.length - 1]) {
			return ticksList.length - 1;
		}

		// Find the index by binary search
		int index = Arrays.binarySearch(ticksList, ticksInCycle);

		// If the returned index is greater or equal to zero,
		// then an exact match was found
		if (index >= 0)
			return index;

		if (index == -1)
			error("No value found at time: %f", getSimTime(ticks));

		// If the returned index is negative, then (insertion index) = -index-1
		// Return the index before the insertion index
		return -index - 2;
	}

	/**
	 * Return the first time that the value will be updated, after the given
	 * simulation time.
	 * @param ticks - simulation time in clock ticks.
	 * @return simulation time in clock ticks at which the time series value will change.
	 */
	@Override
	public long getNextChangeAfterTicks(long ticks) {

		int index = this.getIndexForTicks(ticks);
		long[] ticksList = value.getValue().ticksList;
		long cycleTimeInTicks = getTicks(cycleTime.getValue());
		long startOfCycle = ticks - (ticks % cycleTimeInTicks);

		// If the last entry in the list, then the next change is the end of the cycle
		if (index == ticksList.length-1) {
			if (cycleTimeInTicks == Long.MAX_VALUE)
				return Long.MAX_VALUE;
			return startOfCycle + cycleTimeInTicks;
		}

		return startOfCycle + ticksList[index+1];
	}

	@Output(name = "PresentValue",
	        description = "The time series value for the present time.",
	        unitType = UserSpecifiedUnit.class)
	@Override
	public final double getNextSample(double simTime) {
		return value.getValue().valueList[ getIndexForTicks(getTicks(simTime)) ];
	}

	@Override
	public double getNextTimeAfter(double simTime) {
		return getSimTime( getNextChangeAfterTicks(getTicks(simTime)) );
	}

	@Override
	public long getMaxTicksValue() {
		if (cycleTime.getValue() < Double.POSITIVE_INFINITY)
			return getTicks(cycleTime.getValue());

		long[] ticksList = value.getValue().ticksList;
		return ticksList[ ticksList.length-1 ];
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
