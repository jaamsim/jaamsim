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
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.PolylineInfo;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
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

	@Keyword(description = "The width of the Arrow line segments in pixels.",
	         exampleList = {"1"})
	private final ValueInput widthInput;

	@Keyword(description = "The colour of the arrow, defined using a colour keyword or RGB values.",
	         exampleList = {"red"})
	private final ColourInput colorInput;

	private final ArrayList<DisplayEntity> entityList;  // List of the entities being conveyed
	private final ArrayList<Double> startTimeList;  // List of times at which the entities entered the conveyor
	private double totalLength;  // Graphical length of the conveyor
	private final ArrayList<Double> lengthList;  // Length of each segment of the conveyor
	private final ArrayList<Double> cumLengthList;  // Total length to the end of each segment
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
		entityList = new ArrayList<>();
		startTimeList = new ArrayList<>();
		lengthList = new ArrayList<>();
		cumLengthList = new ArrayList<>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		presentTravelTime = travelTimeInput.getValue().getNextSample(0.0);

		entityList.clear();
		startTimeList.clear();

	    // Initialize the segment length data
		lengthList.clear();
		cumLengthList.clear();
		totalLength = 0.0;
		for (int i = 1; i < pointsInput.getValue().size(); i++) {
			// Get length between points
			Vec3d vec = new Vec3d();
			vec.sub3(pointsInput.getValue().get(i), pointsInput.getValue().get(i-1));
			double length = vec.mag3();

			lengthList.add(length);
			totalLength += length;
			cumLengthList.add(totalLength);
		}
	}

	@Override
	public void addEntity(DisplayEntity ent ) {
		super.addEntity(ent);

		// Update the travel time
		double simTime = this.getSimTime();
		this.updateTravelTime(simTime);

		// Add the entity to the conveyor
		entityList.add(ent);
		startTimeList.add(simTime);

		// If necessary, wake up the conveyor
		if (this.isIdle()) {
			this.startAction();
		}
	}

	@Override
	protected boolean startProcessing(double simTime) {
		return !entityList.isEmpty();
	}

	@Override
	protected void endProcessing(double simTime) {

		// Remove the entity from the conveyor
		DisplayEntity ent = entityList.remove(0);
		startTimeList.remove(0);

		// Update the travel time
		this.updateTravelTime(simTime);

		// Send the entity to the next component
		this.sendToNextComponent(ent);
	}

	@Override
	protected double getProcessingTime(double simTime) {

		// Calculate time for the next entity to reach the end of the conveyor
		double dur = startTimeList.get(0) + presentTravelTime - simTime;
		dur = Math.max(dur, 0);  // Round-off to the nearest tick can cause a negative value
		return dur;
	}

	@Override
	protected boolean updateForStoppage(double startWork, double stopWork, double resumeWork) {

		// Adjust the start time for each entity to account for the delay
		double stopDur = resumeWork - stopWork;
		for (int i = 0; i < entityList.size(); i++) {
			double t = Math.min(stopWork, startTimeList.get(i));
			startTimeList.set(i, t + stopDur);
		}
		return false;
	}

	private void updateTravelTime(double simTime) {

		// Has the travel time changed?
		double newTime = travelTimeInput.getValue().getNextSample(simTime);
		if (newTime != presentTravelTime) {

			// Adjust the start time for each entity to account for the new travel time
			double factor = newTime/presentTravelTime;
			for (int i = 0; i < entityList.size(); i++) {
				double dt = simTime - startTimeList.get(i);
				startTimeList.set(i, simTime - dt*factor);
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

	/**
	 * Return the position coordinates for a given distance along the conveyor.
	 * @param dist = distance along the conveyor.
	 * @return position coordinates
	 */
	private Vec3d getPositionForDistance(double dist) {

		// Find the present segment
		int seg = 0;
		for (int i = 0; i < cumLengthList.size(); i++) {
			if (dist <= cumLengthList.get(i)) {
				seg = i;
				break;
			}
		}

		// Interpolate between the start and end of the segment
		double frac = 0.0;
		if (seg == 0) {
			frac = dist / lengthList.get(0);
		}
		else {
			frac = ( dist - cumLengthList.get(seg-1) ) / lengthList.get(seg);
		}
		if (frac < 0.0)  frac = 0.0;
		else if (frac > 1.0)  frac = 1.0;

		Vec3d vec = new Vec3d();
		vec.interpolate3(pointsInput.getValue().get(seg), pointsInput.getValue().get(seg+1), frac);
		return vec;
	}

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

		double t = simTime;
		if (!this.isBusy())
			t = this.getStopWorkTime();

		// Loop through the entities on the conveyor
		for (int i = 0; i < entityList.size(); i++) {
			DisplayEntity each = entityList.get(i);

			// Calculate the distance travelled by this entity
			double dur = Math.max(0.0, t - startTimeList.get(i));
			double dist = dur / presentTravelTime * totalLength;

			// 0/0 NaNs have been spotted here, just zero them
			if (Double.isNaN(dist))
				dist = 0.0;

			// Set the position for the entity
			Vec3d localPos = this.getPositionForDistance(dist);
			each.setGlobalPosition(this.getGlobalPosition(localPos));
		}
	}

	@Override
	public PolylineInfo[] buildScreenPoints() {
		int w = Math.max(1, widthInput.getValue().intValue());
		PolylineInfo[] ret = new PolylineInfo[1];
		ret[0] = new PolylineInfo(pointsInput.getValue(), colorInput.getValue(), w);
		return ret;
	}
}
