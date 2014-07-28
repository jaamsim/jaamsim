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
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Color4d;
import com.jaamsim.states.StateEntity;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.DimensionlessUnit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityTarget;
import com.sandwell.JavaSimulation3D.DisplayModelCompat;

public class Threshold extends StateEntity {

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

		userList.clear();
		for (Entity each : Entity.getAll()) {
			if (each instanceof ThresholdUser) {
				ThresholdUser tu = (ThresholdUser)each;
				if (tu.getThresholds().contains(this))
					userList.add(tu);
			}
		}
	}

	/**
	 * Get the name of the initial state this Entity will be initialized with.
	 * @return
	 */
	@Override
	public String getInitialState() {
		return "Open";
	}

	/**
	 * Tests the given state name to see if it is valid for this Entity.
	 * @param state
	 * @return
	 */
	@Override
	public boolean isValidState(String state) {
		return "Open".equals(state) || "Closed".equals(state);
	}

	/**
	 * Tests the given state name to see if it is counted as working hours when in
	 * that state..
	 * @param state
	 * @return
	 */
	@Override
	public boolean isValidWorkingState(String state) {
		return "Open".equals(state);
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

	public final void setOpen(boolean open) {
		// If setting to the same value as current, return
		if (this.open == open)
			return;

		this.open = open;
		if (this.open)
			setPresentState("Open");
		else
			setPresentState("Closed");

		for (ThresholdUser user : this.userList) {
			if (!userUpdate.users.contains(user))
				userUpdate.users.add(user);
		}
		if (!userUpdate.users.isEmpty() && !updateHandle.isScheduled())
			this.scheduleProcessTicks(0, 2, false, userUpdate, updateHandle);
	}

	private static class DoOpenTarget extends EntityTarget<Threshold> {
		public DoOpenTarget(Threshold ent, String method) {
			super(ent, method);
		}

		@Override
		public void process() {
			ent.doOpen();
		}
	}

	public final ProcessTarget doOpen = new DoOpenTarget(this, "doOpen");

	public void doOpen() {
		this.trace( "open" );
		this.setOpen( true );
	}

	private static class DoCloseTarget extends EntityTarget<Threshold> {
		public DoCloseTarget(Threshold ent, String method) {
			super(ent, method);
		}

		@Override
		public void process() {
			ent.doClose();
		}
	}

	public final ProcessTarget doClose = new DoCloseTarget(this, "doClose");

	public void doClose() {
		this.trace( "close" );
		this.setOpen( false );
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
		long simTicks = FrameBox.secondsToTicks(simTime);
		long openTicks = this.getTicksInState(simTicks, getState("Open"));
		long closedTicks = this.getTicksInState(simTicks, getState("Closed"));
		long totTicks = openTicks + closedTicks;

		return (double)openTicks / totTicks;
	}

	@Output(name = "ClosedFraction",
	 description = "The fraction of total simulation time that the threshold is closed.",
	    unitType = DimensionlessUnit.class)
	public double getClosedFraction(double simTime) {
		long simTicks = FrameBox.secondsToTicks(simTime);
		long openTicks = this.getTicksInState(simTicks, getState("Open"));
		long closedTicks = this.getTicksInState(simTicks, getState("Closed"));
		long totTicks = openTicks + closedTicks;

		return (double)closedTicks / totTicks;
	}
}
