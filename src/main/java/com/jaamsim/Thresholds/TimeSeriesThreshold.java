/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.Thresholds;

import com.jaamsim.Samples.TimeSeriesConstantDouble;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.TimeSeriesInput;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class TimeSeriesThreshold extends Threshold {

	@Keyword(description = "The name of time series for which the threshold applies.",
	         example = "Threshold1  TimeSeries { TimeSeries1 }")
	private final TimeSeriesInput timeSeries;

	@Keyword(description = "The limit over which the threshold is closed.  " +
			"The limit must be specified after the time series and " +
			"will use the same unit type as the threshold.",
    example = "Threshold1  MaxOpenLimit { 2.0 m }")
	private final TimeSeriesInput maxOpenLimit;

	@Keyword(description = "The limit under which the threshold is closed.  " +
			"The limit must be specified after the time series and " +
			"will use the same unit type as the threshold.",
    example = "Threshold1  MinOpenLimit { 2.0 m }")
	private final TimeSeriesInput minOpenLimit;

	@Keyword(description = "The amount of time that the threshold must remain within minOpenLimit and maxOpenLimit to be considered open.",
            example = "Threshold1  LookAhead { 5.0 h }")
	private final ValueInput lookAhead;

	@Keyword(description = "The amount of time that the threshold adds on to every time series lookup.",
           example = "Threshold1  Offset { 5.0 h }")
	private final ValueInput offset;

	@Keyword(description = "The unit type for the threshold (e.g. DistanceUnit, TimeUnit, MassUnit).",
    example = "Threshold1  UnitType { DistanceUnit }")
	private final UnitTypeInput unitType;

	{
		unitType = new UnitTypeInput("UnitType", "Key Inputs", UserSpecifiedUnit.class);
		unitType.setRequired(true);
		this.addInput(unitType);

		timeSeries = new TimeSeriesInput("TimeSeries", "Key Inputs", null);
		timeSeries.setUnitType(UserSpecifiedUnit.class);
		timeSeries.setRequired(true);
		this.addInput(timeSeries);

		maxOpenLimit = new TimeSeriesInput("MaxOpenLimit", "Key Inputs", new TimeSeriesConstantDouble(Double.POSITIVE_INFINITY));
		maxOpenLimit.setUnitType(UserSpecifiedUnit.class);
		this.addInput( maxOpenLimit );

		minOpenLimit = new TimeSeriesInput("MinOpenLimit", "Key Inputs", new TimeSeriesConstantDouble(Double.NEGATIVE_INFINITY));
		minOpenLimit.setUnitType(UserSpecifiedUnit.class);
		this.addInput( minOpenLimit );

		lookAhead = new ValueInput( "LookAhead", "Key Inputs", 0.0d );
		lookAhead.setUnitType(TimeUnit.class);
		this.addInput( lookAhead );

		offset = new ValueInput( "Offset", "Key Inputs", 0.0d );
		offset.setUnitType(TimeUnit.class);
		this.addInput( offset );
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if (in == unitType) {
			timeSeries.setUnitType(this.getUnitType());
			maxOpenLimit.setUnitType(this.getUnitType());
			minOpenLimit.setUnitType(this.getUnitType());
		}
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
			InputAgent.logWarning("Threshold %s is closed forever.  MaxOpenLimit = %f Max TimeSeries Value = %f",
					this, maxOpenLimit.getValue().getMaxValue(), timeSeries.getValue().getMaxValue());

		if (timeSeries.getValue().getMaxValue() < minOpenLimit.getValue().getMaxValue())
			InputAgent.logWarning("Threshold %s is closed forever.  MinOpenLimit = %f Min TimeSeries Value = %f",
					this, minOpenLimit.getValue().getMaxValue(), timeSeries.getValue().getMinValue());
	}

	@Override
	public void startUp() {
		super.startUp();
		this.doOpenClose();
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
		return isOpenAtTicks(FrameBox.secondsToTicks(simTime));
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
		ticks += FrameBox.secondsToTicks(offset.getValue());
		ticks = Math.max(ticks, 0);

		long changeTime = ticks;

		// if the current point is closed, we are done
		if (!this.isPointOpenAtTicks(changeTime))
			return false;

		// If there is no lookahead, then the threshold is open
		long lookAheadInTicks = FrameBox.secondsToTicks(lookAhead.getValue());
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

		// Add offset from input
		ticks += FrameBox.secondsToTicks(offset.getValue());
		ticks = Math.max(ticks, 0);

		// Threshold is currently closed. Find the next open point
		long openTime = -1;
		long changeTime = ticks;
		long maxTicksValueFromTimeSeries = this.getMaxTicksValueFromTimeSeries();
		long lookAheadInTicks = FrameBox.secondsToTicks(lookAhead.getValue());
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
		ticks += FrameBox.secondsToTicks(offset.getValue());
		ticks = Math.max(ticks, 0);

		// Find the next change point after startTime
		long changeTime = ticks;
		long maxTicksValueFromTimeSeries = this.getMaxTicksValueFromTimeSeries();
		long lookAheadInTicks = FrameBox.secondsToTicks(lookAhead.getValue());
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
		if (minOpenLimitVal > maxOpenLimitVal)
			error("MaxOpenLimit must be larger than MinOpenLimit. MaxOpenLimit: %s, MinOpenLimit: %s, time: %s",
					maxOpenLimitVal, minOpenLimitVal, FrameBox.ticksToSeconds(ticks));

		return (value >= minOpenLimitVal) && (value <= maxOpenLimitVal);
	}

}
