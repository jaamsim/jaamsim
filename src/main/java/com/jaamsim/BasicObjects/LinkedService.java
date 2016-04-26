/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Thresholds.Threshold;
import com.jaamsim.Thresholds.ThresholdUser;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.states.DowntimeUser;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;

public abstract class LinkedService extends LinkedComponent implements ThresholdUser, QueueUser, DowntimeUser {

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

	@Keyword(description = "A list of DowntimeEntities representing planned maintenance that "
	                     + "must be performed immediately, interrupting any work underway at "
	                     + "present.",
	         exampleList = {"DowntimeEntity1 DowntimeEntity2 DowntimeEntity3"})
	protected final EntityListInput<DowntimeEntity> immediateMaintenanceList;

	@Keyword(description = "A list of DowntimeEntities representing planned maintenance that "
	                     + "must begin as soon as task underway at present is finished.",
	         exampleList = {"DowntimeEntity1 DowntimeEntity2 DowntimeEntity3"})
	protected final EntityListInput<DowntimeEntity> forcedMaintenanceList;

	@Keyword(description = "A list of DowntimeEntities representing planned maintenance that "
	                     + "can wait until task underway at present is finished and the queue "
	                     + "of tasks is empty.",
	         exampleList = {"DowntimeEntity1 DowntimeEntity2 DowntimeEntity3"})
	protected final EntityListInput<DowntimeEntity> opportunisticMaintenanceList;

	@Keyword(description = "A list of DowntimeEntities representing unplanned maintenance that "
	                     + "must be performed immediately, interrupting any work underway at "
	                     + "present.",
	         exampleList = {"DowntimeEntity1 DowntimeEntity2 DowntimeEntity3"})
	protected final EntityListInput<DowntimeEntity> immediateBreakdownList;

	@Keyword(description = "A list of DowntimeEntities representing unplanned maintenance that "
	                     + "must begin as soon as task underway at present is finished.",
	         exampleList = {"DowntimeEntity1 DowntimeEntity2 DowntimeEntity3"})
	protected final EntityListInput<DowntimeEntity> forcedBreakdownList;

	@Keyword(description = "A list of DowntimeEntities representing unplanned maintenance that "
	                     + "can wait until task underway at present is finished and the queue "
	                     + "of tasks is empty.",
	         exampleList = {"DowntimeEntity1 DowntimeEntity2 DowntimeEntity3"})
	protected final EntityListInput<DowntimeEntity> opportunisticBreakdownList;

	private boolean busy;  // indicates that entities are being processed
	private Integer matchValue;
	protected double startTime;  // start of service time for the present entity
	protected double duration;  // service time for the present entity
	protected boolean forcedDowntimePending;
	private boolean processKilled;  // indicates that processing of an entity has been interrupted
	protected double stopWorkTime;  // last time at which the busy state was set to false

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

		immediateMaintenanceList =  new EntityListInput<>(DowntimeEntity.class,
				"ImmediateMaintenanceList", "Maintenance", new ArrayList<DowntimeEntity>());
		this.addInput(immediateMaintenanceList);

		forcedMaintenanceList =  new EntityListInput<>(DowntimeEntity.class,
				"ForcedMaintenanceList", "Maintenance", new ArrayList<DowntimeEntity>());
		this.addInput(forcedMaintenanceList);

		opportunisticMaintenanceList =  new EntityListInput<>(DowntimeEntity.class,
				"OpportunisticMaintenanceList", "Maintenance", new ArrayList<DowntimeEntity>());
		this.addInput(opportunisticMaintenanceList);

		immediateBreakdownList =  new EntityListInput<>(DowntimeEntity.class,
				"ImmediateBreakdownList", "Maintenance", new ArrayList<DowntimeEntity>());
		this.addInput(immediateBreakdownList);

		forcedBreakdownList =  new EntityListInput<>(DowntimeEntity.class,
				"ForcedBreakdownList", "Maintenance", new ArrayList<DowntimeEntity>());
		this.addInput(forcedBreakdownList);

