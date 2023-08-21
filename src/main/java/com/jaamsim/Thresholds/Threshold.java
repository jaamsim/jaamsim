/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2023 JaamSim Software Inc.
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

import java.util.ArrayList;

import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.ColourProviders.ColourProvInput;
import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ObserverEntity;
import com.jaamsim.basicsim.SubjectEntity;
import com.jaamsim.basicsim.SubjectEntityDelegate;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Color4d;
import com.jaamsim.states.StateEntity;
import com.jaamsim.units.DimensionlessUnit;

public class Threshold extends StateEntity implements SubjectEntity {

	@Keyword(description = "The colour of the threshold graphic when the threshold is open.")
	private final ColourProvInput openColour;

	@Keyword(description = "The colour of the threshold graphic when the threshold is closed.")
	private final ColourProvInput closedColour;

	@Keyword(description = "A Boolean value.  If TRUE, the threshold is displayed when it is open.")
	private final BooleanProvInput showWhenOpen;

	@Keyword(description = "A Boolean value.  If TRUE, the threshold is displayed when it is closed.")
	private final BooleanProvInput showWhenClosed;

	private final ArrayList<ThresholdUser> userList;
	private boolean open;
	private long openCount;
	private long closedCount;

	private static final String STATE_OPEN = "Open";
	private static final String STATE_CLOSED = "Closed";

	private final SubjectEntityDelegate subject = new SubjectEntityDelegate(this);

	{
		workingStateListInput.setHidden(true);

		openColour = new ColourProvInput("OpenColour", FORMAT, ColourInput.GREEN);
		this.addInput(openColour);
		this.addSynonym(openColour, "OpenColor");

		closedColour = new ColourProvInput("ClosedColour", FORMAT, ColourInput.RED);
		this.addInput(closedColour);
		this.addSynonym(closedColour, "ClosedColor");

		showWhenOpen = new BooleanProvInput("ShowWhenOpen", FORMAT, true);
		this.addInput(showWhenOpen);

		showWhenClosed = new BooleanProvInput("ShowWhenClosed", FORMAT, true);
		this.addInput(showWhenClosed);
	}

	public Threshold() {
		userList = new ArrayList<>();
		open = true;
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		open = getInitialOpenValue();
		openCount = 0L;
		closedCount = 0L;

		userList.clear();
		for (Entity each : getJaamSimModel().getClonesOfIterator(Entity.class, ThresholdUser.class)) {
			ThresholdUser tu = (ThresholdUser) each;
			if (tu.getThresholds().contains(this))
				registerThresholdUser(tu);
		}

		// Clear the list of observers
		subject.clear();
	}

	public void registerThresholdUser(ThresholdUser tu) {
		if (!isActive() || userList.contains(tu))
			return;
		userList.add(tu);
	}

	public void unregisterThresholdUser(ThresholdUser tu) {
		userList.remove(tu);
	}

	public boolean getInitialOpenValue() {
		return true;
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

	@Override
	public String getInitialState() {
		if (getInitialOpenValue())
			return STATE_OPEN;
		else
			return STATE_CLOSED;
	}

	@Override
	public boolean isValidState(String state) {
		return STATE_OPEN.equals(state) || STATE_CLOSED.equals(state);
	}

	@Override
	public boolean isValidWorkingState(String state) {
		return STATE_OPEN.equals(state);
	}

	public boolean isOpen() {
		return open;
	}

	public final void setOpen(boolean bool) {
		// If setting to the same value as current, return
		if (open == bool)
			return;

		if (isTraceFlag()) trace(0, "setOpen(%s)", bool);

		open = bool;

		// Set the new state
		setPresentState();
		if (open) {
			openCount++;
		}
		else {
			closedCount++;
		}

		getJaamSimModel().updateThresholdUsers(userList);

		// Notify any observers
		notifyObservers();
	}

	public void setPresentState() {
		if (open) {
			setPresentState(STATE_OPEN);
		}
		else {
			setPresentState(STATE_CLOSED);
		}
	}

	@Override
	public void clearStatistics() {
		openCount = 0L;
		closedCount = 0L;
	}

	@Override
	public void updateGraphics(double simTime ) {
		super.updateGraphics(simTime);

		// Determine the colour for the square
		Color4d col;
		if (open)
			col = openColour.getNextColour(this, simTime);
		else
			col = closedColour.getNextColour(this, simTime);

		// Show or hide the threshold
		if (!showWhenOpen.isDefault() || !showWhenClosed.isDefault()) {
			if (open)
				setShow(getShowInput() && showWhenOpen.getNextBoolean(this, simTime));
			else
				setShow(getShowInput() && showWhenClosed.getNextBoolean(this, simTime));
		}

		setTagColour( ShapeModel.TAG_CONTENTS, col );
		setTagColour( ShapeModel.TAG_OUTLINES, ColourInput.BLACK );
	}

	@Output(name = "UserList",
	 description = "The objects that are stopped by this Threshold.",
	    sequence = 0)
	public ArrayList<ThresholdUser> getUserList(double simTime) {
		return userList;
	}

	@Output(name = "Open",
	 description = "If open, then return TRUE.  Otherwise, return FALSE.",
	    unitType = DimensionlessUnit.class,
	    sequence = 1)
	public Boolean getOpen(double simTime) {
		return open;
	}

	@Output(name = "OpenFraction",
	 description = "The fraction of total simulation time that the threshold is open.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 2)
	public double getOpenFraction(double simTime) {
		EventManager evt = this.getJaamSimModel().getEventManager();
		long simTicks = evt.secondsToNearestTick(simTime);
		long openTicks = this.getTicksInState(simTicks, getState(STATE_OPEN));
		long closedTicks = this.getTicksInState(simTicks, getState(STATE_CLOSED));
		long totTicks = openTicks + closedTicks;

		return (double)openTicks / totTicks;
	}

	@Output(name = "ClosedFraction",
	 description = "The fraction of total simulation time that the threshold is closed.",
	    unitType = DimensionlessUnit.class,
	    sequence = 3)
	public double getClosedFraction(double simTime) {
		EventManager evt = this.getJaamSimModel().getEventManager();
		long simTicks = evt.secondsToNearestTick(simTime);
		long openTicks = this.getTicksInState(simTicks, getState(STATE_OPEN));
		long closedTicks = this.getTicksInState(simTicks, getState(STATE_CLOSED));
		long totTicks = openTicks + closedTicks;

		return (double)closedTicks / totTicks;
	}

	@Output(name = "OpenCount",
	 description = "The number of times the threshold's state has changed from closed to open.",
	    unitType = DimensionlessUnit.class,
	    sequence = 4)
	public long getOpenCount(double simTime) {
		return openCount;
	}

	@Output(name = "ClosedCount",
	 description = "The number of times the threshold's state has changed from open to closed.",
	    unitType = DimensionlessUnit.class,
	    sequence = 5)
	public long getClosedCount(double simTime) {
		return closedCount;
	}

}
