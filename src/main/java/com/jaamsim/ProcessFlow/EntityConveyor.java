/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2023 JaamSim Software Inc.
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
package com.jaamsim.ProcessFlow;

import java.util.ArrayList;

import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.ColourProviders.ColourProvInput;
import com.jaamsim.DisplayModels.PolylineModel;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.LineEntity;
import com.jaamsim.Graphics.PolylineInfo;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.SubModels.CompoundEntity;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.TimeUnit;

/**
 * Moves one or more Entities along a path at a constant speed.
 */
public class EntityConveyor extends LinkedService implements LineEntity {

	@Keyword(description = "The travel time for the conveyor.",
	         exampleList = {"10.0 s"})
	private final SampleInput travelTimeInput;

	@Keyword(description = "Length of the conveyor.",
	         exampleList = {"10.0 m"})
	private final SampleInput length;

	@Keyword(description = "Unused distance along the conveyor that is required to add the "
	                     + "present entity.",
	         exampleList = {"1.0 m"})
	private final SampleInput entitySpace;

	@Keyword(description = "Distance along the conveyor that is occupied by the present entity "
	                     + "when it is accumulated.",
	         exampleList = {"1.0 m"})
	private final SampleInput accumulationLength;

	@Keyword(description = "Specifies whether the conveyor is accumulating (TRUE) or "
	                     + "non-accumulating (FALSE). "
	                     + "This property determines the conveyor's behaviour when its exit is "
	                     + "blocked. "
	                     + "If accumulating, the conveyor will continue running until all the "
	                     + "entities are bunched together at the end. "
	                     + "If non-accumulating, the conveyor will stop when the first entity "
	                     + "reaches the end and cannot exit.")
	private final BooleanProvInput accumulating;

	@Keyword(description = "Maximum number of objects that can be moved by the conveyor at one "
	                     + "time. "
	                     + "An error message is generated if this limit is exceeded.\n\n"
	                     + "This input is intended to trap a model error that causes the number "
	                     + "of objects to grow without bound. "
	                     + "It has no effect on model logic.",
	         exampleList = {"100"})
	protected final SampleInput maxValidNumber;

	@Keyword(description = "If TRUE, the entities are rotated to match the direction of "
	                     + "the path.")
	private final BooleanProvInput rotateEntities;

	@Keyword(description = "The width of the conveyor in pixels.",
	         exampleList = {"1"})
	private final SampleInput widthInput;

	@Keyword(description = "The colour of the conveyor.")
	private final ColourProvInput colorInput;

	private final ArrayList<ConveyorEntry> entryList;  // List of the entities being conveyed
	private double presentTravelTime;
	private double nextDuration;
	private boolean readyForNext;

	private boolean exitFlag;
	private boolean nextEntFlag;

	{
		displayModelListInput.clearValidClasses();
		displayModelListInput.addValidClass(PolylineModel.class);

		releaseThresholdList.setHidden(false);
		operatingThresholdList.setHidden(true);
		waitQueue.setHidden(true);
		match.setHidden(true);
		watchList.setHidden(true);
		processPosition.setHidden(true);
		forcedMaintenanceList.setHidden(true);
		forcedBreakdownList.setHidden(true);
		selectionCondition.setHidden(true);
		nextEntity.setHidden(true);

		travelTimeInput = new SampleInput("TravelTime", KEY_INPUTS, 0.0d);
		travelTimeInput.setValidRange(0.0, Double.POSITIVE_INFINITY);
		travelTimeInput.setUnitType(TimeUnit.class);
		this.addInput(travelTimeInput);

		length = new SampleInput("Length", KEY_INPUTS, 0.0d);
		length.setValidRange(0.0, Double.POSITIVE_INFINITY);
		length.setUnitType(DistanceUnit.class);
		this.addInput(length);

		entitySpace = new SampleInput("EntitySpace", KEY_INPUTS, 0.0d);
		entitySpace.setValidRange(0.0, Double.POSITIVE_INFINITY);
		entitySpace.setUnitType(DistanceUnit.class);
		this.addInput(entitySpace);

		accumulationLength = new SampleInput("AccumulationLength", KEY_INPUTS, 0.0d);
		accumulationLength.setValidRange(0.0, Double.POSITIVE_INFINITY);
		accumulationLength.setUnitType(DistanceUnit.class);
		this.addInput(accumulationLength);

		accumulating = new BooleanProvInput("Accumulating", KEY_INPUTS, false);
		this.addInput(accumulating);

		maxValidNumber = new SampleInput("MaxValidNumber", KEY_INPUTS, 10000);
		maxValidNumber.setValidRange(0, Double.POSITIVE_INFINITY);
		maxValidNumber.setIntegerValue(true);
		this.addInput(maxValidNumber);

		rotateEntities = new BooleanProvInput("RotateEntities", FORMAT, false);
		this.addInput(rotateEntities);

		widthInput = new SampleInput("LineWidth", FORMAT, 1);
		widthInput.setValidRange(1, Double.POSITIVE_INFINITY);
		widthInput.setIntegerValue(true);
		widthInput.setDefaultText("PolylineModel");
		this.addInput(widthInput);
		this.addSynonym(widthInput, "Width");

		colorInput = new ColourProvInput("LineColour", FORMAT, ColourInput.BLACK);
		colorInput.setDefaultText("PolylineModel");
		this.addInput(colorInput);
		this.addSynonym(colorInput, "Colour");
		this.addSynonym(colorInput, "Color");
	}

