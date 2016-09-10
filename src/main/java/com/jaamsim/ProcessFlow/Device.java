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

	/**
	 * Starts the next time step for the process.
	 */
	protected final void startStep() {
		if (traceFlag) {
			trace(0, "startStep");
			traceLine(1, "endActionHandle.isScheduled=%s, isAvailable=%s, forcedDowntimePending=%s",
					endStepHandle.isScheduled(), this.isAvailable(), forcedDowntimePending);
		}

		double simTime = this.getSimTime();

		// Is the process loop is already working?
		if (endStepHandle.isScheduled()) {
			this.setPresentState();
			return;
		}

		// Stop if any of the thresholds, maintenance, or breakdowns close the operation
		// or if a forced downtime is about to begin
		if (!this.isAvailable() || forcedDowntimePending) {
			forcedDowntimePending = false;
			this.stopProcessing();
			return;
		}

		// Set the last update time in case processing is restarting after a stoppage
		lastUpdateTime = simTime;

		// Start the next time step
		if (this.isNewStepReqd(stepCompleted)) {
			boolean bool = this.startProcessing(simTime);
			if (!bool) {
				this.stopProcessing();
				return;
			}
			duration = this.getStepDuration(simTime);
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

		// Schedule the completion of the time step
		stepCompleted = false;
		endTicks = EventManager.calcSimTicks(duration);
		if (traceFlag) traceLine(1, "duration=%.6f", duration);
		this.scheduleProcess(duration, 5, endStepTarget, endStepHandle);

		// Notify other processes that are dependent on this one
		if (this.isNewStepReqd(stepCompleted)) {
			this.processChanged();
		}
	}

	/**
	 * EndActionTarget
	 */
	private static class EndStepTarget extends EntityTarget<Device> {
		EndStepTarget(Device ent) {
			super(ent, "endStep");
		}

		@Override
		public void process() {
			ent.endStep();
		}
	}
	private final ProcessTarget endStepTarget = new EndStepTarget(this);
	private final EventHandle endStepHandle = new EventHandle();

	/**
	 * Completes the processing of an entity.
	 */
	final void endStep() {
		if (traceFlag) trace(0, "endStep");
		double simTime = this.getSimTime();

		// Update the process for the time that has elapsed
		this.updateProgress();

		// If the full step was completed or if there was an immediate release type threshold
		// closure, then determine whether to change state and/or to continue to the next step
		if (this.getSimTicks() == endTicks || this.isImmediateReleaseThresholdClosure()) {
			stepCompleted = true;
			boolean bool = this.processStep(simTime);
			if (!bool)
				return;
		}

		// Start the next time step
		this.startStep();
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
	 * Halts further processing.
	 */
	private void stopProcessing() {
		if (traceFlag) {
			trace(0, "stopProcessing");
			traceLine(1, "endActionHandle.isScheduled()=%s", endStepHandle.isScheduled());
		}

		// If the process is working, kill the next scheduled update
		if (endStepHandle.isScheduled()) {
			EventManager.killEvent(endStepHandle);
		}

		// Update the service for any partial progress that has been made
		this.updateProgress();

		// Set the process to its stopped condition
		this.setProcessStopped();

		// Update the state
		this.setBusy(false);
		this.setPresentState();

		// Notify other processes that are dependent on this one
		this.processChanged();
	}

	/**
	 * Interrupts the present time step for the process so that a new one can be started based on
	 * new conditions.
	 */
	public void unscheduledUpdate() {
		if (traceFlag) trace(0, "unscheduledUpdate");

		// If the process is working, perform its next update immediately
		if (endStepHandle.isScheduled()) {
			EventManager.interruptEvent(endStepHandle);
			return;
		}

		// If the process is stopped, then restart it
		this.startStep();
	}

	/**
	 * Schedules an update
	 */
	public void performUnscheduledUpdate() {
		if (traceFlag) trace(1, "performUnscheduledUpdate");

		if (!unscheduledUpdateHandle.isScheduled()) {
			EventManager.scheduleTicks(0, 2, false, unscheduledUpdateTarget,
					unscheduledUpdateHandle);
		}
	}

	/**
	 * ProcessTarget for the unscheduledUpdate method
	 */
	private static class UnscheduledUpdateTarget extends EntityTarget<Device> {
		UnscheduledUpdateTarget(Device ent) {
			super(ent, "unscheduledUpdate");
		}

		@Override
		public void process() {
			ent.unscheduledUpdate();
		}
	}
	private final ProcessTarget unscheduledUpdateTarget = new UnscheduledUpdateTarget(this);
	private final EventHandle unscheduledUpdateHandle = new EventHandle();

	/**
	 * Revises the time for the next event by stopping the present process and starting a new one.
	 */
	protected final void resetProcess() {
		if (traceFlag) {
			trace(0, "resetProcess");
			traceLine(1, "endActionHandle.isScheduled()=%s", endStepHandle.isScheduled());
		}

		// Set the present process to completed
		stepCompleted = true;

		// End the present process prematurely
		if (endStepHandle.isScheduled()) {
			EventManager.interruptEvent(endStepHandle);
		}
	}

	/**
	 * Performs the process calculations at the start of a new process time step.
	 * @param simTime - present simulation time
	 * @return indicates whether to continue processing
	 */
	protected boolean startProcessing(double simTime) {
		return true;
	}

	/**
	 * Returns the duration of the next process time step.
	 * @param simTime - present simulation time
	 * @return time step duration
	 */
	protected double getStepDuration(double simTime) {
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
	 * Set the process to its stopped condition.
	 */
	protected void setProcessStopped() {}

	/**
	 * Returns the time at which the last update was performed.
	 * @return time for the last update
	 */
	protected final double getLastUpdateTime() {
		return lastUpdateTime;
	}

	protected final void setStepCompleted(boolean bool) {
		stepCompleted = bool;
	}

	protected final boolean isStepCompleted() {
		return stepCompleted;
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
			if (endStepHandle.isScheduled()) {
				EventManager.interruptEvent(endStepHandle);
			}
			return;
		}

		// If an immediate closure, interrupt the present activity and hold the entity
		if (isImmediateThresholdClosure()) {
			this.stopProcessing();
			return;
		}

		// Otherwise, check whether processing can be restarted
		this.startStep();
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
			this.stopProcessing();
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
		this.startStep();
	}

}
