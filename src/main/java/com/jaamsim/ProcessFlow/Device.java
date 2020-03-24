/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016-2020 JaamSim Software Inc.
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
import com.jaamsim.basicsim.ObserverEntity;
import com.jaamsim.basicsim.SubjectEntity;
import com.jaamsim.basicsim.SubjectEntityDelegate;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;

public abstract class Device extends StateUserEntity implements ObserverEntity, SubjectEntity {

	private double lastUpdateTime; // simulation time at which the process was updated last
	private double duration; // calculated duration of the process time step
	private long endTicks;  // planned simulation time in ticks at the end of the next process step
	private boolean readyToRelease;  // indicates that an entity was prevented from being released by a ReleaseThreshold
	private boolean stepCompleted;  // indicates that the last process time step was completed
	private boolean processing;  // indicates that the process loop is active
	private long startUpTicks;  // clock ticks at which device was started most recently

	private final SubjectEntityDelegate subject = new SubjectEntityDelegate(this);

	public Device() {}

	@Override
	public void earlyInit() {
		super.earlyInit();

		duration = 0.0;
		endTicks = 0L;
		lastUpdateTime = 0.0d;
		readyToRelease = false;
		stepCompleted = true;
		processing = false;
		startUpTicks = -1L;

		// Clear the list of observers
		subject.clear();
	}

	@Override
	public void lateInit() {
		super.lateInit();
		ObserverEntity.registerWithSubjects(this, getWatchList());
	}

	@Override
	public void registerObserver(ObserverEntity obs) {
		subject.registerObserver(obs);
	}

	@Override
	public void notifyObservers() {
		subject.notifyObservers();
	}

	@Override
	public void observerUpdate(SubjectEntity subj) {
		this.performUnscheduledUpdate();
	}

	/**
	 * Restarts the processing loop.
	 */
	public final void restart() {

		// If already working, do nothing
		if (processing) {
			if (isTraceFlag()) trace(0, "restart - ALREADY WORKING");
			setPresentState();
			return;
		}

		// If cannot restart, clear any setup that has already taken place
		if (!isAbleToRestart()) {
			if (isTraceFlag()) trace(0, "restart - UNABLE TO RESTART");
			setProcessStopped();
			setPresentState();
			return;
		}

		// Start work
		if (isTraceFlag()) trace(0, "restart - START WORK");
		processing = true;
		startUpTicks = getSimTicks();
		lastUpdateTime = getSimTime();
		startStep();
	}

	@Override
	public boolean isBusy() {
		return processing;
	}

	/**
	 * Returns whether the caller can be started.
	 * @return true if the caller can be started
	 */
	public boolean isAbleToRestart() {
		return isAvailable() && !isForcedDowntimePending() && !isImmediateDowntimePending();
	}

	/**
	 * Returns the simulation time in clock ticks at which the device was started most recently.
	 * @return start time in clock ticks
	 */
	public long getStartUpTicks() {
		return startUpTicks;
	}

	/**
	 * Starts the next time step for the process.
	 */
	private final void startStep() {
		if (isTraceFlag()) {
			trace(0, "startStep");
			traceLine(1, "isAvailable=%s, forcedDowntimePending=%s, immediateDowntimePending=%s",
					isAvailable(), isForcedDowntimePending(), isImmediateDowntimePending());
		}

		double simTime = this.getSimTime();

		// Is the process loop is already working?
		if (endStepHandle.isScheduled()) {
			error("Processing is already in progress.");
		}

		// Stop if any of the thresholds, maintenance, or breakdowns close the operation
		// or if a forced downtime is about to begin
		if (isReadyToStop()) {
			this.stopProcessing();
			return;
		}

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

		// Set the state for the time step
		long durTicks = EventManager.secsToNearestTick(duration);
		if (durTicks > 0L) {
			setPresentState();
		}

		// Schedule the completion of the time step
		stepCompleted = false;
		endTicks = getSimTicks() + durTicks;
		if (isTraceFlag()) traceLine(1, "duration=%.6f", duration);
		EventManager.scheduleTicks(durTicks, 5, true, endStepTarget, endStepHandle);  // FIFO order

		// Notify other processes that are dependent on this one
		if (this.isNewStepReqd(stepCompleted)) {
			this.processChanged();
		}

		// Notify any observers
		notifyObservers();
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
		if (isTraceFlag()) trace(0, "endStep");
		double simTime = this.getSimTime();

		// Update the process for the time that has elapsed
		this.updateProgress();

		// If the full step was completed or if there was an immediate release type threshold
		// closure, then determine whether to change state and/or to continue to the next step
		if (this.getSimTicks() == endTicks || this.isImmediateReleaseThresholdClosure()) {
			stepCompleted = true;
			this.processStep(simTime);
		}

		// Start the next time step
		this.startStep();
	}

	/**
	 * Updates the process calculations at the end of the time step.
	 */
	protected final void updateProgress() {
		if (isTraceFlag()) trace(1, "updateProgress");
		double simTime = this.getSimTime();

		if (this.isBusy()) {
			double dt = simTime - lastUpdateTime;
			duration -= dt;
			this.updateProgress(dt);
		}
		lastUpdateTime = simTime;
	}

	/**
	 * Return whether all work in progress has been completed.
	 * @return true if there is no work in progress
	 */
	public boolean isFinished() {
		return true;
	}

	public void setReadyToRelease(boolean bool) {
		readyToRelease = bool;
	}

