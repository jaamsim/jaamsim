/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Thresholds.Threshold;
import com.jaamsim.Thresholds.ThresholdUser;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;

public abstract class LinkedService extends LinkedComponent implements ThresholdUser, QueueUser {

	@Keyword(description = "The position of the entity being processed relative to the processor.",
	         exampleList = {"1.0 0.0 0.01 m"})
	protected final Vec3dInput processPosition;

	@Keyword(description = "The queue in which the waiting DisplayEntities will be placed.",
	         exampleList = {"Queue1"})
	protected final EntityInput<Queue> waitQueue;

	@Keyword(description = "An expression returning a dimensionless integer value that can be "
	                     + "used to determine which of the queued entities is eligible for "
	                     + "processing.",
	         exampleList = {"this.obj.Attrib1"})
	protected final SampleInput match;

	@Keyword(description = "A list of thresholds that must be satisified for the entity to "
	                     + "operate.",
	         exampleList = {"ExpressionThreshold1 TimeSeriesThreshold1 SignalThreshold1"})
	protected final EntityListInput<Threshold> operatingThresholdList;

	private boolean busy;
	private Integer matchValue;
	protected final ProcessTarget endActionTarget = new EndActionTarget(this);

	{
		stateGraphics.setHidden(false);

		processPosition = new Vec3dInput("ProcessPosition", "Key Inputs", new Vec3d(0.0d, 0.0d, 0.01d));
		processPosition.setUnitType(DistanceUnit.class);
		this.addInput(processPosition);

		waitQueue = new EntityInput<>(Queue.class, "WaitQueue", "Key Inputs", null);
		waitQueue.setRequired(true);
		this.addInput(waitQueue);

		match = new SampleInput("Match", "Key Inputs", null);
		match.setUnitType(DimensionlessUnit.class);
		match.setEntity(this);
		this.addInput(match);

		operatingThresholdList = new EntityListInput<>(Threshold.class, "OperatingThresholdList", "Key Inputs", new ArrayList<Threshold>());
		this.addInput(operatingThresholdList);
	}

	public LinkedService() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		this.setBusy(false);
		matchValue = null;
	}

	@Override
	public String getInitialState() {
		return "Idle";
	}

	@Override
	public void addEntity(DisplayEntity ent) {

		// If there is no queue, then process the entity immediately
		if (waitQueue.getValue() == null) {
			super.addEntity(ent);
			return;
		}

		// Add the entity to the queue
		waitQueue.getValue().addEntity(ent);
	}

	// ********************************************************************************************
	// SELECTING AN ENTITY FROM THE WAIT QUEUE
	// ********************************************************************************************

	/**
	 * Removes the next entity to be processed from the queue.
	 * If the specified match value is not null, then only the queued entities
	 * with the same match value are eligible to be removed.
	 * @param m - match value.
	 * @return next entity for processing.
	 */
	protected DisplayEntity getNextEntityForMatch(Integer m) {
		DisplayEntity ent = waitQueue.getValue().removeFirstForMatch(m);
		this.registerEntity(ent);
		return ent;
	}

	/**
	 * Returns a value which determines which of the entities in the queue are
	 * eligible to be removed.
	 * @param simTime - present simulation time in seconds.
	 * @return match value.
	 */
	protected Integer getNextMatchValue(double simTime) {
		matchValue = null;
		if (match.getValue() != null)
			matchValue = (int) match.getValue().getNextSample(simTime);
		return matchValue;
	}

	protected void setMatchValue(Integer m) {
		matchValue = m;
	}

	protected Integer getMatchValue() {
		return matchValue;
	}

	// ********************************************************************************************
	// WAIT QUEUE
	// ********************************************************************************************

	@Override
	public ArrayList<Queue> getQueues() {
		ArrayList<Queue> ret = new ArrayList<>();
		if (waitQueue.getValue() != null)
			ret.add(waitQueue.getValue());
		return ret;
	}

	@Override
	public void queueChanged() {

		// If necessary, wake up the server
		if (!this.isBusy() && this.isOpen()) {
			this.setBusy(true);
			this.setPresentState();
			this.startAction();
		}
	}

	// ********************************************************************************************
	// PROCESSING ENTITIES
	// ********************************************************************************************

	/**
	 * EndActionTarget
	 */
	private static class EndActionTarget extends EntityTarget<LinkedService> {
		EndActionTarget(LinkedService ent) {
			super(ent, "endAction");
		}

		@Override
		public void process() {
			ent.endAction();
		}
	}

	protected boolean isBusy() {
		return busy;
	}

	protected void setBusy(boolean bool) {
		busy = bool;
	}

	/**
	 * Starts the processing of an entity.
	 */
	public abstract void startAction();

	/**
	 * Completes the processing of an entity.
	 */
	public abstract void endAction();

	// ********************************************************************************************
	// THRESHOLDS
	// ********************************************************************************************

	@Override
	public ArrayList<Threshold> getThresholds() {
		return operatingThresholdList.getValue();
	}

	@Override
	public void thresholdChanged() {

		// If necessary, restart processing
		if (this.isOpen() && !this.isBusy()) {
			this.setBusy(true);
			this.setPresentState();
			this.startAction();
		}
		else {
			this.setPresentState();
		}
	}

	// ********************************************************************************************
	// PRESENT STATE
	// ********************************************************************************************

	/**
	 * Tests whether all the thresholds are open.
	 * @return true if all the thresholds are open.
	 */
	public boolean isOpen() {
		for (Threshold thr : operatingThresholdList.getValue()) {
			if (!thr.isOpen())
				return false;
		}
		return true;
	}

	@Override
	public void setPresentState() {
		if (this.isOpen()) {
			if (this.isBusy()) {
				this.setPresentState("Working");
			}
			else {
				this.setPresentState("Idle");
			}
		}
		else {
			if (this.isBusy()) {
				this.setPresentState("Clearing_while_Stopped");
			}
			else {
				this.setPresentState("Stopped");
			}
		}
	}

	// ********************************************************************************************
	// GRAPHICS
	// ********************************************************************************************

	protected final void moveToProcessPosition(DisplayEntity ent) {
		Vec3d pos = this.getGlobalPosition();
		pos.add3(processPosition.getValue());
		ent.setGlobalPosition(pos);
	}

	// ********************************************************************************************
	// OUTPUTS
	// ********************************************************************************************

	@Output(name = "MatchValue",
	 description = "The present value to be matched in the queue.",
	    sequence = 0)
	public Integer getMatchValue(double simTime) {
		return matchValue;
	}

	@Output(name = "Open",
	 description = "Returns TRUE if all the thresholds specified by the OperatingThresholdList "
	             + "keyword are open.",
	    sequence = 1)
	public boolean getOpen(double simTime) {
		for (Threshold thr : operatingThresholdList.getValue()) {
			if (!thr.getOpen(simTime))
				return false;
		}
		return true;
	}

	@Output(name = "Working",
	 description = "Returns TRUE if entities are being processed.",
	    sequence = 2)
	public boolean isBusy(double simTime) {
		return isBusy();
	}

}
