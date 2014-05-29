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
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Color4d;
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

	protected boolean closed;

	protected double simTimeOfLastUpdate; // Simulation time in seconds of last update
	protected double openSimTime; // Number of seconds open
	protected double closedSimTime; // Number of seconds closed

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
	}

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

	protected void scheduleChangedCallback() {
		for (ThresholdUser user : userList) {
			if (!userUpdate.users.contains(user))
				userUpdate.users.add(user);
		}
		if (!userUpdate.users.isEmpty())
			this.scheduleSingleProcess(userUpdate, 2);
	}

	protected void thresholdChangedCallback() {
		for (ThresholdUser user : userList) {
			user.thresholdChanged();
		}
	}

	public boolean isClosed() {
		return closed;
	}

	@Override
	public void updateGraphics( double time ) {
		super.updateGraphics(time);

		// Determine the colour for the square
		Color4d col;
		if( closed )
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

	@Output(name = "Open",
	 description = "If open, then return TRUE.  Otherwise, return FALSE.",
	    unitType = DimensionlessUnit.class)
	public Boolean getOpen(double simTime) {
		return !closed;
	}
}
