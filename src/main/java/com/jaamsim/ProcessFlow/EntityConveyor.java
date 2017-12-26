/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.PolylineInfo;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

/**
 * Moves one or more Entities along a path at a constant speed.
 */
public class EntityConveyor extends LinkedService {

	@Keyword(description = "The travel time for the conveyor.",
	         exampleList = {"10.0 s"})
	private final SampleInput travelTimeInput;

	@Keyword(description = "The width of the conveyor in pixels.",
	         exampleList = {"1"})
	private final ValueInput widthInput;

	@Keyword(description = "The colour of the conveyor.",
	         exampleList = {"red"})
	private final ColourInput colorInput;

	private final ArrayList<ConveyorEntry> entryList;  // List of the entities being conveyed
	private double presentTravelTime;

	{
		operatingThresholdList.setHidden(true);
		waitQueue.setHidden(true);
		match.setHidden(true);
		processPosition.setHidden(true);
		forcedMaintenanceList.setHidden(true);
		forcedBreakdownList.setHidden(true);

		travelTimeInput = new SampleInput("TravelTime", "Key Inputs", new SampleConstant(0.0d));
		travelTimeInput.setValidRange(0.0, Double.POSITIVE_INFINITY);
		travelTimeInput.setUnitType(TimeUnit.class);
		travelTimeInput.setEntity(this);
		this.addInput(travelTimeInput);

		widthInput = new ValueInput("Width", "Key Inputs", 1.0d);
		widthInput.setUnitType(DimensionlessUnit.class);
		widthInput.setValidRange(1.0d, Double.POSITIVE_INFINITY);
		this.addInput(widthInput);

		colorInput = new ColourInput("Color", "Key Inputs", ColourInput.BLACK);
		this.addInput(colorInput);
		this.addSynonym(colorInput, "Colour");
	}

	public EntityConveyor() {
		entryList = new ArrayList<>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		entryList.clear();
		presentTravelTime = 0.0d;
	}

	@Override
	public void startUp() {
		super.startUp();
		presentTravelTime = travelTimeInput.getValue().getNextSample(0.0);
	}

	private static class ConveyorEntry {
		final DisplayEntity entity;
		double position;

		public ConveyorEntry(DisplayEntity ent, double pos) {
			entity = ent;
			position = pos;
		}

		@Override
		public String toString() {
			return String.format("(%s, %.6f)", entity, position);
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
		ConveyorEntry entry = new ConveyorEntry(ent, 0.0d);
		entryList.add(entry);

		// If necessary, wake up the conveyor
		this.startStep();
	}

	@Override
	protected boolean startProcessing(double simTime) {
		return !entryList.isEmpty();
	}

	@Override
	protected boolean processStep(double simTime) {

		// Remove the entity from the conveyor
		DisplayEntity ent = entryList.remove(0).entity;

		// Update the travel time
		this.updateTravelTime(simTime);

		// Send the entity to the next component
		this.sendToNextComponent(ent);

		return true;
	}

	@Override
	protected double getStepDuration(double simTime) {

		// Calculate the time for the first entity to reach the end of the conveyor
		double dt = simTime - this.getLastUpdateTime();
		double dur = (1.0d - entryList.get(0).position)*presentTravelTime - dt;
		dur = Math.max(dur, 0);  // Round-off to the nearest tick can cause a negative value
		if (isTraceFlag()) trace(1, "getProcessingTime = %.6f", dur);
		return dur;
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
		for (ConveyorEntry entry : entryList) {
			entry.position += frac;
		}
		if (isTraceFlag()) traceLine(2, "AFTER - entryList=%s", entryList);
	}

	private void updateTravelTime(double simTime) {

		// Has the travel time changed?
		double newTime = travelTimeInput.getValue().getNextSample(simTime);
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

	// ********************************************************************************************
	// GRAPHICS
	// ********************************************************************************************

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		// If Points were input, then use them to set the start and end coordinates
		if (in == pointsInput || in == colorInput || in == widthInput) {
			invalidateScreenPoints();
			return;
		}
	}

	@Override
	public void updateGraphics(double simTime) {

		if (presentTravelTime == 0.0d || !usePointsInput())
			return;

		// Move each entity on the conveyor to its present position
		double frac = 0.0d;
		if (isBusy()) {
			frac = (simTime - this.getLastUpdateTime())/presentTravelTime;
		}
		for (int i=0; i<entryList.size(); i++) {
			ConveyorEntry entry = entryList.get(i);
			Vec3d localPos = PolylineInfo.getPositionOnPolyline(getCurvePoints(), entry.position + frac);
			entry.entity.setGlobalPosition(this.getGlobalPosition(localPos));
		}
	}

	@Override
	public PolylineInfo[] buildScreenPoints(double simTime) {
		int w = Math.max(1, widthInput.getValue().intValue());
		PolylineInfo[] ret = new PolylineInfo[1];
		ret[0] = new PolylineInfo(getCurvePoints(), colorInput.getValue(), w);
		return ret;
	}

	// LinkDisplayable overrides
	@Override
	public Vec3d getSourcePoint() {
		ArrayList<Vec3d> points = pointsInput.getValue();
		if (points.size() == 0) {
			return getGlobalPosition();
		}
		return new Vec3d(points.get(points.size()-1));
	}
	@Override
	public Vec3d getSinkPoint() {
		ArrayList<Vec3d> points = pointsInput.getValue();
		if (points.size() == 0) {
			return getGlobalPosition();
		}
		return new Vec3d(points.get(0));
	}

	@Override
	public double getRadius() {
		return 0.2; // TODO: make this a tunable parameter
	}

}
