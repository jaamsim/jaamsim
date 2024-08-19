/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2024 JaamSim Software Inc.
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
package com.jaamsim.Thresholds;

import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Samples.TimeSeries;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.TimeSeriesInput;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class TimeSeriesThreshold extends Threshold {

	@Keyword(description = "The TimeSeries object whose values are to be tested.",
	         exampleList = {"TimeSeries1"})
	private final TimeSeriesInput timeSeries;

	@Keyword(description = "The largest TimeSeries value for which the threshold is open. "
	                     + "The threshold is open for TimeSeries values x that satisfy "
	                     + "MinOpenLimit <= x <= MaxOpenLimit. "
	                     + "Note that the inputs for 'LookAhead' and 'Offset' can be used to "
	                     + "modify this behaviour.",
	         exampleList = {"2.0 m", "TimeSeries2"})
	private final TimeSeriesInput maxOpenLimit;

	@Keyword(description = "The smallest TimeSeries value for which the threshold is open. "
	                     + "The threshold is open for TimeSeries values x that satisfy "
	                     + "MinOpenLimit <= x <= MaxOpenLimit. "
	                     + "Note that the inputs for 'LookAhead' and 'Offset' can be used to "
	                     + "modify this behaviour.",
	         exampleList = {"2.0 m", "TimeSeries3"})
	private final TimeSeriesInput minOpenLimit;

	@Keyword(description = "The length of time over which the TimeSeries values must be "
	                     + ">= MinOpenLimit and <= MaxOpenLimit.\n"
	                     + "The threshold is open if the TimeSeries values x(t) satisfy "
	                     + "MinOpenLimit <= x(t) <= MaxOpenLimit for simulation times t from "
	                     + "(SimTime + Offset) to (SimTime + Offset + LookAhead).",
	         exampleList = {"5.0 h"})
	private final SampleInput lookAhead;

	@Keyword(description = "The amount of time that the threshold adds on to every time series "
	                     + "lookup.\n"
	                     + "The threshold is open if the TimeSeries values x(t) satisfy "
	                     + "MinOpenLimit <= x(t) <= MaxOpenLimit for simulation times t from "
	                     + "(SimTime + Offset) to (SimTime + Offset + LookAhead).",
	         exampleList = {"5.0 h"})
	private final SampleInput offset;

	@Keyword(description = "The unit type for the threshold (e.g. DistanceUnit, TimeUnit, MassUnit).",
	         exampleList = {"DistanceUnit"})
	private final UnitTypeInput unitType;

	{
		unitType = new UnitTypeInput("UnitType", KEY_INPUTS, UserSpecifiedUnit.class);
		unitType.setRequired(true);
		unitType.setCallback(inputCallback);
		this.addInput(unitType);

		timeSeries = new TimeSeriesInput("TimeSeries", KEY_INPUTS, null);
		timeSeries.setUnitType(UserSpecifiedUnit.class);
		timeSeries.setRequired(true);
		this.addInput(timeSeries);

		maxOpenLimit = new TimeSeriesInput("MaxOpenLimit", KEY_INPUTS, Double.POSITIVE_INFINITY);
		maxOpenLimit.setUnitType(UserSpecifiedUnit.class);
		this.addInput( maxOpenLimit );

		minOpenLimit = new TimeSeriesInput("MinOpenLimit", KEY_INPUTS, Double.NEGATIVE_INFINITY);
		minOpenLimit.setUnitType(UserSpecifiedUnit.class);
		this.addInput( minOpenLimit );

		lookAhead = new SampleInput("LookAhead", KEY_INPUTS, 0.0d);
		lookAhead.setUnitType(TimeUnit.class);
		this.addInput( lookAhead );

		offset = new SampleInput("Offset", KEY_INPUTS, 0.0d);
		offset.setUnitType(TimeUnit.class);
		this.addInput( offset );
	}

	static final InputCallback inputCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((TimeSeriesThreshold)ent).updateInputValue();
		}
	};

	void updateInputValue() {
		timeSeries.setUnitType(this.getUnitType());
		maxOpenLimit.setUnitType(this.getUnitType());
		minOpenLimit.setUnitType(this.getUnitType());
		updateUserOutputMap();
	}


	@Override
	public void validate() throws InputErrorException {
		super.validate();

		if( (maxOpenLimit.getValue().getMinValue() == Double.POSITIVE_INFINITY) &&
				(minOpenLimit.getValue().getMaxValue() == Double.NEGATIVE_INFINITY) ) {
			throw new InputErrorException( "Missing Limit" );
		}

		if (minOpenLimit.getValue().getMaxValue() > maxOpenLimit.getValue().getMaxValue())
			throw new InputErrorException("MaxOpenLimit must be larger than MinOpenLimit");

		if( timeSeries.getValue().getUnitType() != this.getUnitType() )
			throw new InputErrorException("Time Series unitType (%s) does not match the Threshold Unit type (%s)",
					timeSeries.getValue().getUnitType(), this.getUnitType());

		if (timeSeries.getValue().getMinValue() > maxOpenLimit.getValue().getMaxValue())
			InputAgent.logWarning(getJaamSimModel(),
					"Threshold %s is closed forever.  MaxOpenLimit = %f Min TimeSeries Value = %f",
					this, maxOpenLimit.getValue().getMaxValue(), timeSeries.getValue().getMinValue());

		if (timeSeries.getValue().getMaxValue() < minOpenLimit.getValue().getMinValue())
			InputAgent.logWarning(getJaamSimModel(),
					"Threshold %s is closed forever.  MinOpenLimit = %f Max TimeSeries Value = %f",
					this, minOpenLimit.getValue().getMinValue(), timeSeries.getValue().getMaxValue());
	}

	@Override
	public void startUp() {
		super.startUp();
		this.doOpenClose();
	}

	@Override
	public Class<? extends Unit> getUserUnitType() {
		return unitType.getUnitType();
	}

	public Class<? extends Unit> getUnitType() {
		return unitType.getUnitType();
	}

	private static class DoOpenCloseTarget extends EntityTarget<TimeSeriesThreshold> {
		public DoOpenCloseTarget(TimeSeriesThreshold ent, String method) {
			super(ent, method);
		}

		@Override
		public void process() {
			ent.doOpenClose();
		}
	}

	private final ProcessTarget doOpenClose = new DoOpenCloseTarget(this, "doOpenClose");

	/**
	 * The process loop that opens and closes the threshold.
	 */
	public void doOpenClose() {
		long wait;
		if (this.isOpenAtTicks(getSimTicks())) {
			setOpen(true);
			wait = this.calcOpenTicksFromTicks(getSimTicks());
		}
		else {
			setOpen(false);
			wait = this.calcClosedTicksFromTicks(getSimTicks());
		}

		if (wait == Long.MAX_VALUE)
			return;

		this.scheduleProcessTicks(wait, 1, doOpenClose);
	}

	/**
	 * Return TRUE if the threshold is open at the given time
	 * @param simTime - simulation time in seconds
	 * @return TRUE if open, FALSE if closed
	 */
	public boolean isOpenAtTime(double simTime) {
		EventManager evt = this.getJaamSimModel().getEventManager();
		return isOpenAtTicks(evt.secondsToNearestTick(simTime));
	}

	/**
	 * Returns TRUE if the threshold is open at the given time.
	 * <p>
	 * Note: if lookahead > 0, the condition for open is that opentime >= lookahead.
	 * However, if the lookahead == 0, the condition for open is that opentime > lookahead.
	 * @param ticks - simulation time in clock ticks
	 * @return TRUE if open, FALSE if closed
	 */
	private boolean isOpenAtTicks(long ticks) {

		// Add offset from input
		double simTime = getSimTime();
		EventManager evt = this.getJaamSimModel().getEventManager();
		ticks += evt.secondsToNearestTick(offset.getNextSample(this, simTime));
		ticks = Math.max(ticks, 0);

		long changeTime = ticks;

		// if the current point is closed, we are done
		if (!this.isPointOpenAtTicks(changeTime))
			return false;

		// If there is no lookahead, then the threshold is open
		long lookAheadInTicks = evt.secondsToNearestTick(lookAhead.getNextSample(this, simTime));
		if (lookAheadInTicks == 0)
			return true;

		while( true ) {

			// If the next point is closed, determine if open long enough too satisfy lookahead
			changeTime = this.getNextChangeAfterTicks(changeTime);
			if (!this.isPointOpenAtTicks(changeTime))
				return (changeTime - ticks >= lookAheadInTicks);

			// The next point is open, determine whether the lookahead is already satisfied
			if (changeTime - ticks >= lookAheadInTicks)
				return true;
		}
	}

	/**
	 * Return the time during which the threshold is closed starting from the given time.
	 * @param ticks - simulation time in clock ticks
	 * @return the time in clock ticks that the threshold is closed
	 */
	private long calcClosedTicksFromTicks(long ticks) {

		// If the series is always outside the limits, the threshold is closed forever
		if (isAlwaysClosed())
			return Long.MAX_VALUE;

		// If the series is always within the limits, the threshold is open forever
		if (this.isAlwaysOpen())
			return 0;

		// If the threshold is not closed at the given time, return 0.0
		// This check must occur before adding the offset because isClosedAtTicks also adds the offset
		if (this.isOpenAtTicks(ticks))
			return 0;

		EventManager evt = this.getJaamSimModel().getEventManager();
		// Add offset from input
		double simTime = getSimTime();
		ticks += evt.secondsToNearestTick(offset.getNextSample(this, simTime));
		ticks = Math.max(ticks, 0);

		// Threshold is currently closed. Find the next open point
		long openTime = -1;
		long changeTime = ticks;
		long maxTicksValueFromTimeSeries = this.getMaxTicksValueFromTimeSeries();
		long lookAheadInTicks = evt.secondsToNearestTick(lookAhead.getNextSample(this, simTime));
		while( true ) {
			changeTime = this.getNextChangeAfterTicks(changeTime);

			if (changeTime == Long.MAX_VALUE) {
				if( openTime == -1 )
					return Long.MAX_VALUE;
				else
					return openTime - ticks;
			}

			// if have already searched the longest cycle, the threshold will never open
			if (changeTime > ticks + maxTicksValueFromTimeSeries + lookAheadInTicks)
				return Long.MAX_VALUE;

			// Closed index
			if (!this.isPointOpenAtTicks(changeTime)) {

				// If an open point has not been found yet, keep looking
				if (openTime == -1)
					continue;

				// Has enough time been gathered to satisfy the lookahead?
				if (changeTime - openTime >= lookAheadInTicks)
					return openTime - ticks;

				// not enough time, need to start again
				else
					openTime = -1;
			}

			// Open index
			else {

				// Keep track of the first open index.
				if (openTime == -1)
					openTime = changeTime;
			}
		}
	}

	private boolean isAlwaysOpen() {
		double tsMin = timeSeries.getValue().getMinValue();
		double tsMax = timeSeries.getValue().getMaxValue();

		double maxMinOpen = minOpenLimit.getValue().getMaxValue();
		double minMaxOpen = maxOpenLimit.getValue().getMinValue();

		return (tsMin >= maxMinOpen && tsMax <= minMaxOpen);
	}

	private boolean isAlwaysClosed() {
		double tsMin = timeSeries.getValue().getMinValue();
		double tsMax = timeSeries.getValue().getMaxValue();

		double minMinOpen = minOpenLimit.getValue().getMinValue();
		double maxMaxOpen = maxOpenLimit.getValue().getMaxValue();

		return (tsMax < minMinOpen || tsMin > maxMaxOpen);
	}

	/**
	 * Return the time during which the threshold is open starting from the given time.
	 * @param ticks - simulation time in clock ticks
	 * @return the time in clock ticks that the threshold is open
	 */
	private long calcOpenTicksFromTicks(long ticks) {

		// If the series is always outside the limits, the threshold is closed forever
		if (isAlwaysClosed())
			return 0;

		// If the series is always within the limits, the threshold is open forever
		if (this.isAlwaysOpen())
			return Long.MAX_VALUE;

		// If the threshold is closed at the given time, return 0.0
		// This check must occur before adding the offset because isClosedAtTIme also adds the offset
		if (!this.isOpenAtTicks(ticks))
			return 0;

		// Add offset from input
		double simTime = getSimTime();
		EventManager evt = this.getJaamSimModel().getEventManager();
		ticks += evt.secondsToNearestTick(offset.getNextSample(this, simTime));
		ticks = Math.max(ticks, 0);

		// Find the next change point after startTime
		long changeTime = ticks;
		long maxTicksValueFromTimeSeries = this.getMaxTicksValueFromTimeSeries();
		long lookAheadInTicks = evt.secondsToNearestTick(lookAhead.getNextSample(this, simTime));
		while( true ) {
			changeTime = this.getNextChangeAfterTicks(changeTime);

			if( changeTime == Long.MAX_VALUE )
				return Long.MAX_VALUE;

			// if have already searched the longest cycle, the threshold will never close
			if( changeTime > ticks + maxTicksValueFromTimeSeries )
				return Long.MAX_VALUE;

			// Closed index
			if (!this.isPointOpenAtTicks(changeTime)) {
				if (lookAheadInTicks == 0)
					return changeTime - ticks;
				else
					return changeTime - lookAheadInTicks - ticks + 1;
			}
		}
	}

	/**
	 * Return the time in seconds during which the threshold is open
	 * from the given start time to the given end time
	 * @param startTime - simulation start time in seconds
	 * @param endTime - simulation end time in seconds
	 * @return the time in seconds that the threshold is open
	 */
	public double calcOpenTimeFromTimeToTime(double startTime, double endTime) {

		// If the series is always outside the limits, the threshold is closed forever
		if (isAlwaysClosed())
			return 0;

		// If the series is always within the limits, the threshold is open forever
		if (this.isAlwaysOpen())
			return endTime - startTime;

		EventManager evt = this.getJaamSimModel().getEventManager();
		long ticks = evt.secondsToNearestTick(startTime);
		long endTicks = evt.secondsToNearestTick(endTime);
		long openTicks = 0;

		boolean done = false;
		while (! done) {
			if (this.isOpenAtTicks(ticks)) {
				long tempTicks = this.calcOpenTicksFromTicks(ticks);
				if (ticks + tempTicks >= endTicks) {
					openTicks += Math.min( tempTicks, endTicks - ticks);
					done = true;
				}
				else {
					openTicks += tempTicks;
					ticks += tempTicks;
				}
			}
			else {
				long tempTicks = this.calcClosedTicksFromTicks(ticks);
				if (ticks + tempTicks >= endTicks) {
					done = true;
				}
				else {
					ticks += tempTicks;
				}
			}
		}
		return evt.ticksToSeconds(openTicks);
	}

	/**
	 * Returns the last time that one of the parameters TimeSeries, MaxOpenLimit, or MinOpenLimit
	 * changed, before the given time.
	 * @param simTime - simulation time in seconds
	 * @return the last simulation time in seconds that a change occurred
	 */
	public double getLastChangeBeforeTime(double simTime) {
		EventManager evt = this.getJaamSimModel().getEventManager();
		return evt.ticksToSeconds(getLastChangeBeforeTicks(evt.secondsToNearestTick(simTime)));
	}

	/**
	 * Returns the last time that one of the parameters TimeSeries, MaxOpenLimit, or MinOpenLimit
	 * changed, before the given time.
	 * @param ticks - simulation time in clock ticks.
	 * @return the last time in clock ticks that a change occurred
	 */
	private long getLastChangeBeforeTicks(long ticks) {
		long lastChange = timeSeries.getValue().getLastChangeBeforeTicks(ticks);
		lastChange = Math.min(lastChange, maxOpenLimit.getValue().getLastChangeBeforeTicks(ticks));
		lastChange = Math.min(lastChange, minOpenLimit.getValue().getLastChangeBeforeTicks(ticks));
		return lastChange;
	}

	/**
	 * Returns the next time that one of the parameters TimeSeries, MaxOpenLimit, or MinOpenLimit
	 * will change, after the given time.
	 * @param simTime - simulation time in seconds
	 * @return the next simulation time in seconds that a change will occur
	 */
	public double getNextChangeAfterTime(double simTime) {
		EventManager evt = this.getJaamSimModel().getEventManager();
		return evt.ticksToSeconds(getNextChangeAfterTicks(evt.secondsToNearestTick(simTime)));
	}

	/**
	 * Returns the next time that one of the parameters TimeSeries, MaxOpenLimit, or MinOpenLimit
	 * will change, after the given time.
	 * @param ticks - simulation time in clock ticks.
	 * @return the next time in clock ticks that a change will occur
	 */
	private long getNextChangeAfterTicks(long ticks) {
		long firstChange = timeSeries.getValue().getNextChangeAfterTicks(ticks);
		firstChange = Math.min(firstChange, maxOpenLimit.getValue().getNextChangeAfterTicks(ticks));
		firstChange = Math.min(firstChange, minOpenLimit.getValue().getNextChangeAfterTicks(ticks));
		return firstChange;
	}

	/**
	 * Returns the largest time in TimeSeries, MaxOpenLimit, and MinOpenLimit time series.
	 * This value is used to determine whether the series has cycled around once while finding the next open/close time.
	 * @return the last time in clock ticks that a change will occur
	 */
	private long getMaxTicksValueFromTimeSeries() {
		long maxCycle = timeSeries.getValue().getMaxTicksValue();
		maxCycle = Math.max(maxCycle, maxOpenLimit.getValue().getMaxTicksValue());
		maxCycle = Math.max(maxCycle, minOpenLimit.getValue().getMaxTicksValue());
		return maxCycle;
	}

	/**
	 * Return TRUE if, at the given time, the TimeSeries input value falls outside of the values for MaxOpenLimit and
	 * MinOpenLimit. Does not include the effect of the offset value.
	 * @param ticks - simulation time in clock ticks.
	 * @return TRUE if open, FALSE if closed
	 */
	private boolean isPointOpenAtTicks(long ticks) {

		double value = timeSeries.getValue().getValueForTicks(ticks);
		double minOpenLimitVal = minOpenLimit.getValue().getValueForTicks(ticks);
		double maxOpenLimitVal = maxOpenLimit.getValue().getValueForTicks(ticks);

		// Error check that threshold limits remain consistent
		if (minOpenLimitVal > maxOpenLimitVal) {
			EventManager evt = this.getJaamSimModel().getEventManager();
			error("MaxOpenLimit must be larger than MinOpenLimit. MaxOpenLimit: %s, MinOpenLimit: %s, time: %s",
					maxOpenLimitVal, minOpenLimitVal, evt.ticksToSeconds(ticks));
		}
		return (value >= minOpenLimitVal) && (value <= maxOpenLimitVal);
	}

	@Output(name = "TimeSeriesValue",
	 description = "The value of the TimeSeries object at the present time plus the offset.",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 5)
	public double getTimeSeriesValue(double simTime) {
		if (timeSeries.getValue() == null)
			return Double.NaN;
		return ((TimeSeries)timeSeries.getValue()).getPresentValue(simTime + offset.getNextSample(this, simTime));
	}

	@Output(name = "NextOpenTime",
	 description = "The next time at which the threshold will be open.",
	    unitType = TimeUnit.class,
	    sequence = 6)
	public double getNextOpenTime(double simTime) {
		if (timeSeries.getValue() == null)
			return Double.NaN;
		EventManager evt = this.getJaamSimModel().getEventManager();
		long simTicks = evt.secondsToNearestTick(simTime);
		long ticks = simTicks + calcClosedTicksFromTicks(simTicks);
		return evt.ticksToSeconds(ticks);
	}

	@Output(name = "NextCloseTime",
	 description = "The next time at which the threshold will be closed.",
	    unitType = TimeUnit.class,
	    sequence = 7)
	public double getNextCloseTime(double simTime) {
		if (timeSeries.getValue() == null)
			return Double.NaN;
		EventManager evt = this.getJaamSimModel().getEventManager();
		long simTicks = evt.secondsToNearestTick(simTime);
		long ticks = simTicks + calcOpenTicksFromTicks(simTicks);
		return evt.ticksToSeconds(ticks);
	}

	@Output(name = "NextOpenDuration",
	 description = "The duration for which the threshold will be open at the next open time.",
	    unitType = TimeUnit.class,
	    sequence = 8)
	public double getNextOpenDuration(double simTime) {
		if (timeSeries.getValue() == null)
			return Double.NaN;
		EventManager evt = this.getJaamSimModel().getEventManager();
		long simTicks = evt.secondsToNearestTick(simTime);
		long ticks = calcOpenTicksFromTicks(simTicks + calcClosedTicksFromTicks(simTicks));
		return evt.ticksToSeconds(ticks);
	}

	@Output(name = "NextCloseDuration",
	 description = "The duration for which the threshold will be closed at the next close time.",
	    unitType = TimeUnit.class,
	    sequence = 9)
	public double getNextCloseDuration(double simTime) {
		if (timeSeries.getValue() == null)
			return Double.NaN;
		EventManager evt = this.getJaamSimModel().getEventManager();
		long simTicks = evt.secondsToNearestTick(simTime);
		long ticks = calcClosedTicksFromTicks(simTicks + calcOpenTicksFromTicks(simTicks));
		return evt.ticksToSeconds(ticks);
	}

}