	public EntityConveyor() {
		entryList = new ArrayList<>();
	}

	@Override
	public void validate() {
		super.validate();
		if (isAccumulating() && length.getNextSample(this, 0.0d) <= 0.0d)
			throw new InputErrorException("A non-zero 'Length' input must be specified when the "
					+ "'Accumulating' input is TRUE");
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		entryList.clear();
		presentTravelTime = 0.0d;
		readyForNext = true;
	}

	@Override
	public void startUp() {
		super.startUp();
		presentTravelTime = travelTimeInput.getNextSample(this, 0.0);
	}

	public boolean isAccumulating() {
		return accumulating.getNextBoolean(this, 0.0d);
	}

	public boolean isRotateEntities(double simTime) {
		return rotateEntities.getNextBoolean(this, simTime);
	}

	private static class ConveyorEntry {
		final DisplayEntity entity;
		double length;
		double position;

		public ConveyorEntry(DisplayEntity ent, double lgth, double pos) {
			entity = ent;
			length = lgth;
			position = pos;
		}

		@Override
		public String toString() {
			return String.format("(%s, %.6f, %.6f)", entity, length, position);
		}
	}

	@Override
	public void addEntity(DisplayEntity ent ) {
		super.addEntity(ent);
		double simTime = this.getSimTime();

		// Update the positions of the entities on the conveyor
		this.updateProgress();

		// Update the travel time
		this.updateTravelTime(simTime);

		// Add the entity to the conveyor
		double convLength = length.getNextSample(this, simTime);
		double reqdLength = entitySpace.getNextSample(this, simTime);
		double entLength = accumulationLength.getNextSample(this, simTime);
		double position = 0.0d;
		if (!entryList.isEmpty() && convLength > 0.0d) {
			position = entryList.get(entryList.size() - 1).position - reqdLength/convLength;
			position = Math.min(position, 0.0d);
		}
		ConveyorEntry entry = new ConveyorEntry(ent, entLength, position);
		entryList.add(entry);

		int maxNumber = (int) maxValidNumber.getNextSample(this, simTime);
		if (entryList.size() > maxNumber)
			error("Number of objects on the conveyor exceeds the limit of %s set by the "
					+ "'MaxValidNumber' input.", maxNumber);

		readyForNext = (position * convLength >= reqdLength);

		// Assign attributes
		assignAttributesAtStart(simTime);

		// Notify any observers
		notifyObservers();

		// If necessary, wake up the conveyor
		performUnscheduledUpdate();
	}

