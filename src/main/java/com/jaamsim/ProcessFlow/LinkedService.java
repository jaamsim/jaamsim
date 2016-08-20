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
package com.jaamsim.ProcessFlow;

import java.util.ArrayList;

import com.jaamsim.BasicObjects.DowntimeEntity;
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

	@Keyword(description = "A list of thresholds that must be satisfied for the object to "
	                     + "operate. Operation is stopped immediately when one of the thresholds "
	                     + "closes. If a threshold closes part way though processing an entity, "
	                     + "the work is considered to be partly done and the remainder is "
	                     + "completed once the threshold re-opens.",
	         exampleList = {"ExpressionThreshold1 TimeSeriesThreshold1 SignalThreshold1"})
	protected final EntityListInput<Threshold> immediateThresholdList;

	@Keyword(description = "A list of thresholds that must be satisfied for the object to "
	                     + "operate. Operation is stopped immediately when one of the thresholds "
	                     + "closes. If a threshold closes part way though processing an entity, "
	                     + "the work is interrupted and the entity is released.",
	         exampleList = {"ExpressionThreshold1 TimeSeriesThreshold1 SignalThreshold1"})
	protected final EntityListInput<Threshold> immediateReleaseThresholdList;

	@Keyword(description = "A list of thresholds that must be satisfied for the object to "
	                     + "operate. If a threshold closes part way though processing an entity, "
	                     + "the remaining work is completed and the entity is released before the "
	                     + "object is closed.",
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
	private double duration;  // service time for the present entity
	private long endTicks;  // planned simulation time in ticks at the next event
	private double lastUpdateTime;
	private boolean forcedDowntimePending;
	private boolean processCompleted;  // indicates that the last processing loop was completed

	{
		stateGraphics.setHidden(false);
		workingStateListInput.setHidden(false);

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

		immediateThresholdList = new EntityListInput<>(Threshold.class, "ImmediateThresholdList", "Key Inputs", new ArrayList<Threshold>());
		this.addInput(immediateThresholdList);

		immediateReleaseThresholdList = new EntityListInput<>(Threshold.class, "ImmediateReleaseThresholdList", "Key Inputs", new ArrayList<Threshold>());
		this.addInput(immediateReleaseThresholdList);

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
		duration = 0.0;
		endTicks = 0L;
		lastUpdateTime = 0.0d;
		forcedDowntimePending = false;
		processCompleted = true;

		this.addState("Idle");
		this.addState("Working");
		this.addState("Stopped");
		this.addState("Clearing_while_Stopped");
		this.addState("Maintenance");
		this.addState("Breakdown");
	}

	@Override
	public String getInitialState() {
		return "Idle";
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		if (traceFlag) trace(0, "addEntity(%s)", ent);

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
		if (traceFlag) trace(0, "queueChanged");
		this.startAction();
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
	private final ProcessTarget endActionTarget = new EndActionTarget(this);
	private final EventHandle endActionHandle = new EventHandle();

	/**
	 * Starts the processing of an entity.
	 */
	protected final void startAction() {
		if (traceFlag) {
			trace(0, "startAction");
			traceLine(1, "endActionHandle.isScheduled=%s, isAvailable=%s, forcedDowntimePending=%s",
					endActionHandle.isScheduled(), this.isAvailable(), forcedDowntimePending);
		}

		double simTime = this.getSimTime();

		// Is the process loop is already working?
		if (endActionHandle.isScheduled()) {
			this.setPresentState();
			return;
		}

		// Stop if any of the thresholds, maintenance, or breakdowns close the operation
		// or if a forced downtime is about to begin
		if (!this.isAvailable() || forcedDowntimePending) {
			forcedDowntimePending = false;
			this.stopAction();
			return;
		}

		// Set the last update time in case processing is restarting after a stoppage
		lastUpdateTime = simTime;

		// Start a new process
		if (processCompleted) {
			boolean bool = this.startProcessing(simTime);
			if (!bool) {
				this.stopAction();
				return;
			}
			duration = this.getProcessingTime(simTime);
		}

		// Set the state
		if (!isBusy()) {
			this.setBusy(true);
			this.setPresentState();
		}

		// Schedule the completion of service
		processCompleted = false;
		endTicks = EventManager.calcSimTicks(duration);
		if (traceFlag) traceLine(1, "duration=%.6f", duration);
		this.scheduleProcess(duration, 5, endActionTarget, endActionHandle);
	}

	/**
	 * Completes the processing of an entity.
	 */
	final void endAction() {
		if (traceFlag) trace(0, "endAction");
		double simTime = this.getSimTime();

		// Update the progress that has been made
		this.updateProgress(simTime, lastUpdateTime);

		// If the process ended normally or if there was an immediate release type threshold
		// closure, then perform the special processing for this sub-class of LinkedService
		if (this.getSimTicks() == endTicks || this.isImmediateReleaseThresholdClosure()) {
			this.endProcessing(simTime);
			processCompleted = true;
		}

		// Process the next entity
		this.startAction();
	}

	/**
	 * Interrupts processing of an entity and holds it.
	 */
	private void stopAction() {
		if (traceFlag) {
			trace(0, "stopAction");
			traceLine(1, "endActionHandle.isScheduled()=%s", endActionHandle.isScheduled());
		}

		double simTime = this.getSimTime();

		// Interrupt processing, if underway
		if (endActionHandle.isScheduled()) {
			EventManager.killEvent(endActionHandle);
		}

		// Update the service for any partial progress that has been made
		this.updateProgress(simTime, lastUpdateTime);

		// Update the state
		this.setBusy(false);
		this.setPresentState();
	}

	/**
	 * Revises the time for the next event by stopping the present process and starting a new one.
	 */
	protected final void resetProcess() {
		if (traceFlag) {
			trace(0, "resetProcess");
			traceLine(1, "endActionHandle.isScheduled()=%s", endActionHandle.isScheduled());
		}

		// Set the present process to completed
		processCompleted = true;

		// End the present process prematurely
		if (endActionHandle.isScheduled()) {
			EventManager.interruptEvent(endActionHandle);
		}
	}

	/**
	 * Performs any special processing required for this sub-class of LinkedService
	 * @param simTime - present simulation time
	 * @return true if processing can continue
	 */
	protected boolean startProcessing(double simTime) {
		return true;
	}

	/**
	 * Returns the time required to complete the processing of an entity
	 * @param simTime - present simulation time
	 * @return duration required for processing
	 */
	protected double getProcessingTime(double simTime) {
		return 0.0;
	}

	/**
	 * Performs any special processing required for this sub-class of LinkedService
	 * @param simTime - present simulation time
	 */
	protected void endProcessing(double simTime) {}

	/**
	 * Performs any progress tracking that is required for this sub-class of LinkedService
	 * @param simTime - present simulation time
	 * @param lastTime - last time that the update was performed
	 */
	protected void updateProgress(double simTime, double lastTime) {
		lastUpdateTime = simTime;
		if (this.isBusy()) {
			duration -= simTime - lastTime;
		}
		if (traceFlag) {
			trace(1, "updateProgress");
			traceLine(2, "lastUpdateTime=%.6f, duration=%.6f", lastUpdateTime, duration);
		}
	}

	/**
	 * Returns the time at which the last update was performed.
	 * @return time for the last update
	 */
	protected final double getLastUpdateTime() {
		return lastUpdateTime;
	}

	// ********************************************************************************************
	// THRESHOLDS
	// ********************************************************************************************

	@Override
	public ArrayList<Threshold> getThresholds() {
		ArrayList<Threshold> ret = new ArrayList<>(operatingThresholdList.getValue());
		ret.addAll(immediateThresholdList.getValue());
		ret.addAll(immediateReleaseThresholdList.getValue());
		return ret;
	}

	@Override
	public void thresholdChanged() {
		if (traceFlag) {
			trace(0, "thresholdChanged");
			traceLine(1, "isImmediateReleaseThresholdClosure=%s, isImmediateThresholdClosure=%s",
				isImmediateReleaseThresholdClosure(), isImmediateThresholdClosure());
		}

		// If an interrupt closure, interrupt the present activity and release the entity
		if (isImmediateReleaseThresholdClosure()) {
			if (endActionHandle.isScheduled()) {
				EventManager.interruptEvent(endActionHandle);
			}
			return;
		}

		// If an immediate closure, interrupt the present activity and hold the entity
		if (isImmediateThresholdClosure()) {
			this.stopAction();
			return;
		}

		// Otherwise, check whether processing can be restarted
		this.startAction();
	}

	private boolean isImmediateThresholdClosure() {
		for (Threshold thresh : immediateThresholdList.getValue()) {
			if (!thresh.isOpen())
				return true;
		}
		return false;
	}

	private boolean isImmediateReleaseThresholdClosure() {
		for (Threshold thresh : immediateReleaseThresholdList.getValue()) {
			if (!thresh.isOpen())
				return true;
		}
		return false;
	}

	// ********************************************************************************************
	// PRESENT STATE
	// ********************************************************************************************

	private void setBusy(boolean bool) {
		busy = bool;
	}

	protected final boolean isBusy() {
		return busy;
	}

	/**
	 * Tests whether all the thresholds are open.
	 * @return true if all the thresholds are open.
	 */
	protected final boolean isOpen() {
		for (Threshold thr : immediateThresholdList.getValue()) {
			if (!thr.isOpen())
				return false;
		}
		for (Threshold thr : immediateReleaseThresholdList.getValue()) {
			if (!thr.isOpen())
				return false;
		}
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

	public boolean isAvailable() {
		return isOpen() && !isMaintenance() && !isBreakdown();
	}

	/**
	 * Tests whether the LinkedService is available for work.
	 * <p>
	 * A LinkedService has three mutually exclusive states: Busy, Idle, and UnableToWork.
	 * @return true if the LinkedService is available for work
	 */
	public boolean isIdle() {
		return !isBusy() && isAvailable();
	}

	/**
	 * Tests whether the LinkedService is not processing entities because something has prevented
	 * it from working.
	 * <p>
	 * A LinkedService has three mutually exclusive states: Busy, Idle, and UnableToWork.
	 * @return true if the LinkedService is not working because it is prevented from doing so
	 */
	public boolean isUnableToWork() {
		return !isBusy() && !isAvailable();
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
		if (traceFlag) {
			trace(0, "prepareForDowntime(%s)", down);
			traceLine(1, "isImmediateDowntime=%s, isForcedDowntime=%s, isBusy=%s",
				isImmediateDowntime(down), isForcedDowntime(down), isBusy());
		}

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
		if (traceFlag) trace(0, "startDowntime(%s)", down);
		this.setPresentState();
	}

	@Override
	public void endDowntime(DowntimeEntity down) {
		if (traceFlag) trace(0, "endDowntime(%s)", down);
		this.startAction();
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
	             + "and ImmediateThresholdList keywords are open.",
	    sequence = 1)
	public boolean getOpen(double simTime) {
		return isOpen();
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