		opportunisticBreakdownList =  new EntityListInput<>(DowntimeEntity.class,
				"OpportunisticBreakdownList", "Maintenance", new ArrayList<DowntimeEntity>());
		this.addInput(opportunisticBreakdownList);
	}

	public LinkedService() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		this.setBusy(false);
		matchValue = null;
		startTime = 0.0;
		duration = 0.0;
		forcedDowntimePending = false;
		processKilled = false;
		stopWorkTime = 0.0;
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
		this.restartAction();
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
	protected final ProcessTarget endActionTarget = new EndActionTarget(this);
	protected final EventHandle endActionHandle = new EventHandle();

	protected boolean isBusy() {
		return busy;
	}

	protected void setBusy(boolean bool) {
		if (bool == busy)
			return;

		if (!bool)
			stopWorkTime = this.getSimTime();

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

	/**
	 * Interrupts processing of an entity.
	 */
	protected void stopAction() {

		// Is processing underway?
		if (endActionHandle.isScheduled()) {

			// Interrupt the process
			EventManager.killEvent(endActionHandle);
			processKilled = true;

			// Update the state
			this.setBusy(false);
			this.setPresentState();
		}

		// If the server is not processing entities, then record the state change
		this.setPresentState();
	}

	/**
	 * Checks whether processing can be resumed or restarted.
	 */
	protected void restartAction() {

		// Is the server unused, but available to start work?
		if (this.isIdle()) {
			this.setBusy(true);
			this.setPresentState();

			// If work has been interrupted by a breakdown or other event, then resume work
			if (processKilled) {
				processKilled = false;
				duration -= stopWorkTime - startTime;
				startTime = this.getSimTime();
				this.scheduleProcess(duration, 5, endActionTarget, endActionHandle);
				return;
			}

			// Otherwise, start work on a new entity
			this.startAction();
			return;
		}

		// If the server cannot start work or is already working, then record the state change
		this.setPresentState();
	}

	// ********************************************************************************************
	// THRESHOLDS
	// ********************************************************************************************

	@Override
	public ArrayList<Threshold> getThresholds() {
		return operatingThresholdList.getValue();
	}

	@Override
	public void thresholdChanged() {
		this.restartAction();
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

	private boolean isMaintenance() {
		for (DowntimeEntity de : immediateMaintenanceList.getValue()) {
			if (de.isDown())
				return true;
		}
		for (DowntimeEntity de : forcedMaintenanceList.getValue()) {
			if (de.isDown())
				return true;
		}
		for (DowntimeEntity de : opportunisticMaintenanceList.getValue()) {
			if (de.isDown())
				return true;
		}
		return false;
	}

	private boolean isBreakdown() {
		for (DowntimeEntity de : immediateBreakdownList.getValue()) {
			if (de.isDown())
				return true;
		}
		for (DowntimeEntity de : forcedBreakdownList.getValue()) {
			if (de.isDown())
				return true;
		}
		for (DowntimeEntity de : opportunisticBreakdownList.getValue()) {
			if (de.isDown())
				return true;
		}
		return false;
	}

	/**
	 * Tests whether the LinkedService is available for work.
	 * <p>
	 * A LinkedService has three mutually exclusive states: Busy, Idle, and UnableToWork.
	 * @return true if the LinkedService is available for work
	 */
	public boolean isIdle() {
		return !isBusy() && isOpen() && !isMaintenance() && !isBreakdown();
	}

	/**
	 * Tests whether the LinkedService is not processing entities because something has prevented
	 * it from working.
	 * <p>
	 * A LinkedService has three mutually exclusive states: Busy, Idle, and UnableToWork.
	 * @return true if the LinkedService is not working because it is prevented from doing so
	 */
	public boolean isUnableToWork() {
		return !isBusy() && (!isOpen() || isMaintenance() || isBreakdown());
	}

	@Override
	public void setPresentState() {

		// Processing entities (Busy)
		if (this.isBusy()) {
			if (this.isOpen()) {
				this.setPresentState("Working");
				return;
			}
			else {
				this.setPresentState("Clearing_while_Stopped");
				return;
			}
		}

		// Not processing entities because something has prevented it from working (UnableToWork)
		if (!this.isOpen()) {
			this.setPresentState("Stopped");
			return;
		}
		if (this.isMaintenance()) {
			this.setPresentState("Maintenance");
			return;
		}
		if (this.isBreakdown()) {
			this.setPresentState("Breakdown");
			return;
		}

		// Not processing entities because there is nothing to do (Idle)
		this.setPresentState("Idle");
		return;
	}

	// ********************************************************************************************
	// MAINTENANCE AND BREAKDOWNS
	// ********************************************************************************************

	@Override
	public ArrayList<DowntimeEntity> getMaintenanceEntities() {
		ArrayList<DowntimeEntity> ret = new ArrayList<>();
		ret.addAll(immediateMaintenanceList.getValue());
		ret.addAll(forcedMaintenanceList.getValue());
		ret.addAll(opportunisticMaintenanceList.getValue());
		return ret;
	}

	@Override
	public ArrayList<DowntimeEntity> getBreakdownEntities() {
		ArrayList<DowntimeEntity> ret = new ArrayList<>();
		ret.addAll(immediateBreakdownList.getValue());
		ret.addAll(forcedBreakdownList.getValue());
		ret.addAll(opportunisticBreakdownList.getValue());
		return ret;
	}

	public boolean isImmediateDowntime(DowntimeEntity down) {
		return immediateMaintenanceList.getValue().contains(down)
				|| immediateBreakdownList.getValue().contains(down);
	}

	public boolean isForcedDowntime(DowntimeEntity down) {
		return forcedMaintenanceList.getValue().contains(down)
				|| forcedBreakdownList.getValue().contains(down);
	}

	public boolean isOpportunisticDowntime(DowntimeEntity down) {
		return opportunisticMaintenanceList.getValue().contains(down)
				|| opportunisticBreakdownList.getValue().contains(down);
	}

	@Override
	public boolean canStartDowntime(DowntimeEntity down) {

		// Downtime can only start from the Idle state, that is:
		// - any work in progress must have been interrupted,
		// - there can be no other maintenance or breakdown activities in progress, and
		// - all the thresholds must be open
		return isIdle();
	}

	@Override
	public void prepareForDowntime(DowntimeEntity down) {

		// If an immediate downtime, interrupt the present activity
		if (isImmediateDowntime(down)) {
			this.stopAction();
			return;
		}

		// If a forced downtime, then set the flag to stop further processing
		if (isForcedDowntime(down) && this.isBusy())
			forcedDowntimePending = true;
	}

	@Override
	public void startDowntime(DowntimeEntity down) {
		this.setPresentState();
	}

	@Override
	public void endDowntime(DowntimeEntity down) {
		this.restartAction();
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

	@Output(name = "Maintenance",
	 description = "Returns TRUE if maintenance is being performed.",
	    sequence = 3)
	public boolean isMaintenance(double simTime) {
		return isMaintenance();
	}

	@Output(name = "Breakdown",
	 description = "Returns TRUE if a breakdown is being repaired.",
	    sequence = 4)
	public boolean isBreakdown(double simTime) {
		return isBreakdown();
	}

	@Output(name = "Utilisation",
	 description = "The fraction of calendar time (excluding the initialisation period) that "
	             + "this object is in the Working state.",
	  reportable = true,
	    sequence = 5)
	public double getUtilisation(double simTime) {
		double total = simTime;
		if (simTime > Simulation.getInitializationTime())
			total -= Simulation.getInitializationTime();
		double working = this.getTimeInState(simTime, "Working");
		return working/total;
	}

	@Output(name = "Commitment",
	 description = "The fraction of calendar time (excluding the initialisation period) that "
	             + "this object is in any state other than Idle.",
	  reportable = true,
	    sequence = 6)
	public double getCommitment(double simTime) {
		double total = simTime;
		if (simTime > Simulation.getInitializationTime())
			total -= Simulation.getInitializationTime();
		double idle = this.getTimeInState(simTime, "Idle");
		return 1.0d - idle/total;
	}

	@Output(name = "Availability",
	 description = "The fraction of calendar time (excluding the initialisation period) that "
	             + "this object is in any state other than Maintenance or Breakdown.",
	  reportable = true,
	    sequence = 7)
	public double getAvailability(double simTime) {
		double total = simTime;
		if (simTime > Simulation.getInitializationTime())
			total -= Simulation.getInitializationTime();
		double maintenance = this.getTimeInState(simTime, "Maintenance");
		double breakdown = this.getTimeInState(simTime, "Breakdown");
		return 1.0d - (maintenance + breakdown)/total;
	}

	@Output(name = "Reliability",
	 description = "The ratio of Working time to the sum of Working time and Breakdown time. "
	             + "All times exclude the initialisation period.",
	  reportable = true,
	    sequence = 8)
	public double getReliability(double simTime) {
		double working = this.getTimeInState(simTime, "Working");
		double breakdown = this.getTimeInState(simTime, "Breakdown");
		return working / (working + breakdown);
	}

}
