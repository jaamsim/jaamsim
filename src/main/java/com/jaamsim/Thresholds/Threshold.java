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

import java.util.ArrayList;

import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityTarget;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Process;
import com.sandwell.JavaSimulation.TimeSeriesInput;
import com.sandwell.JavaSimulation.TimeSeriesProvider;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.DisplayModelCompat;

public class Threshold extends DisplayEntity {

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

	@Keyword(description = "The colour of the threshold graphic when the present time series value is less than or equal to the MaxOpenLimit and " +
					"greater than or equal to the MinOpenLimit.",
	         example = "Threshold1  OpenColour { green }")
	private final ColourInput openColour;

	@Keyword(description = "The colour of the threshold graphic when the present time series value is greater than the MaxOpenLimit " +
					" or less than the MinOpenLimit.",
			example = "Threshold1  ClosedColour { red }")
	private final ColourInput closedColour;

	@Keyword(description = "A Boolean value.  If TRUE, the threshold is displayed when it is open.",
	         example = "Threshold1 ShowWhenOpen { FALSE }")
	private final BooleanInput showWhenOpen;

	@Keyword(description = "A Boolean value.  If TRUE, the threshold is displayed when it is closed.",
	         example = "Threshold1 ShowWhenClosed { FALSE }")
	private final BooleanInput showWhenClosed;

	private final ArrayList<ThresholdUser> userList;

	private boolean closed;

	private double simTimeOfLastUpdate; // Simulation time in seconds of last update
	private double openSimTime; // Number of seconds open
	private double closedSimTime; // Number of seconds closed

	{
		unitType = new UnitTypeInput("UnitType", "Key Inputs", UserSpecifiedUnit.class);
		this.addInput(unitType, true);

		timeSeries = new TimeSeriesInput("TimeSeries", "Key Inputs", null);
		timeSeries.setUnitType(UserSpecifiedUnit.class);
		this.addInput(timeSeries, true);

		maxOpenLimit = new TimeSeriesInput( "MaxOpenLimit", "Key Inputs", null );
		maxOpenLimit.setUnitType(UserSpecifiedUnit.class);
		this.addInput( maxOpenLimit, true );

		minOpenLimit = new TimeSeriesInput( "MinOpenLimit", "Key Inputs", null );
		minOpenLimit.setUnitType(UserSpecifiedUnit.class);
		this.addInput( minOpenLimit, true );

		lookAhead = new ValueInput( "LookAhead", "Key Inputs", 0.0d );
		lookAhead.setUnitType(TimeUnit.class);
		this.addInput( lookAhead, true );

		offset = new ValueInput( "Offset", "Key Inputs", 0.0d );
		offset.setUnitType(TimeUnit.class);
		this.addInput( offset, true );

		openColour = new ColourInput( "OpenColour", "Graphics", ColourInput.GREEN );
		this.addInput( openColour, true, "OpenColor" );

		closedColour = new ColourInput( "ClosedColour", "Graphics", ColourInput.RED );
		this.addInput( closedColour, true, "ClosedColor" );

		showWhenOpen = new BooleanInput("ShowWhenOpen", "Graphics", true);
		this.addInput(showWhenOpen, true);

		showWhenClosed = new BooleanInput("ShowWhenClosed", "Graphics", true);
		this.addInput(showWhenClosed, true);
	}