	@Override
	protected boolean startProcessing(double simTime) {
		if (entryList.isEmpty()) {
			readyForNext = true;
			return false;
		}

		double convLength = length.getNextSample(this, simTime);
		double reqdLength = entitySpace.getNextSample(this, simTime);

		long nextEntTicks = Long.MAX_VALUE;
		long exitTicks = Long.MAX_VALUE;
		long accumTicks = Long.MAX_VALUE;
		EventManager evt = EventManager.current();

		// Time for the conveyor to be ready for the next entity
		if (!readyForNext) {
			ConveyorEntry entry = entryList.get(entryList.size() - 1);
			double reqdPos = reqdLength/convLength;
			double reqdFrac = Math.max(reqdPos - entry.position, 0.0d);
			nextEntTicks = evt.secondsToNearestTick(reqdFrac * presentTravelTime);
		}

		// Time for the last entity to accumulate at the end of the conveyor
		if (isAccumulating() && isReleaseThresholdClosure()) {
			double maxPos = 1.0d;
			ConveyorEntry lastEntry = null;
			for (ConveyorEntry entry : entryList) {
				if (lastEntry != null)
					maxPos -= entry.length/convLength;
				lastEntry = entry;
			}
			double reqdFrac = Math.max(maxPos - lastEntry.position, 0.0d);
			long ticks = evt.secondsToNearestTick(reqdFrac * presentTravelTime);
			if (ticks > 0L)
				accumTicks = ticks;

			// Ensure that there is room for the next entity to be added
			if (reqdLength/convLength > maxPos)
				nextEntTicks = Long.MAX_VALUE;
		}

		// Time for the first entity to reach the end of the conveyor
		else {
			double reqdFrac = Math.max(1.0d - entryList.get(0).position, 0.0d);
			exitTicks = evt.secondsToNearestTick(reqdFrac * presentTravelTime);
		}

		// Determine the type of event to occur at the end of the time step
		long durTicks = Math.min(nextEntTicks, Math.min(exitTicks, accumTicks));
		exitFlag = (exitTicks == durTicks);
		nextEntFlag = (nextEntTicks == durTicks);
		nextDuration = evt.ticksToSeconds(durTicks);

		if (isTraceFlag()) {
			traceLine(2, "nextEntTicks=%s, exitTicks=%s, accumTicks=%s",
					nextEntTicks, exitTicks, accumTicks);
			traceLine(2, "nextEntFlag=%s, exitFlag=%s", nextEntFlag, exitFlag);
		}

		return durTicks != Long.MAX_VALUE;
	}

	@Override
	protected void processStep(double simTime) {

		// Release the entity at the exit of the conveyor
		if (exitFlag) {
			if (isReleaseThresholdClosure()) {
				setReadyToRelease(true);
			}
			else {
				ConveyorEntry entry = entryList.remove(0);
				DisplayEntity ent = entry.entity;
				sendToNextComponent(ent);
			}
		}

		// Allow the next entity to be added to the conveyor
		if (nextEntFlag) {
			readyForNext = true;
		}

		// Reset all flags
		exitFlag = false;
		nextEntFlag = false;

		// Update the travel time
		this.updateTravelTime(simTime);
	}

	@Override
	protected double getStepDuration(double simTime) {
		return nextDuration;
	}

	@Override
	protected boolean isNewStepReqd(boolean completed) {
		return true;
	}

	@Override
	public void updateProgress(double dt) {

		if (presentTravelTime == 0.0d)
			return;

		// Calculate the fractional distance travelled since the last update
		double frac = dt/presentTravelTime;
		if (MathUtils.near(frac, 0.0d))
			return;

		// Increment the positions of the entities on the conveyor
		if (isTraceFlag()) traceLine(2, "BEFORE - entryList=%s", entryList);

		ConveyorEntry lastEntry = null;
		double convLength = length.getNextSample(this, getSimTime());
		double maxPos = 1.0d;
		for (ConveyorEntry entry : entryList) {
			if (lastEntry != null && convLength > 0.0d)
				maxPos = lastEntry.position - entry.length/convLength;
			entry.position = Math.min(entry.position + frac, maxPos);
			lastEntry = entry;
		}

		if (isTraceFlag()) traceLine(2, "AFTER - entryList=%s", entryList);
	}

	private void updateTravelTime(double simTime) {

		// Has the travel time changed?
		double newTime = travelTimeInput.getNextSample(this, simTime);
		if (newTime != presentTravelTime) {

			if (isTraceFlag()) {
				trace(1, "updateTravelTime");
				traceLine(2, "newTime=%.6f, presentTravelTime=%.6f", newTime, presentTravelTime);
			}

			// Set the new travel time
			presentTravelTime = newTime;

			// Adjust the time at which the next entity will reach the end of the conveyor
			// (required when an entity is added to a conveyor that already has entities in flight)
			this.resetProcess();
		}
	}

