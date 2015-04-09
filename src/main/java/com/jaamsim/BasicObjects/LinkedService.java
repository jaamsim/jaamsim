/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Thresholds.Threshold;
import com.jaamsim.Thresholds.ThresholdUser;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;

public abstract class LinkedService extends LinkedComponent implements ThresholdUser, QueueUser {

	@Keyword(description = "The queue in which the waiting DisplayEntities will be placed.",
	         example = "Server1 WaitQueue { Queue1 }")
	protected final EntityInput<Queue> waitQueue;

	@Keyword(description = "A list of thresholds that must be satisified for the entity to operate.",
			example = "EntityGenerator1 OperatingThresholdList { Server1 }")
	protected final EntityListInput<Threshold> operatingThresholdList;

	private boolean busy;
	protected final ProcessTarget endActionTarget = new EndActionTarget(this);

	{
		stateGraphics.setHidden(false);

		waitQueue = new EntityInput<>(Queue.class, "WaitQueue", "Key Inputs", null);
		this.addInput(waitQueue);

		operatingThresholdList = new EntityListInput<>(Threshold.class, "OperatingThresholdList", "Key Inputs", new ArrayList<Threshold>());
		this.addInput( operatingThresholdList);
	}

	public LinkedService() {}

	@Override
	public void validate() {

		// Confirm that the target queue has been specified
		if (!waitQueue.getHidden() && waitQueue.getValue() == null) {
			throw new InputErrorException("The keyword WaitQueue must be set.");
		}

	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		this.setBusy(false);
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

	/**
	 * Removes the next entity to be processed from the queue.
	 * @return the next entity for processing.
	 */
	protected DisplayEntity getNextEntity() {
		DisplayEntity ent = waitQueue.getValue().removeFirst();
		this.registerEntity(ent);
		return ent;
	}

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

	public abstract void startAction();

	public abstract void endAction();

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

	protected void setPresentState() {
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

	// ******************************************************************************************************
	// OUTPUTS
	// ******************************************************************************************************

	@Output(name = "Open",
	 description = "Returns TRUE if all the thresholds specified by the OperatingThresholdList keyword are open.")
	public boolean getOpen(double simTime) {
		for (Threshold thr : operatingThresholdList.getValue()) {
			if (!thr.getOpen(simTime))
				return false;
		}
		return true;
	}

	@Output(name = "Working",
	 description = "Returns TRUE if entities are being processed.")
	public boolean isBusy(double simTime) {
		return isBusy();
	}

}
