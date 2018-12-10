/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018 JaamSim Software Inc.
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
import java.util.HashMap;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.LineEntity;
import com.jaamsim.Graphics.PolylineInfo;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.TimeUnit;

/**
 * Moves one or more Entities along a path with a specified travel time. Entities can have different travel times, which
 * are represented as varying speeds.
 */
public class EntityDelay extends LinkedComponent implements LineEntity {

	@Keyword(description = "The delay time for the path.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput duration;

	@Keyword(description = "If TRUE, an entity can pass a second entity that started ahead of it. "
	                     + "If FALSE, the entity's duration is increased sufficiently for it to "
	                     + "arrive no earlier than the previous entity.",
	         exampleList = {"TRUE"})
	private final BooleanInput allowOvertaking;

	@Keyword(description = "The minimum time between the previous entity leaving the path and "
	                     + "the present entity leaving the path. "
	                     + "Applicable only when entities are prevented from overtaking.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput minSeparation;

	@Keyword(description = "If TRUE, a delayed entity is moved along the specified path to "
	                     + "indicate its progression through the delay.",
	         exampleList = {"TRUE"})
	private final BooleanInput animation;

	@Keyword(description = "The width of the path in pixels.",
	         exampleList = {"1"})
	private final IntegerInput widthInput;

	@Keyword(description = "The colour of the path.",
	         exampleList = {"red"})
	private final ColourInput colorInput;

	private double exitTime;  // time at which the previous entity will leave the path
	private final HashMap<Long, EntityDelayEntry> entityMap = new HashMap<>();  // List of the entities being handled

	{
		stateGraphics.setHidden(false);

		duration = new SampleInput("Duration", KEY_INPUTS, null);
		duration.setUnitType(TimeUnit.class);
		duration.setEntity(this);
		duration.setValidRange(0, Double.POSITIVE_INFINITY);
		duration.setRequired(true);
		this.addInput(duration);

		allowOvertaking = new BooleanInput("AllowOvertaking", KEY_INPUTS, true);
		this.addInput(allowOvertaking);

		minSeparation = new SampleInput("MinSeparation", KEY_INPUTS, new SampleConstant(0.0d));
		minSeparation.setUnitType(TimeUnit.class);
		minSeparation.setEntity(this);
		minSeparation.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(minSeparation);

		animation = new BooleanInput("Animation", FORMAT, true);
		this.addInput(animation);

		widthInput = new IntegerInput("LineWidth", FORMAT, 1);
		widthInput.setValidRange(1, Integer.MAX_VALUE);
		widthInput.setDefaultText("PolylineModel");
		this.addInput(widthInput);
		this.addSynonym(widthInput, "Width");

		colorInput = new ColourInput("LineColour", FORMAT, ColourInput.BLACK);
		colorInput.setDefaultText("PolylineModel");
		this.addInput(colorInput);
		this.addSynonym(colorInput, "Colour");
		this.addSynonym(colorInput, "Color");
	}

	public EntityDelay() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput( in );

		// If animation is turned off, clear the list of entities to be displayed
		if (in == animation) {
			if (!animation.getValue())
				entityMap.clear();
			return;
		}

		// If Points were input, then use them to set the start and end coordinates
		if (in == pointsInput || in == colorInput || in == widthInput) {
			invalidateScreenPoints();
			return;
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		exitTime = Double.NEGATIVE_INFINITY;
		entityMap.clear();
	}

	@Override
	public String getInitialState() {
		return "Idle";
	}

	private static class EntityDelayEntry {
		DisplayEntity ent;
		double startTime;
		double duration;
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);

		// Select the delay time for this entity
		double simTime = this.getSimTime();
		double dur = duration.getValue().getNextSample(simTime);

		// Adjust the duration for the previous entity's exit time
		if (!allowOvertaking.getValue()) {
			double sep = minSeparation.getValue().getNextSample(simTime);
			dur = Math.max(dur, exitTime - simTime + sep);
			exitTime = simTime + dur;
		}

		// Add the entity to the list of entities being delayed
		if (animation.getValue()) {
			EntityDelayEntry entry = new EntityDelayEntry();
			entry.ent = ent;
			entry.startTime = simTime;
			entry.duration = dur;
			entityMap.put(ent.getEntityNumber(), entry);
		}
		else {
			ent.setGlobalPosition(this.getGlobalPosition());
		}

		scheduleProcess(dur, 5, true, new RemoveDisplayEntityTarget(this, ent), null); // FIFO

		// Set the present state to Working
		this.setPresentState();
	}

	private static class RemoveDisplayEntityTarget extends EntityTarget<EntityDelay> {
		private final DisplayEntity delayedEnt;

		RemoveDisplayEntityTarget(EntityDelay d, DisplayEntity e) {
			super(d, "removeDisplayEntity");
			delayedEnt = e;
		}

		@Override
		public void process() {
			ent.removeDisplayEntity(delayedEnt);
		}
	}

	public void removeDisplayEntity(DisplayEntity ent) {

		// Remove the entity from the lists
		if (animation.getValue())
			entityMap.remove(ent.getEntityNumber());

		// Send the entity to the next component
		this.sendToNextComponent(ent);
		this.setPresentState();
	}

	public void setPresentState() {
		if (this.getNumberInProgress() > 0) {
			this.setPresentState("Working");
		}
		else {
			this.setPresentState("Idle");
		}
	}

	@Override
	public boolean isOutlined() {
		return true;
	}

	@Override
	public int getLineWidth() {
		return widthInput.getValue();
	}

	@Override
	public Color4d getLineColour() {
		return colorInput.getValue();
	}

	@Override
	public void updateGraphics(double simTime) {

		if (!usePointsInput())
			return;

		// Loop through the entities on the path
		for (EntityDelayEntry entry : entityMap.values()) {
			// Calculate the distance travelled by this entity
			double frac = ( simTime - entry.startTime ) / entry.duration;

			// Set the position for the entity
			Vec3d localPos = PolylineInfo.getPositionOnPolyline(getCurvePoints(), frac);
			entry.ent.setGlobalPosition(this.getGlobalPosition(localPos));
		}
	}

	@Override
	public PolylineInfo[] buildScreenPoints(double simTime) {
		int wid = -1;
		if (!widthInput.isDefault())
			wid = Math.max(1, widthInput.getValue());

		Color4d col = null;
		if (!colorInput.isDefault())
			col = colorInput.getValue();

		PolylineInfo[] ret = new PolylineInfo[1];
		ret[0] = new PolylineInfo(getCurvePoints(), col, wid);
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
