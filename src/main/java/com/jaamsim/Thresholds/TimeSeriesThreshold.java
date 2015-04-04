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
import com.jaamsim.basicsim.Simulation;
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
		this.addInput(unitType);

		timeSeries = new TimeSeriesInput("TimeSeries", "Key Inputs", null);
		timeSeries.setUnitType(UserSpecifiedUnit.class);
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

		if (unitType.getValue() == null)
			throw new InputErrorException( "UnitType must be specified first" );

		if (timeSeries.getValue() == null)
			throw new InputErrorException( "Missing TimeSeries" );

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

	public void doOpenClose() {
		double wait;
		if( this.isClosedAtTime( getCurrentTime() ) ) {
			setOpen(false);
			wait = this.calcClosedTimeFromTime( getCurrentTime() );
		}
		else {
			setOpen(true);
			wait = this.calcOpenTimeFromTime( getCurrentTime() );
		}

		if( wait == Double.POSITIVE_INFINITY )
			return;

		this.scheduleProcess(wait * 3600.0d, 1, doOpenClose);
	}

	/**
	 * Return TRUE if the threshold is closed at the given time
	 * @param time - The time in hours
	 */
	public boolean isClosedAtTime( double time ) {

		// Add offset from input
		time += offset.getValue()/3600.0;
		time = Math.max(time, 0.0);

		double changeTime = time;

		// if the current point is closed, we are done
		if( this.isPointClosedAtHours(changeTime) ) {
			return true;
		}

		while( true ) {

			// Current point is open
			// If there has already been lookahead hours since the given time, the threshold is open
			if (changeTime - lookAhead.getValue()/3600.0 > time)
				return false;

			// If the next point is closed, determine if open long enough too satisfy lookahead
			changeTime = this.getNextChangeTimeAfterHours(changeTime);
			if( this.isPointClosedAtHours(changeTime) ) {
				return (changeTime - lookAhead.getValue()/3600.0) < time;
			}
		}
	}

	public boolean isOpenAtTicks(long ticks) {

		// Add offset from input
		ticks += FrameBox.secondsToTicks(offset.getValue());
		ticks = Math.max(ticks, 0);

		long changeTime = ticks;

		// if the current point is closed, we are done
		if( !this.isPointOpenAtTicks(changeTime) ) {
			return false;
		}

		long lookAheadInTicks = FrameBox.secondsToTicks(lookAhead.getValue());

		while( true ) {

			// Current point is open
			// If there has already been lookahead hours since the given time, the threshold is open
			if (changeTime - lookAheadInTicks > ticks)
				return true;

			// If the next point is closed, determine if open long enough too satisfy lookahead
			changeTime = this.getNextChangeAfterTicks(changeTime);
			if( !this.isPointOpenAtTicks(changeTime) ) {
				return (changeTime - lookAheadInTicks) >= ticks;
			}
		}
	}

	private static final double doubleTolerance = 1.0E-9;
	private boolean greaterCheckTolerance( double first, double second ) {
		return (first - doubleTolerance) >= second;
	}

	/**
	 * Return the time in hours during which the threshold is closed starting from the given time.
	 */
	private double calcClosedTimeFromTime( double startTime ) {

		// If the series is always outside the limits, the threshold is closed forever
		if (isAlwaysClosed())
			return Double.POSITIVE_INFINITY;

		// If the series is always within the limits, the threshold is open forever
		if (this.isAlwaysOpen())
			return 0.0;

		// If the threshold is not closed at the given time, return 0.0
		// This check must occur before adding the offset because isClosedAtTIme also adds the offset
		if( ! this.isClosedAtTime(startTime) ) {
			return 0.0;
		}

		// Add offset from input
		startTime += offset.getValue()/3600.0;
		startTime = Math.max(startTime, 0.0);

		// Threshold is currently closed. Find the next open point
		double openTime = -1;
		double changeTime = startTime;
		double maxTimeValueFromTimeSeries = this.getMaxTimeValueFromTimeSeries();
		while( true ) {
			changeTime = this.getNextChangeTimeAfterHours( changeTime );

			if( changeTime == Double.POSITIVE_INFINITY ) {

				// If an open point was found, it will be open forever
				if( openTime != -1 ) {
					return openTime - startTime;
				}

				// Threshold will never be open
				return Double.POSITIVE_INFINITY;
			}

			// if have already searched the longest cycle, the threshold will never open
			if (greaterCheckTolerance(changeTime, startTime + maxTimeValueFromTimeSeries + lookAhead.getValue()/3600.0 ))
				return Double.POSITIVE_INFINITY;

			// Closed index
			if( this.isPointClosedAtHours(changeTime) ) {

				// If an open point has not been found yet, keep looking
				if( openTime == -1 ) {
					continue;
				}

				// Has enough time been gathered to satisfy the lookahead?
				double openDuration = changeTime - openTime;
				if( openDuration >= lookAhead.getValue()/3600.0 ) {
					return openTime - startTime;
				}
				// not enough time, need to start again
				else {
					openTime = -1;
				}
			}
			// Open index
			else {

				// Keep track of the first open index.
				if( openTime == -1 ) {
					openTime = changeTime;
				}
			}
		}
	}

	public boolean isAlwaysOpen() {
		double tsMin = timeSeries.getValue().getMinValue();
		double tsMax = timeSeries.getValue().getMaxValue();

		double maxMinOpen = minOpenLimit.getValue().getMaxValue();
		double minMaxOpen = maxOpenLimit.getValue().getMinValue();

		return (tsMin >= maxMinOpen && tsMax <= minMaxOpen);
	}

	public boolean isAlwaysClosed() {
		double tsMin = timeSeries.getValue().getMinValue();
		double tsMax = timeSeries.getValue().getMaxValue();

		double minMinOpen = minOpenLimit.getValue().getMinValue();
		double maxMaxOpen = maxOpenLimit.getValue().getMaxValue();

		return (tsMax < minMinOpen || tsMin > maxMaxOpen);
	}

	/**
	 * Return the time in hours during which the threshold is open starting from the given time.
	 */
	public double calcOpenTimeFromTime( double startTime ) {

		// If the series is always outside the limits, the threshold is closed forever
		if(isAlwaysClosed())
			return 0.0;

		// If the series is always within the limits, the threshold is open forever
		if (this.isAlwaysOpen())
			return Double.POSITIVE_INFINITY;

		// If the threshold is closed at the given time, return 0.0
		// This check must occur before adding the offset because isClosedAtTIme also adds the offset
		if( this.isClosedAtTime(startTime) ) {
			return 0.0;
		}

		// Add offset from input
		startTime += offset.getValue()/3600.0;
		startTime = Math.max(startTime, 0.0);

		// Find the next change point after startTime
		double changeTime = startTime;
		double maxTimeValueFromTimeSeries = this.getMaxTimeValueFromTimeSeries();
		while( true ) {
			changeTime = this.getNextChangeTimeAfterHours( changeTime );

			if( changeTime == Double.POSITIVE_INFINITY )
				return Double.POSITIVE_INFINITY;

			// if have already searched the longest cycle, the threshold will never close
			if( changeTime > startTime + maxTimeValueFromTimeSeries )
				return Double.POSITIVE_INFINITY;

			// Closed index
			if( this.isPointClosedAtHours(changeTime) ) {

				double timeUntilClose = changeTime - lookAhead.getValue()/3600.0 - startTime;

				// if the time required is 0.0, the lookahead window is equal to the time until the next closed point.
				// Need to wait at least one clock tick before closing again.
				return Math.max(timeUntilClose, Simulation.getEventTolerance());
			}
		}
	}

	/**
	 * Return the next time that one of the parameters TimeSeries, MaxOpenLimit, or MinOpenLimit will change, after the given time.
	 * @param time
	 * @return
	 */
	public double getNextChangeTimeAfterHours(double hours) {
		double simTime = hours * 3600.0;
		double firstChange = timeSeries.getValue().getNextTimeAfter(simTime);
		firstChange = Math.min(firstChange, maxOpenLimit.getValue().getNextTimeAfter(simTime));
		firstChange = Math.min(firstChange, minOpenLimit.getValue().getNextTimeAfter(simTime));
		return firstChange/3600.0;
	}

	/**
	 * Return the next time that one of the parameters TimeSeries, MaxOpenLimit, or MinOpenLimit will change, after the given time.
	 * @param ticks - simulation time in ticks.
	 * @return
	 */
	public long getNextChangeAfterTicks(long ticks) {
		long firstChange = timeSeries.getValue().getNextChangeAfterTicks(ticks);
		firstChange = Math.min(firstChange, maxOpenLimit.getValue().getNextChangeAfterTicks(ticks));
		firstChange = Math.min(firstChange, minOpenLimit.getValue().getNextChangeAfterTicks(ticks));
		return firstChange;
	}

	/**
	 * Return either the longest cycle, or the largest time in TimeSeries, MaxOpenLimit, and MinOpenLimit. This value is used to
	 * determine whether the series has cycled around once while finding the next open/close time.
	 */
	public double getMaxTimeValueFromTimeSeries() {
		double maxCycle = timeSeries.getValue().getMaxTimeValue();
		maxCycle = Math.max(maxCycle, maxOpenLimit.getValue().getMaxTimeValue());
		maxCycle = Math.max(maxCycle, minOpenLimit.getValue().getMaxTimeValue());
		return maxCycle / 3600.0d;
	}

	/**
	 * Return either the longest cycle, or the largest time in TimeSeries, MaxOpenLimit, and MinOpenLimit. This value is used to
	 * determine whether the series has cycled around once while finding the next open/close time.
	 */
	public long getMaxTicksValueFromTimeSeries() {
		long maxCycle = timeSeries.getValue().getMaxTicksValue();
		maxCycle = Math.max(maxCycle, maxOpenLimit.getValue().getMaxTicksValue());
		maxCycle = Math.max(maxCycle, minOpenLimit.getValue().getMaxTicksValue());
		return maxCycle;
	}

	/**
	 * Return TRUE if, at the given time, the TimeSeries input value falls outside of the values for MaxOpenLimit and
	 * MinOpenLimit.
	 */
	public boolean isPointClosedAtHours( double time ) {
		double simTime = time * 3600.0d;
		double value = timeSeries.getValue().getNextSample(simTime);

		double minOpenLimitVal = minOpenLimit.getValue().getNextSample(simTime);
		double maxOpenLimitVal = maxOpenLimit.getValue().getNextSample(simTime);

		// Error check that threshold limits remain consistent
		if (minOpenLimitVal > maxOpenLimitVal)
			error("MaxOpenLimit must be larger than MinOpenLimit. MaxOpenLimit: %s, MinOpenLimit: %s, time: %s",
					maxOpenLimitVal, minOpenLimitVal, simTime);

		return (value > maxOpenLimitVal) || (value < minOpenLimitVal);
	}

	/**
	 * Return TRUE if, at the given time, the TimeSeries input value falls outside of the values for MaxOpenLimit and
	 * MinOpenLimit.
	 */
	public boolean isPointOpenAtTicks(long ticks) {

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
