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

import com.jaamsim.events.EventHandle;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Color4d;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.DimensionlessUnit;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.DisplayModelCompat;

public class Threshold extends DisplayEntity {

	@Keyword(description = "The colour of the threshold graphic when the threshold is open.",
	         example = "Threshold1  OpenColour { green }")
	private final ColourInput openColour;

	@Keyword(description = "The colour of the threshold graphic when the threshold is closed.",
			example = "Threshold1  ClosedColour { red }")
	private final ColourInput closedColour;

	@Keyword(description = "A Boolean value.  If TRUE, the threshold is displayed when it is open.",
	         example = "Threshold1 ShowWhenOpen { FALSE }")
	private final BooleanInput showWhenOpen;

	@Keyword(description = "A Boolean value.  If TRUE, the threshold is displayed when it is closed.",
	         example = "Threshold1 ShowWhenClosed { FALSE }")
	private final BooleanInput showWhenClosed;

	private final ArrayList<ThresholdUser> userList;

	private boolean open;

	private long lastTickUpdate;
	private long openTicks;
	private long closedTicks;

	{
		openColour = new ColourInput( "OpenColour", "Graphics", ColourInput.GREEN );
		this.addInput( openColour );
		this.addSynonym( openColour, "OpenColor" );

		closedColour = new ColourInput( "ClosedColour", "Graphics", ColourInput.RED );
		this.addInput( closedColour );
		this.addSynonym( closedColour, "ClosedColor" );

		showWhenOpen = new BooleanInput("ShowWhenOpen", "Graphics", true);
		this.addInput(showWhenOpen);

		showWhenClosed = new BooleanInput("ShowWhenClosed", "Graphics", true);
		this.addInput(showWhenClosed);
	}

	public Threshold() {
		userList = new ArrayList<ThresholdUser>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		userUpdate.users.clear();
		open = true;
		lastTickUpdate = getSimTicks();
		openTicks = 0;
		closedTicks = 0;

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
	}

	private static final EventHandle updateHandle = new EventHandle();
	private static final DoThresholdChanged userUpdate = new DoThresholdChanged();
	private static class DoThresholdChanged extends ProcessTarget {
		public final ArrayList<ThresholdUser> users = new ArrayList<ThresholdUser>();

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

	public boolean isOpen() {
		return open;
	}

	public boolean isClosed() {
		return !open;
	}

	@Override
	public void updateGraphics( double time ) {
		super.updateGraphics(time);

		// Determine the colour for the square
		Color4d col;
		if (open) {
			col = openColour.getValue();
			setTagVisibility(DisplayModelCompat.TAG_CONTENTS, showWhenOpen.getValue());
			setTagVisibility(DisplayModelCompat.TAG_OUTLINES, showWhenOpen.getValue());
		}
		else {
			col = closedColour.getValue();
			setTagVisibility(DisplayModelCompat.TAG_CONTENTS, showWhenClosed.getValue());
			setTagVisibility(DisplayModelCompat.TAG_OUTLINES, showWhenClosed.getValue());

		}

		setTagColour( DisplayModelCompat.TAG_CONTENTS, col );
		setTagColour( DisplayModelCompat.TAG_OUTLINES, ColourInput.BLACK );
	}

	// ********************************************************************************
    // Reporting
    // ********************************************************************************

	public void clearStatistics() {
		openTicks = 0;
		closedTicks = 0;
		lastTickUpdate = getSimTicks();
	}

	public final void setOpen(boolean open) {
		// If setting to the same value as current, return
		if (this.open == open)
			return;

		if (this.open) {
			openTicks += getSimTicks() - lastTickUpdate;
		}
		else {
			closedTicks += getSimTicks() - lastTickUpdate;
		}

		lastTickUpdate = getSimTicks();
		this.open = open;

		for (ThresholdUser user : this.userList) {
			if (!userUpdate.users.contains(user))
				userUpdate.users.add(user);
		}
		if (!userUpdate.users.isEmpty() && !updateHandle.isScheduled())
			this.scheduleProcessTicks(0, 2, false, userUpdate, updateHandle);
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
		long durTicks = getSimTicks() - lastTickUpdate;
		long totalSimTicks = openTicks + closedTicks + durTicks;
		if (totalSimTicks == 0)
			return;

		anOut.format( "%s\t", getName() );

		long totOpen = openTicks;
		long totClosed = closedTicks;
		if (isClosed())
			totClosed += durTicks;
		else
			totOpen += durTicks;
		// Print percentage of time open
		anOut.format("%.1f%%\t", (totOpen * 100 / (double)totalSimTicks));

		// Print percentage of time closed
		anOut.format("%.1f%%\t", (totClosed * 100 / (double)totalSimTicks));
	}

	@Output(name = "Open",
	 description = "If open, then return TRUE.  Otherwise, return FALSE.",
	    unitType = DimensionlessUnit.class)
	public Boolean getOpen(double simTime) {
		return open;
	}

	@Output(name = "OpenFraction",
	 description = "The fraction of total simulation time that the threshold is open.",
	    unitType = DimensionlessUnit.class)
	public double getOpenFraction(double simTime) {
		double dur = simTime - FrameBox.ticksToSeconds(lastTickUpdate);
		double openTime = FrameBox.ticksToSeconds(openTicks);
		double closedTime = FrameBox.ticksToSeconds(closedTicks);
		double totalTime = openTime + closedTime + dur;
		if (isOpen())
			return (openTime + dur) / totalTime;
		else
			return openTime / totalTime;
	}

	@Output(name = "ClosedFraction",
	 description = "The fraction of total simulation time that the threshold is closed.",
	    unitType = DimensionlessUnit.class)
	public double getClosedFraction(double simTime) {
		double dur = simTime - FrameBox.ticksToSeconds(lastTickUpdate);
		double openTime = FrameBox.ticksToSeconds(openTicks);
		double closedTime = FrameBox.ticksToSeconds(closedTicks);
		double totalTime = openTime + closedTime + dur;
		if (isClosed())
			return (closedTime + dur) / totalTime;
		else
			return closedTime / totalTime;
	}
}