	public boolean isReadyToStop() {
		return !isActive() || isMaintenance() || isBreakdown()
				|| isImmediateThresholdClosure() || isImmediateReleaseThresholdClosure()
				|| (isOperatingThresholdClosure() && isFinished())
				|| (isReleaseThresholdClosure() && readyToRelease)
				|| isImmediateDowntimePending() || (isForcedDowntimePending() && isFinished());
	}

	/**
	 * Halts further processing.
	 */
	private final void stopProcessing() {
		if (isTraceFlag()) trace(0, "stopProcessing");

		processing = false;
		setReadyToRelease(false);

		// Notify other processes that are dependent on this one
		this.processChanged();

		// Set the process to its stopped condition
		this.setProcessStopped();

		// Update the state
		this.setPresentState();
		startUpTicks = -1L;

		// Notify any observers
		notifyObservers();
	}

	/**
	 * Interrupts the present time step for the process so that a new one can be started based on
	 * new conditions.
	 */
	final void unscheduledUpdate() {

		// If process is being set up, wait for it to complete
		if (isSetup()) {
			if (isTraceFlag()) trace(0, "unscheduledUpdate - SETUP IN PROGRESS");
			return;
		}

		// If the process is working, perform its next update immediately
		if (endStepHandle.isScheduled()) {
			if (isTraceFlag()) trace(0, "unscheduledUpdate - WORK IN PROGRESS");
			EventManager.killEvent(endStepHandle);
			EventManager.scheduleTicks(0L, 5, true, endStepTarget, endStepHandle);  // FIFO order
			return;
		}

		// If the process is stopped, then restart it
		if (isTraceFlag()) trace(0, "unscheduledUpdate - RESTART");
		this.restart();
	}

	/**
	 * Schedules an update
	 */
	public final void performUnscheduledUpdate() {
		if (isTraceFlag()) trace(0, "performUnscheduledUpdate");

		if (!unscheduledUpdateHandle.isScheduled()) {
			EventManager.scheduleTicks(0, 10, true, unscheduledUpdateTarget,
					unscheduledUpdateHandle);
		}
	}

	private final EventHandle unscheduledUpdateHandle = new EventHandle();
	private final ProcessTarget unscheduledUpdateTarget = new EntityTarget<Device>(this, "unscheduledUpdate") {
		@Override
		public void process() {
			unscheduledUpdate();
		}
	};

	/**
	 * Revises the time for the next event by stopping the present process and starting a new one.
	 */
	protected final void resetProcess() {
		if (isTraceFlag()) {
			trace(0, "resetProcess");
			traceLine(1, "endActionHandle.isScheduled()=%s", endStepHandle.isScheduled());
		}

		// Set the present process to completed
		stepCompleted = true;

		// End the present process prematurely
		if (endStepHandle.isScheduled()) {
			EventManager.killEvent(endStepHandle);
			EventManager.scheduleTicks(0L, 5, true, endStepTarget, endStepHandle);  // FIFO order
		}
	}

	/**
	 * Performs the process calculations at the start of a new process time step.
	 * @param simTime - present simulation time
	 * @return indicates whether to continue processing
	 */
	protected abstract boolean startProcessing(double simTime);

	/**
	 * Returns the duration of the next process time step.
	 * @param simTime - present simulation time
	 * @return time step duration
	 */
	protected abstract double getStepDuration(double simTime);

	/**
	 * Performs the process calculations at the end of the time step.
	 * @param dt - elapsed simulation time
	 */
	protected abstract void updateProgress(double dt);

	/**
	 * Performs any calculations related to the state of the process and returns a boolean to
	 * specify whether to start a new time step.
	 * @param simTime - present simulation time
	 * @return indicates whether to start a new time step
	 */
	protected abstract void processStep(double simTime);

	/**
	 * Alerts other processes that the present process has changed.
	 */
	protected abstract void processChanged();

	/**
	 * Determines whether to start a new time step or to complete the present one.
	 * @param completed - indicate whether the present time step duration was completed in full
	 * @return whether to start a new time step
	 */
	protected abstract boolean isNewStepReqd(boolean completed);

	/**
	 * Set the process to its stopped condition.
	 */
	protected abstract void setProcessStopped();

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

	protected final double getRemainingDuration() {
		return duration;
	}

	// ********************************************************************************************
	// THRESHOLDS
	// ********************************************************************************************

	@Override
	public void thresholdChanged() {
		if (isTraceFlag()) {
			trace(0, "thresholdChanged");
			traceLine(1, "isImmediateReleaseThresholdClosure=%s, isImmediateThresholdClosure=%s",
				isImmediateReleaseThresholdClosure(), isImmediateThresholdClosure());
		}

		// If an immediate closure, interrupt the present activity and hold the entity
		if (isImmediateThresholdClosure() || isImmediateReleaseThresholdClosure()) {
			this.performUnscheduledUpdate();
			return;
		}

		// Otherwise, check whether processing can be restarted
		this.restart();
	}

	// ********************************************************************************************
	// MAINTENANCE AND BREAKDOWNS
	// ********************************************************************************************

	@Override
	public void prepareForDowntime(DowntimeEntity down) {
		super.prepareForDowntime(down);

		// If the device is idle already, then downtime can start right away
		if (!this.isBusy())
			return;

		// For an immediate downtime, interrupt the present process
		if (isImmediateDowntime(down)) {
			this.performUnscheduledUpdate();
			return;
		}
	}

	@Override
	public void endDowntime(DowntimeEntity down) {
		super.endDowntime(down);
		this.restart();
	}

}