	@Override
	public void thresholdChanged() {
		if (isImmediateReleaseThresholdClosure()) {
			for (ConveyorEntry entry : entryList) {
				entry.position = 1.0d;
			}
		}
		if (isBusy() && isAccumulating()) {
			performUnscheduledUpdate();
			return;
		}
		super.thresholdChanged();
	}

	@Override
	public boolean isFinished() {
		return entryList.isEmpty();
	}

	@Override
	public boolean isStopped() {
		return isImmediateThresholdClosure() || isImmediateReleaseThresholdClosure()
				|| (isOperatingThresholdClosure() && isFinished())
				|| (isReleaseThresholdClosure() && isReadyToRelease() && !isAccumulating());
	}

	@Override
	public void setPresentState() {
		if (isIdle() && !entryList.isEmpty()) {
			setPresentState(STATE_BLOCKED);
			return;
		}
		super.setPresentState();
	}

	// ********************************************************************************************
	// GRAPHICS
	// ********************************************************************************************

	@Override
	public boolean isOutlined(double simTime) {
		return true;
	}

	@Override
	public int getLineWidth(double simTime) {
		if (widthInput.isDefault()) {
			LineEntity model = getDisplayModel(LineEntity.class);
			if (model != null)
				return model.getLineWidth(simTime);
		}
		return (int) widthInput.getNextSample(this, simTime);
	}

	@Override
	public Color4d getLineColour(double simTime) {
		if (colorInput.isDefault()) {
			LineEntity model = getDisplayModel(LineEntity.class);
			if (model != null)
				return model.getLineColour(simTime);
		}
		return colorInput.getNextColour(this, simTime);
	}

	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);

		if (presentTravelTime == 0.0d || !usePointsInput())
			return;

		// Copy the list to avoid concurrent modification exceptions
		ArrayList<ConveyorEntry> copiedList;
		try {
			copiedList = new ArrayList<>(entryList);
		}
		catch (Exception e) {
			return;
		}

		// If the conveyor is not visible show the entities at the sub-model's process position
		if (!getShow() && getVisibleParent() instanceof CompoundEntity) {
			CompoundEntity ce = (CompoundEntity) getVisibleParent();
			for (ConveyorEntry entry : copiedList) {
				entry.entity.moveToProcessPosition(ce, ce.getProcessPosition());
			}
			return;
		}

		// Move each entity on the conveyor to its present position
		double frac = 0.0d;
		if (isBusy()) {
			frac = (simTime - this.getLastUpdateTime())/presentTravelTime;
		}
		double convLength = length.getNextSample(this, simTime);
		ConveyorEntry lastEntry = null;
		double lastPos = 0.0d;
		for (ConveyorEntry entry : copiedList) {

			entry.entity.setRegion(this.getCurrentRegion());

			double maxPos = 1.0d;
			if (lastEntry != null && convLength > 0.0d)
				maxPos = lastPos - entry.length/convLength;
			double convPos = Math.min(entry.position + frac, maxPos);
			lastPos = convPos;
			lastEntry = entry;

			convPos = Math.max(convPos, 0.0d);
			Vec3d localPos = PolylineInfo.getPositionOnPolyline(getCurvePoints(), convPos);
			Vec3d alignment = entry.entity.getAlignment();
			alignment.x = -0.5d;
			entry.entity.setGlobalPositionForAlignment(getGlobalPosition(localPos), alignment);

			if (isRotateEntities(simTime)) {
				Vec3d orient = PolylineInfo.getOrientationOnPolyline(getCurvePoints(), convPos);
				entry.entity.setRelativeOrientation(orient);
			}
		}
	}

	@Output(name = "EntityList",
	 description = "The entities being processed at present.",
	    sequence = 1)
	public ArrayList<DisplayEntity> getEntityList(double simTime) {
		ArrayList<DisplayEntity> ret = new ArrayList<>(entryList.size());
		for (ConveyorEntry entry : entryList) {
			ret.add(entry.entity);
		}
		return ret;
	}

	@Output(name = "ReadyForNextEntity",
	 description = "Returns true if there is enough space on the conveyor to accept the next "
	             + "entity.",
	    sequence = 2)
	public boolean readyForNextEntity(double simTime) {
		return readyForNext;
	}

}
