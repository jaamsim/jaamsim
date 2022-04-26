/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2022 JaamSim Software Inc.
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
import java.util.LinkedHashMap;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.DisplayModels.PolylineModel;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.LineEntity;
import com.jaamsim.Graphics.PolylineInfo;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.SubModels.CompoundEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
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
	                     + "Applicable only when AllowOvertaking is FALSE.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput minSeparation;

	@Keyword(description = "If TRUE, an entity is moved along the specified path to "
	                     + "indicate its progression through the delay activity.",
	         exampleList = {"TRUE"})
	private final BooleanInput animation;

	@Keyword(description = "If TRUE, the entities are rotated to match the direction of "
	                     + "the path.",
	         exampleList = {"TRUE"})
	private final BooleanInput rotateEntities;

	@Keyword(description = "The width in pixels of the line representing the EntityDelay.",
	         exampleList = {"1"})
	private final IntegerInput widthInput;

	@Keyword(description = "The colour of the line representing the EntityDelay.",
	         exampleList = {"red"})
	private final ColourInput colorInput;

	private long exitTicks;  // ticks at which the previous entity will leave the path
	private final LinkedHashMap<Long, EntityDelayEntry> entityMap = new LinkedHashMap<>();  // Entities being handled

	{
		displayModelListInput.clearValidClasses();
		displayModelListInput.addValidClass(PolylineModel.class);

		stateGraphics.setHidden(false);

		duration = new SampleInput("Duration", KEY_INPUTS, null);
		duration.setUnitType(TimeUnit.class);
		duration.setValidRange(0, Double.POSITIVE_INFINITY);
		duration.setRequired(true);
		this.addInput(duration);

		allowOvertaking = new BooleanInput("AllowOvertaking", KEY_INPUTS, true);
		this.addInput(allowOvertaking);

		minSeparation = new SampleInput("MinSeparation", KEY_INPUTS, new SampleConstant(0.0d));
		minSeparation.setUnitType(TimeUnit.class);
		minSeparation.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(minSeparation);

		animation = new BooleanInput("Animation", FORMAT, true);
		animation.setCallback(inputCallback);
		this.addInput(animation);

		rotateEntities = new BooleanInput("RotateEntities", FORMAT, false);
		this.addInput(rotateEntities);

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

	static final InputCallback inputCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((EntityDelay)ent).updateAnimationValue();
		}
	};

	void updateAnimationValue() {
		if (!animation.getValue())
			entityMap.clear();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		exitTicks = -1L;
		entityMap.clear();
	}

	@Override
	public String getInitialState() {
		return "Idle";
	}

	private static class EntityDelayEntry {
		final DisplayEntity ent;
		final double startTime;
		final double duration;

		public EntityDelayEntry(DisplayEntity e, double start, double dur) {
			ent = e;
			startTime = start;
			duration = dur;
		}
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);

		// Select the delay time for this entity
		double simTime = this.getSimTime();
		double dur = duration.getNextSample(simTime);
		long durTicks = EventManager.current().secondsToNearestTick(dur);

		// Adjust the duration for the previous entity's exit time
		if (!allowOvertaking.getValue()) {
			double sep = minSeparation.getNextSample(simTime);
			long sepTicks = EventManager.current().secondsToNearestTick(sep);
			long simTicks = getSimTicks();
			durTicks = Math.max(durTicks, exitTicks - simTicks + sepTicks);
			exitTicks = simTicks + durTicks;
		}

		// Add the entity to the list of entities being delayed
		if (animation.getValue()) {
			dur = EventManager.current().ticksToSeconds(durTicks);
			EntityDelayEntry entry = new EntityDelayEntry(ent, simTime, dur);
			entityMap.put(ent.getEntityNumber(), entry);
		}

		RemoveDisplayEntityTarget target = new RemoveDisplayEntityTarget(this, ent);
		scheduleProcessTicks(durTicks, 5, true, target, null); // FIFO

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

		// Notify any observers
		notifyObservers();
	}

	public void setPresentState() {
		if (this.getNumberInProgress() > 0) {
			this.setPresentState("Working");
		}
		else {
			this.setPresentState("Idle");
		}
	}

	public PolylineModel getPolylineModel() {
		DisplayModel dm = getDisplayModel();
		if (dm instanceof PolylineModel)
			return (PolylineModel) dm;
		return null;
	}

	@Override
	public boolean isOutlined() {
		return true;
	}

	@Override
	public int getLineWidth() {
		PolylineModel model = getPolylineModel();
		if (widthInput.isDefault() && model != null)
			return model.getLineWidth();
		return widthInput.getValue();
	}

	@Override
	public Color4d getLineColour() {
		PolylineModel model = getPolylineModel();
		if (colorInput.isDefault() && model != null)
			return model.getLineColour();
		return colorInput.getValue();
	}

	@Override
	public void updateGraphics(double simTime) {

		if (!usePointsInput())
			return;

		// Copy the list to avoid concurrent modification exceptions
		ArrayList<EntityDelayEntry> copiedList;
		try {
			copiedList = new ArrayList<>(entityMap.values());
		}
		catch (Exception e) {
			return;
		}

		// If the EntityDelay is not visible show the entities at the sub-model's process position
		if (!getShow() && getVisibleParent() instanceof CompoundEntity) {
			CompoundEntity ce = (CompoundEntity) getVisibleParent();
			for (EntityDelayEntry entry : copiedList) {
				entry.ent.moveToProcessPosition(ce, ce.getProcessPosition());
			}
			return;
		}

		// Loop through the entities on the path
		for (EntityDelayEntry entry : copiedList) {
			// Calculate the distance travelled by this entity
			double frac = ( simTime - entry.startTime ) / entry.duration;

			// Set the region for the entity
			entry.ent.setRegion(this.getCurrentRegion());

			// Set the position for the entity
			Vec3d localPos = PolylineInfo.getPositionOnPolyline(getCurvePoints(), frac);
			entry.ent.setGlobalPosition(this.getGlobalPosition(localPos));

			// Set the orientation for the entity
			Vec3d orient = new Vec3d();
			if (rotateEntities.getValue()) {
				orient.z = PolylineInfo.getAngleOnPolyline(getCurvePoints(), frac);
			}
			entry.ent.setRelativeOrientation(orient);
		}
	}

	@Output(name = "EntityList",
	 description = "The entities being processed at present.",
	    sequence = 1)
	public ArrayList<DisplayEntity> getEntityList(double simTime) {
		ArrayList<DisplayEntity> ret = new ArrayList<>(entityMap.size());
		for (EntityDelayEntry entry : entityMap.values()) {
			ret.add(entry.ent);
		}
		return ret;
	}

}