	public Threshold() {
		userList = new ArrayList<ThresholdUser>();
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

		if( unitType.getValue() == null )
			throw new InputErrorException( "UnitType must be specified first" );

		if( timeSeries.getValue() == null ) {
			throw new InputErrorException( "Missing TimeSeries" );
		}
		if( (maxOpenLimit.getValue() == null) && (minOpenLimit.getValue() == null) ) {
			throw new InputErrorException( "Missing Limit" );
		}

		if( maxOpenLimit.getValue() != null && minOpenLimit.getValue() != null ) {
			if( this.getMaxMinOpenLimit() > this.getMaxMaxOpenLimit() ) {
				throw new InputErrorException( "MaxOpenLimit must be larger than MinOpenLimit" );
			}
		}

		if( timeSeries.getValue().getUnitType() != this.getUnitType() )
			throw new InputErrorException( "Time Series unitType ("+timeSeries.getValue().getUnitType()+") does not match the Threshold Unit type ("+this.getUnitType()+")" );

		if( this.getTimeSeries().getMinValue() > this.getMaxMaxOpenLimit() )
			InputAgent.logWarning( "Threshold %s is closed forever.  MaxOpenLimit = %f Max TimeSeries Value = %f", this, this.getMaxMaxOpenLimit(), this.getTimeSeries().getMaxValue() );

		if( this.getTimeSeries().getMaxValue() < this.getMaxMinOpenLimit() )
			InputAgent.logWarning( "Threshold %s is closed forever.  MinOpenLimit = %f Min TimeSeries Value = %f", this, this.getMaxMinOpenLimit(), this.getTimeSeries().getMinValue() );
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		userUpdate.users.clear();
		closed = false;

		userList.clear();
		for (Entity each : Entity.getAll()) {
			if (each instanceof ThresholdUser) {
				ThresholdUser tu = (ThresholdUser)each;
				if (tu.getThresholds().contains(this))
					userList.add(tu);
			}
		}
	}

	@Override
	public void startUp() {
		super.startUp();
		this.clearStatistics();
		this.doOpenClose();
	}

	public Class<? extends Unit> getUnitType() {
		return unitType.getUnitType();
	}

	private static class DoOpenCloseTarget extends EntityTarget<Threshold> {
		public DoOpenCloseTarget(Threshold ent, String method) {
			super(ent, method);
		}

		@Override
		public void process() {
			ent.doOpenClose();
		}
	}

	private final ProcessTarget doOpenClose = new DoOpenCloseTarget(this, "doOpenClose");
	public void doOpenClose() {
		this.update();
		double wait;
		if( this.isClosedAtTime( getCurrentTime() ) ) {
			closed = true;
			if( traceFlag ) this.trace( "Closed" );
			for( ThresholdUser user : userList ) {
				if (!userUpdate.users.contains(user))
					userUpdate.users.add(user);
				user.thresholdClosed();
			}
			if (!userUpdate.users.isEmpty())
				this.scheduleSingleProcess(userUpdate, 2);
			wait = this.calcClosedTimeFromTime( getCurrentTime() );
		}
		else {
			closed = false;
			if( traceFlag ) this.trace( "Open" );
			for( ThresholdUser user : userList ) {
				if (!userUpdate.users.contains(user))
					userUpdate.users.add(user);
				user.thresholdOpen();
			}
			if (!userUpdate.users.isEmpty())
				this.scheduleSingleProcess(userUpdate, 2);
			wait = this.calcOpenTimeFromTime( getCurrentTime() );
		}

		if( wait == Double.POSITIVE_INFINITY )
			return;

		this.scheduleProcess(wait * 3600.0d, 1, doOpenClose);
	}

	private static final DoThresholdChanged userUpdate = new DoThresholdChanged();
	private static class DoThresholdChanged extends ProcessTarget {
		final ArrayList<ThresholdUser> users = new ArrayList<ThresholdUser>();

		public DoThresholdChanged() {}

		@Override
		public void process() {
			for (ThresholdUser each : users)
				each.thresholdChanged();

			users.clear();
		}

		@Override
		public String getDescription() {
			return "UpdateAllThresholdUsers";
		}
	}

	public boolean isClosed() {
		return closed;
	}

	/**
	 * Return TRUE if the threshold is closed at the given time
	 * @param time - The time in hours
	 */
	public boolean isClosedAtTime( double time ) {

		// Add offset from input
		time += this.getOffsetInHours();

		double changeTime = time;

		// if the current point is closed, we are done
		if( this.isPointClosed(changeTime) ) {
			return true;
		}

		while( true ) {

			// Current point is open
			// If there has already been lookahead hours since the given time, the threshold is open
			if( changeTime - this.getLookAheadInHours() > time )
				return false;

			// If the next point is closed, determine if open long enough too satisfy lookahead
			changeTime = this.getNextChangeTimeAfterHours(changeTime);
			if( this.isPointClosed(changeTime) ) {
				return (changeTime - this.getLookAheadInHours()) < time;
			}
		}
	}

