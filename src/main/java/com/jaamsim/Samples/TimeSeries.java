/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2023 JaamSim Software Inc.
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
package com.jaamsim.Samples;

import java.util.ArrayList;
import java.util.Arrays;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.basicsim.ObserverEntity;
import com.jaamsim.basicsim.SubjectEntity;
import com.jaamsim.basicsim.SubjectEntityDelegate;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.TimeSeriesDataInput;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class TimeSeries extends DisplayEntity implements TimeSeriesProvider, SubjectEntity {

	@Keyword(description = "If TRUE, the simulation times corresponding to the time stamps "
	                     + "entered to the 'Value' keyword are calculated relative to the "
	                     + "first time stamp. This offset sets the simulation time for the first "
	                     + "time stamp to zero seconds.",
	         exampleList = {"TRUE"})
	private final BooleanInput offsetToFirst;

	@Keyword(description = "The unit type for the time series. The UnitType input must be "
	                     + "specified before the Value input.",
	         exampleList = {"DistanceUnit", "MassUnit", "DimensionlessUnit"})
	private final UnitTypeInput unitType;

	@Keyword(description = "A list of time series records with format { time value }, where: "
	                     + "'time' is the time stamp for the record and 'value' is the time "
	                     + "series value. Records are entered in order of increasing simulation "
	                     + "time. The appropriate units should be included with both the time "
	                     + "and value inputs.",
	         exampleList = {"{ 0 h 1 } { 3 h 0 }",
	                        "{ 0 h 0.5 m } { 3 h 1.5 m }",
	                        "{ '2010-01-01 00:00:00' 0.5 m } { '2010-01-01 03:00:00' 1.5 m }"} )
	private final TimeSeriesDataInput value;

	@Keyword(description = "The time at which the time series will repeat from the start.",
	         exampleList = {"8760.0 h"})
	private final ValueInput cycleTime;

	private final SubjectEntityDelegate subject = new SubjectEntityDelegate(this);

	{
		offsetToFirst = new BooleanInput("OffsetToFirst", KEY_INPUTS, true);
		this.addInput(offsetToFirst);

		unitType = new UnitTypeInput("UnitType", KEY_INPUTS, UserSpecifiedUnit.class);
		unitType.setRequired(true);
		unitType.setCallback(inputCallback);
		this.addInput(unitType);

		value = new TimeSeriesDataInput("Value", KEY_INPUTS, null);
		value.setTickLength(getSimulation().getTickLength());
		value.setUnitType(UserSpecifiedUnit.class);
		value.setRequired(true);
		this.addInput(value);

		cycleTime = new ValueInput("CycleTime", KEY_INPUTS, Double.POSITIVE_INFINITY);
		cycleTime.setUnitType(TimeUnit.class);
		this.addInput(cycleTime);
	}

	public TimeSeries() { }

	@Override
	public void earlyInit() {
		super.earlyInit();
		subject.clear();
	}

	@Override
	public void validate() {
		super.validate();

		if (value.getTickLength() != getSimulation().getTickLength())
			throw new InputErrorException("A new value was entered for the Simulation keyword TickLength " +
					"after the TimeSeries data had been loaded.%n" +
					"The configuration file must be saved and reloaded before the simulation can be executed.");

		long[] ticksList = value.getValue().ticksList;
		if (getCycleTicks() < ticksList[ticksList.length - 1] - ticksList[0])
			throw new InputErrorException( "CycleTime must be larger than the difference between "
					+ "the first and last times in the series." );
	}

	static final InputCallback inputCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((TimeSeries)ent).updateInputValue();
		}
	};

	void updateInputValue() {
		value.setUnitType( unitType.getUnitType() );
	}

	@Override
	public void startUp() {
		super.startUp();
		this.waitForNextValue();
	}

	@Override
	public void registerObserver(ObserverEntity obs) {
		subject.registerObserver(obs);
	}

	@Override
	public void notifyObservers() {
		subject.notifyObservers();
	}

	@Override
	public ArrayList<ObserverEntity> getObserverList() {
		return subject.getObserverList();
	}

	public boolean isOffsetToFirst() {
		return offsetToFirst.getValue();
	}

	public long getCycleTicks() {
		return getTicks( cycleTime.getValue() );
	}

	/**
	 * Schedules an event when the TimeSeries' value changes.
	 */
	final void waitForNextValue() {
		long ticks = this.getSimTicks();
		long durTicks = getNextChangeAfterTicks(ticks) - ticks;
		if (isTraceFlag())
			trace(0, "waitForNextValue - dur=%.6f", EventManager.current().ticksToSeconds(durTicks));
		if (durTicks == 0L)
			return;
		this.scheduleProcessTicks(durTicks, 0, waitForNextValueTarget);

		// Notify any observers
		notifyObservers();
	}

	/**
	 * WaitForNextValueTarget
	 */
	private static class WaitForNextValueTarget extends EntityTarget<TimeSeries> {
		WaitForNextValueTarget(TimeSeries ent) {
			super(ent, "endStep");
		}

		@Override
		public void process() {
			ent.waitForNextValue();
		}
	}
	private final ProcessTarget waitForNextValueTarget = new WaitForNextValueTarget(this);

	@Override
	public Class<? extends Unit> getUserUnitType() {
		return unitType.getUnitType();
	}

	private long getTicks(double simTime) {
		EventManager evt = this.getJaamSimModel().getEventManager();
		return evt.secondsToNearestTick(simTime);
	}

	private double getSimTime(long ticks) {
		if (ticks == Long.MAX_VALUE)
			return Double.POSITIVE_INFINITY;
		EventManager evt = this.getJaamSimModel().getEventManager();
		return evt.ticksToSeconds(ticks);
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
	 * Return the last time that the value updated, before the given
	 * simulation time.
	 * @param ticks - simulation time in clock ticks.
	 * @return simulation time in clock ticks at which the time series value changed.
	 */
	@Override
	public long getLastChangeBeforeTicks(long ticks) {
		return getTicks(getTSPointBefore(getTSPointForTicks(ticks)));
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
			return getCycleTicks();

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

	public boolean isMonotonic(int dir) {
		return value.getValue().isMonotonic(dir);
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
		long cycleTicks = getCycleTicks();
		long numberOfCycles = (ticks - ticksList[0]) / cycleTicks;
		long ticksInCycle = (ticks - ticksList[0]) % cycleTicks + ticksList[0];

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
		return value.getValue().ticksList[pt.index] + pt.numberOfCycles*getCycleTicks();
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
	 * Returns the position in the time series that precedes the specified
	 * position.
	 * <p>
	 * An index of -1 is returned if the specified position is a the start
	 * of the time series data and a cycle time is not specified.
	 * @param pt - specified position in the time series.
	 * @return previous position in the time series.
	 */
	private TSPoint getTSPointBefore(TSPoint pt) {
		if (pt.index == -1)
			return new TSPoint(pt.index, pt.numberOfCycles);

		if (pt.index == 0) {
			if (cycleTime.getValue() == Double.POSITIVE_INFINITY)
				return new TSPoint(-1, pt.numberOfCycles);

			return new TSPoint(value.getValue().ticksList.length - 1, pt.numberOfCycles - 1);
		}

		return new TSPoint(pt.index - 1, pt.numberOfCycles);
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

	public final double getNextSample(double simTime) {
		return getNextSample(this, simTime);
	}

	@Override
	public final double getNextSample(Entity thisEnt, double simTime) {
		return getValue(getTSPointForTicks(getTicks(simTime)));
	}

	// ******************************************************************************************************
	// OUTPUTS
	// ******************************************************************************************************

	@Output(name = "PresentValue",
	 description = "Value for the time series at the present time.",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 1)
	public final double getPresentValue(double simTime) {
		if (value.getValue() == null)
			return Double.NaN;
		return this.getNextSample(simTime);
	}

	@Output(name = "NextTime",
	 description = "Time at which the time series value is updated next.",
	    unitType = TimeUnit.class,
	    sequence = 2)
	public final double getNextEventTime(double simTime) {
		if (value.getValue() == null)
			return 0.0d;
		return this.getNextTimeAfter(simTime);
	}

	@Output(name = "NextValue",
	 description = "Value for the time series when it is updated next.",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 3)
	public final double getNextValue(double simTime) {
		if (value.getValue() == null)
			return Double.NaN;
		EventManager evt = this.getJaamSimModel().getEventManager();
		long simTicks = evt.secondsToNearestTick(simTime);
		long nextTicks = getNextChangeAfterTicks(simTicks);
		return this.getValueForTicks(nextTicks);
	}

}
