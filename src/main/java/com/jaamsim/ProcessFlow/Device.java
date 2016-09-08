/*
 * JaamSim Discrete Event Simulation
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


import com.jaamsim.BasicObjects.DowntimeEntity;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;

public class Device extends StateUserEntity {

	private double lastUpdateTime; // simulation time at which the process was updated last
	private double duration; // calculated duration of the process time step
	private long endTicks;  // planned simulation time in ticks at the end of the next process step
	private boolean forcedDowntimePending;  // indicates that a forced downtime event is ready
	private boolean stepCompleted;  // indicates that the last process time step was completed

	public Device() {}

	@Override
	public void earlyInit() {
		super.earlyInit();

		this.setBusy(false);
		duration = 0.0;
		endTicks = 0L;
		lastUpdateTime = 0.0d;
		forcedDowntimePending = false;
		stepCompleted = true;
	}

	// ********************************************************************************************
	// PROCESSING ENTITIES
	// ********************************************************************************************

	/**
	 * EndActionTarget
	 */
	private static class EndActionTarget extends EntityTarget<Device> {
		EndActionTarget(Device ent) {
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
		if (this.isNewStepReqd(stepCompleted)) {
			boolean bool = this.startProcessing(simTime);
			if (!bool) {
				this.stopAction();
				return;
			}
			duration = this.getProcessingTime(simTime);
		}

		// Trap errors
		if (Double.isNaN(duration))
			error("Cannot calculate duration");
		if (duration == Double.POSITIVE_INFINITY)
			error("Infinite duration");

		// Set the state
		if (!isBusy()) {
			this.setBusy(true);
			this.setPresentState();
		}

		// Schedule the completion of service
		stepCompleted = false;
		endTicks = EventManager.calcSimTicks(duration);
		if (traceFlag) traceLine(1, "duration=%.6f", duration);
		this.scheduleProcess(duration, 5, endActionTarget, endActionHandle);

		// Notify other processes that are dependent on this one
		if (this.isNewStepReqd(stepCompleted)) {
			this.processChanged();
		}
	}

	/**
	 * Completes the processing of an entity.
	 */
	final void endAction() {
		if (traceFlag) trace(0, "endAction");
		double simTime = this.getSimTime();

		// Update the progress that has been made
		this.updateProgress();

		// If the process ended normally or if there was an immediate release type threshold
		// closure, then perform the special processing for this sub-class of LinkedService
		if (this.getSimTicks() == endTicks || this.isImmediateReleaseThresholdClosure()) {
			stepCompleted = true;
			boolean bool = this.processStep(simTime);
			if (!bool)
				return;
		}

		// Process the next entity
		this.startAction();
	}

	/**
	 * Updates the process calculations at the end of the time step.
	 */
	protected final void updateProgress() {
		if (traceFlag) trace(1, "updateProgress");
		double simTime = this.getSimTime();

		if (this.isBusy()) {
			double dt = simTime - lastUpdateTime;
			duration -= dt;
			this.updateProgress(dt);
		}
		lastUpdateTime = simTime;
	}

	/**
	 * Interrupts processing of an entity and holds it.
	 */
	private void stopAction() {
		if (traceFlag) {
			trace(0, "stopAction");
			traceLine(1, "endActionHandle.isScheduled()=%s", endActionHandle.isScheduled());
		}

		// Interrupt processing, if underway
		if (endActionHandle.isScheduled()) {
			EventManager.killEvent(endActionHandle);
		}

		// Update the service for any partial progress that has been made
		this.updateProgress();

		// Update the state
		this.setBusy(false);
		this.setPresentState();

		// Notify other processes that are dependent on this one
		this.processChanged();
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
		stepCompleted = true;

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
	 * Performs the process calculations at the end of the time step.
	 * @param dt - elapsed simulation time
	 */
	protected void updateProgress(double dt) {}

	/**
	 * Performs any special processing required for this sub-class of LinkedService
	 * @param simTime - present simulation time
	 */
	protected void endProcessing(double simTime) {}

	/**
	 * Performs any calculations related to the state of the process and returns a boolean to
	 * specify whether to start a new time step.
	 * @param simTime - present simulation time
	 * @return indicates whether to start a new time step
	 */
	protected boolean processStep(double simTime) {
		this.endProcessing(simTime);
		return true;
	}

	/**
	 * Alerts other processes that the present process has changed.
	 */
	protected void processChanged() {}

	/**
	 * Determines whether to start a new time step or to complete the present one.
	 * @param completed - indicate whether the present time step duration was completed in full
	 * @return whether to start a new time step
	 */
	protected boolean isNewStepReqd(boolean completed) {
		return completed;
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

	// ********************************************************************************************
	// MAINTENANCE AND BREAKDOWNS
	// ********************************************************************************************

	@Override
	public boolean canStartDowntime(DowntimeEntity down) {

		// Downtime can start when any work in progress has been interrupted and there are no
		// other maintenance or breakdown activities that are being performed. It is okay to start
		// downtime when one or more thresholds are closed.
		return !isBusy() && !isMaintenance() && !isBreakdown();
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

}