	/**
	 * Return the time in hours during which the threshold is closed starting from the given time.
	 */
	public double calcClosedTimeFromTime( double startTime ) {

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
		startTime += this.getOffsetInHours();

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
			if( changeTime > startTime + maxTimeValueFromTimeSeries )
				return Double.POSITIVE_INFINITY;

			// Closed index
			if( this.isPointClosed(changeTime) ) {

				// If an open point has not been found yet, keep looking
				if( openTime == -1 ) {
					continue;
				}

				// Has enough time been gathered to satisfy the lookahead?
				double openDuration = changeTime - openTime;
				if( openDuration >= this.getLookAheadInHours() ) {
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
		double tsMin = this.getTimeSeries().getMinValue();
		double tsMax = this.getTimeSeries().getMaxValue();

		double maxMinOpen = this.getMaxMinOpenLimit();
		double minMaxOpen = this.getMinMaxOpenLimit();

		if (tsMin >= maxMinOpen && tsMax <= minMaxOpen)
			return true;
		else
			return false;
	}

	public boolean isAlwaysClosed() {
		double tsMin = this.getTimeSeries().getMinValue();
		double tsMax = this.getTimeSeries().getMaxValue();

		double minMinOpen = this.getMinMinOpenLimit();
		double maxMaxOpen = this.getMaxMaxOpenLimit();

		if (tsMax < minMinOpen || tsMin > maxMaxOpen)
			return true;
		else
			return false;
	}

	public double getLookAheadInHours() {
		return lookAhead.getValue() / 3600;
	}

	public double getLookAhead() {
		return lookAhead.getValue();
	}

	public double getOffsetInHours() {
		return offset.getValue() / 3600;
	}

	public double getOffset() {
		return offset.getValue();
	}

	private TimeSeriesProvider getTimeSeries() {
		return timeSeries.getValue();
	}

	public double getMaxMinOpenLimit() {
		if( minOpenLimit.getValue() == null )
			return Double.NEGATIVE_INFINITY;

		return minOpenLimit.getValue().getMaxValue();
	}

	public double getMinMinOpenLimit() {
		if (minOpenLimit.getValue() == null)
			return Double.NEGATIVE_INFINITY;

		return minOpenLimit.getValue().getMinValue();
	}


	public double getMaxMaxOpenLimit() {
		if (maxOpenLimit.getValue() == null)
			return Double.POSITIVE_INFINITY;

		return maxOpenLimit.getValue().getMaxValue();
	}

	public double getMinMaxOpenLimit() {
		if (maxOpenLimit.getValue() == null)
			return Double.POSITIVE_INFINITY;

		return maxOpenLimit.getValue().getMinValue();
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
		startTime += this.getOffsetInHours();

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
			if( this.isPointClosed(changeTime) ) {

				double timeUntilClose = changeTime - this.getLookAheadInHours() - startTime;

				// if the time required is 0.0, the lookahead window is equal to the time until the next closed point.
				// Need to wait at least one clock tick before closing again.
				return Math.max(timeUntilClose, Process.getEventTolerance());
			}
		}
	}

	/**
	 * Return the next time that one of the parameters TimeSeries, MaxOpenLimit, or MinOpenLimit will change, after the given time.
	 * @param time
	 * @return
	 */
	public double getNextChangeTimeAfterHours( double time ) {
		double firstChange = this.getTimeSeries().getNextChangeTimeAfterHours(time);

		if (maxOpenLimit.getValue() != null)
			firstChange = Math.min(firstChange, maxOpenLimit.getValue().getNextChangeTimeAfterHours(time));

		if (minOpenLimit.getValue() != null)
			firstChange = Math.min(firstChange, minOpenLimit.getValue().getNextChangeTimeAfterHours(time));

		return firstChange;
	}

	/**
	 * Return either the longest cycle, or the largest time in TimeSeries, MaxOpenLimit, and MinOpenLimit. This value is used to
	 * determine whether the series has cycled around once while finding the next open/close time.
	 */
	public double getMaxTimeValueFromTimeSeries() {
		double maxCycle = this.getTimeSeries().getMaxTimeValue();

		if (maxOpenLimit.getValue() != null)
			maxCycle = Math.max(maxCycle, maxOpenLimit.getValue().getMaxTimeValue());

		if (minOpenLimit.getValue() != null)
			maxCycle = Math.max(maxCycle, minOpenLimit.getValue().getMaxTimeValue());

		// return in hours for now
		return maxCycle / 3600.0d;
	}

	/**
	 * Return TRUE if, at the given time, the TimeSeries input value falls outside of the values for MaxOpenLimit and
	 * MinOpenLimit.
	 */
	public boolean isPointClosed( double time ) {
		double value = this.getTimeSeries().getValueForTimeHours(time);

		double minOpenLimitVal = Double.NEGATIVE_INFINITY;
		if (minOpenLimit.getValue() != null)
			minOpenLimitVal = minOpenLimit.getValue().getValueForTimeHours(time);

		double maxOpenLimitVal = Double.POSITIVE_INFINITY;
		if (maxOpenLimit.getValue() != null)
			maxOpenLimitVal = maxOpenLimit.getValue().getValueForTimeHours(time);

		// Error check that threshold limits remain consistent
		if (minOpenLimitVal > maxOpenLimitVal)
			this.error( "isPointClosed( "+time+" )", "MaxOpenLimit must be larger than MinOpenLimit", "MaxOpenLimit = "+maxOpenLimitVal+" MinOpenLimit = "+minOpenLimitVal );


		if (value > maxOpenLimitVal)
			return true;

		if (value < minOpenLimitVal)
			return true;

		return false;
	}

	@Override
	public void updateGraphics( double time ) {
		super.updateGraphics(time);

		// Determine the colour for the square
		Color4d col;
		if( timeSeries.getValue() != null && closed )
			col = closedColour.getValue();
		else
			col = openColour.getValue();

		if (closed) {
			setTagVisibility(DisplayModelCompat.TAG_CONTENTS, showWhenClosed.getValue());
			setTagVisibility(DisplayModelCompat.TAG_OUTLINES, showWhenClosed.getValue());
		}
		else {
			setTagVisibility(DisplayModelCompat.TAG_CONTENTS, showWhenOpen.getValue());
			setTagVisibility(DisplayModelCompat.TAG_OUTLINES, showWhenOpen.getValue());
		}

		setTagColour( DisplayModelCompat.TAG_CONTENTS, col );
		setTagColour( DisplayModelCompat.TAG_OUTLINES, ColourInput.BLACK );
	}

	// ********************************************************************************
    // Reporting
    // ********************************************************************************

	public void clearStatistics() {
		openSimTime = 0.0;
		closedSimTime = 0.0;
		simTimeOfLastUpdate = getSimTime();
	}

	public void update() {
		if( closed )
			closedSimTime += getSimTime() - simTimeOfLastUpdate;
		else
			openSimTime += getSimTime() - simTimeOfLastUpdate;

		simTimeOfLastUpdate = getSimTime();
	}

	/**
	 * Prints the header for the statistics
	 */
	public void printUtilizationHeaderOn( FileEntity anOut ) {
		anOut.format( "Name\t" );
		anOut.format( "Open\t" );
		anOut.format( "Closed\t" );
	}

	/**
	 * Print the threshold name and percentage of time open and closed
	 */
	public void printUtilizationOn( FileEntity anOut ) {
		this.update();

		double totalSimTime = openSimTime + closedSimTime;
		if (totalSimTime == 0.0d)
			return;

		anOut.format( "%s\t", getName() );

		// Print percentage of time open
		double fraction = openSimTime/totalSimTime;
		anOut.format("%.1f%%\t", fraction * 100.0d);

		// Print percentage of time closed
		fraction = closedSimTime/totalSimTime;
		anOut.format("%.1f%%\t", fraction * 100.0d);
	}
}
