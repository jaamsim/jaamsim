/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
import com.jaamsim.Graphics.PolylineInfo;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

/**
 * Moves one or more Entities along a path with a specified travel time. Entities can have different travel times, which
 * are represented as varying speeds.
 */
public class EntityDelay extends LinkedComponent {

	@Keyword(description = "The delay time for the path.\n" +
			"The input can be a constant value, a time series of values, or a probability distribution to be sampled.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput duration;

	@Keyword(description = "If TRUE, a delayed entity is moved along the " +
			"specified path to indicate its progression through the delay.",
	         exampleList = {"TRUE"})
	private final BooleanInput animation;

	@Keyword(description = "The width of the path in pixels.",
	         exampleList = {"1"})
	private final ValueInput widthInput;

	@Keyword(description = "The colour of the path.\n" +
			"The input can be a colour keyword or RGB value.",
	         exampleList = {"red"})
	private final ColourInput colorInput;

	private final HashMap<Long, EntityDelayEntry> entityMap = new HashMap<>();  // List of the entities being handled

	{
		stateGraphics.setHidden(false);

		duration = new SampleInput("Duration", "Key Inputs", null);
		duration.setUnitType(TimeUnit.class);
		duration.setEntity(this);
		duration.setValidRange(0, Double.POSITIVE_INFINITY);
		duration.setRequired(true);
		this.addInput(duration);

		animation = new BooleanInput("Animation", "Key Inputs", true);
		this.addInput(animation);

		widthInput = new ValueInput("Width", "Key Inputs", 1.0d);
		widthInput.setUnitType(DimensionlessUnit.class);
		widthInput.setValidRange(1.0d, Double.POSITIVE_INFINITY);
		this.addInput(widthInput);

		colorInput = new ColourInput("Color", "Key Inputs", ColourInput.BLACK);
		this.addInput(colorInput);
		this.addSynonym(colorInput, "Colour");
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

		// Add the entity to the list of entities being delayed
		if (animation.getValue()) {
			EntityDelayEntry entry = new EntityDelayEntry();
			entry.ent = ent;
			entry.startTime = simTime;
			entry.duration = dur;
			entityMap.put(ent.getEntityNumber(), entry);
		}

		this.scheduleProcess(dur, 5, new RemoveDisplayEntityTarget(this, ent));

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

	@Override
	public void setPresentState() {
		if (this.getNumberInProgress() > 0) {
			this.setPresentState("Working");
		}
		else {
			this.setPresentState("Idle");
		}
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
