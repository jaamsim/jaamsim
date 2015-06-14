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
	         exampleList = {"{ 0 h 0.5 m } { 3 h 1.5 m } { 6 h 1.2 m }",
							"{ '2010-01-01 00:00:00' 0.5 m } { '2010-01-01 03:00:00' 1.5 m } { '2010-01-01 06:00:00' 1.2 m }"})
	private final TimeSeriesDataInput value;

	@Keyword(description = "The unit type for the time series (e.g. DistanceUnit, TimeUnit, MassUnit).  " +
			"If the UnitType keyword is specified, it must be specified before the Value keyword.",
	         exampleList = {"DistanceUnit"})
	private final UnitTypeInput unitType;

	@Keyword(description = "Defines when the time series will repeat from the start.",
	         exampleList = {"8760.0 h"})
	private final ValueInput cycleTime;

	{
		unitType = new UnitTypeInput( "UnitType", "Key Inputs", UserSpecifiedUnit.class );
		unitType.setRequired(true);
		this.addInput( unitType );

		value = new TimeSeriesDataInput("Value", "Key Inputs", null);
		value.setUnitType(UserSpecifiedUnit.class);
		value.setRequired(true);
		this.addInput(value);

		cycleTime = new ValueInput( "CycleTime", "Key Inputs", Double.POSITIVE_INFINITY );
		cycleTime.setUnitType(TimeUnit.class);
		this.addInput( cycleTime );
	}

	public TimeSeries() { }

	@Override
	public void validate() {
		super.validate();

		if (value.getTickLength() != Simulation.getTickLength())
			throw new InputErrorException("A new value was entered for the Simulation keyword TickLength " +
					"after the TimeSeries data had been loaded.%n" +
					"The configuration file must be saved and reloaded before the simulation can be executed.");

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
		return getValue(getTSPointForTicks(ticks));
	}

	/**
	 * Return the first time that the value will be updated, after the given
	 * simulation time.
	 * @param ticks - simulation time in clock ticks.
	 * @return simulation time in clock ticks at which the time series value will change.
	 */
	@Override
	public long getNextChangeAfterTicks(long ticks) {
		return getTicks(getTSPointAfter(getTSPointForTicks(ticks)));
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

	/**
	 * Returns the position in the time series that corresponds to the specified
	 * time in simulation clock ticks.
	 * <p>
	 * The position returned is the largest one whose ticks value is less than
	 * or equal to the specified ticks.
	 * @param ticks - simulation time in clock ticks.
	 * @return position in the TimeSeries.
	 */
	private TSPoint getTSPointForTicks(long ticks) {

		long[] ticksList = value.getValue().ticksList;
		if (ticks == Long.MAX_VALUE) {
			if (cycleTime.getValue() == Double.POSITIVE_INFINITY)
				return new TSPoint(ticksList.length - 1, 0);
			return new TSPoint(ticksList.length - 1, Long.MAX_VALUE);
		}

		// Find the time within the present cycle
		final long cycleTicks = getTicks(cycleTime.getValue());
		long numberOfCycles = ticks / cycleTicks;
		long ticksInCycle = ticks % cycleTicks;

		// If the time in the cycle is greater than the last time, return the last value
		if (ticksInCycle >= ticksList[ticksList.length - 1]) {
			return new TSPoint(ticksList.length - 1, numberOfCycles);
		}

		// Find the index by binary search
		int k = Arrays.binarySearch(ticksList, ticksInCycle);

		// If the returned index is greater or equal to zero,
		// then an exact match was found
		if (k >= 0)
			return new TSPoint(k, numberOfCycles);

		if (k == -1)
			error("No value found at time: %f", getSimTime(ticks));

		// If the returned index is negative, then (insertion index) = -k-1
		// Return the index before the insertion index
		return new TSPoint(-k - 2, numberOfCycles);
	}

	/**
	 * Returns the position in the time series that corresponds to the specified value.
	 * <p>
	 * The TimeSeries values must increase monotonically. The position returned
	 * is the largest one whose value is less than or equal to the specified value.
	 * @param val - specified value.
	 * @return position in the TimeSeries.
	 */
	private TSPoint getTSPointForValue(double val) {

		double[] valueList = value.getValue().valueList;
		if (val > getMaxValue() && cycleTime.getValue() == Double.POSITIVE_INFINITY)
			return new TSPoint(valueList.length - 1, 0);

		// Find the value within the present cycle
		double valInCycle = val % getMaxValue();
		long numberOfCycles = Math.round((val - valInCycle) / getMaxValue());

		// If the value in the cycle is greater than or equal to the last value, return the last index
		if (valInCycle >= valueList[valueList.length - 1])
			return new TSPoint(valueList.length - 1, numberOfCycles);

		// Find the index by binary search
		int k = Arrays.binarySearch(valueList, valInCycle);

		// If the returned index is greater or equal to zero,
		// then an exact match was found
		if (k >= 0)
			return new TSPoint(k, numberOfCycles);

		if (k == -1)
			error("No entry found for value: %f", val);

		// If the returned index is negative, then (insertion index) = -k-1
		// Return the index before the insertion index
		return new TSPoint(-k - 2, numberOfCycles);
	}

	/**
	 * Returns the simulation time in clock ticks for the specified position in
	 * the time series.
	 * @param pt - position in the time series.
	 * @return simulation time in clock ticks.
	 */
	private long getTicks(TSPoint pt) {
		if (pt.index == -1)
			return Long.MAX_VALUE;
		if (cycleTime.getValue() == Double.POSITIVE_INFINITY)
			return value.getValue().ticksList[pt.index];
		return value.getValue().ticksList[pt.index] + pt.numberOfCycles*getTicks(cycleTime.getValue());
	}

	/**
	 * Returns the time series value for the specified position in the time
	 * series.
	 * @param pt - position in the time series.
	 * @return value for the time series.
	 */
	private double getValue(TSPoint pt) {
		double valueList[] = value.getValue().valueList;
		if (pt.index == -1)
			return valueList[ valueList.length - 1 ];
		return valueList[pt.index];
	}

	/**
	 * Returns the total value for the time series at the specified position.
	 * <p>
	 * If a cycle time has been specified, then the total time increases
	 * with each pass through the time series.
	 * @param pt - position in the time series.
	 * @return total value for the time series.
	 */
	private double getCumulativeValue(TSPoint pt) {
		if (cycleTime.getValue() == Double.POSITIVE_INFINITY)
			return getValue(pt);
		return getValue(pt) + pt.numberOfCycles*getMaxValue();
	}

	/**
	 * Returns the position in the time series that follows the specified
	 * position.
	 * <p>
	 * An index of -1 is returned if the specified position is a the end
	 * of the time series data and a cycle time is not specified.
	 * @param pt - specified position in the time series.
	 * @return next position in the time series.
	 */
	private TSPoint getTSPointAfter(TSPoint pt) {
		if (pt.index == -1)
			return new TSPoint(pt.index, pt.numberOfCycles);

		if (pt.index == value.getValue().ticksList.length - 1) {
			if (cycleTime.getValue() == Double.POSITIVE_INFINITY)
				return new TSPoint(-1, pt.numberOfCycles);

			return new TSPoint(0, pt.numberOfCycles + 1);
		}

		return new TSPoint(pt.index + 1, pt.numberOfCycles);
	}

	@Override
	public long getInterpolatedTicksForValue(double val) {

		TSPoint low = getTSPointForValue(val);
		TSPoint high = getTSPointAfter(low);
		if (high.index == -1)
			return Long.MAX_VALUE;

		long ticksLow = getTicks(low);
		long ticksHigh = getTicks(high);
		double valueLow = getCumulativeValue(low);
		double valueHigh = getCumulativeValue(high);

		// The value at the end of the cycle is equal to the value at the start of the next cycle
		if (valueHigh == valueLow) {
			high = getTSPointAfter(high);
			ticksHigh = getTicks(high);
			valueHigh = getCumulativeValue(high);
		}

		return ticksLow + Math.round((val - valueLow)*(ticksHigh - ticksLow)/(valueHigh - valueLow));
	}

	@Override
	public double getInterpolatedCumulativeValueForTicks(long ticks) {

		TSPoint low = getTSPointForTicks(ticks);
		TSPoint high = getTSPointAfter(low);
		if (high.index == -1) {
			double valueList[] = value.getValue().valueList;
			return valueList[ valueList.length - 1 ];
		}

		long ticksLow = getTicks(low);
		long ticksHigh = getTicks(high);
		double valueLow = getCumulativeValue(low);
		double valueHigh = getCumulativeValue(high);

		// The value at the end of the cycle is equal to the value at the start of the next cycle
		if (valueHigh == valueLow) {
			high = getTSPointAfter(high);
			ticksHigh = getTicks(high);
			valueHigh = getCumulativeValue(high);
		}

		return valueLow + (ticks - ticksLow)*(valueHigh - valueLow)/(ticksHigh - ticksLow);
	}

	// ******************************************************************************************************
	// TIME SERIES POINT
	// ******************************************************************************************************
	private class TSPoint {
		private int index;            // index number for the time series point
		private long numberOfCycles;  // number of passes through the time series data

		private TSPoint(int ind, long n) {
			index = ind;
			numberOfCycles = n;
		}
	}

	// ******************************************************************************************************
	// OUTPUTS
	// ******************************************************************************************************

	@Output(name = "PresentValue",
	        description = "The time series value for the present time.",
	        unitType = UserSpecifiedUnit.class)
	@Override
	public final double getNextSample(double simTime) {
		return getValue(getTSPointForTicks(getTicks(simTime)));
	}

}
