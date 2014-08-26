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

import com.jaamsim.Thresholds.Threshold;
import com.jaamsim.Thresholds.ThresholdUser;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.EntityTarget;

public abstract class LinkedService extends LinkedComponent implements ThresholdUser {

	@Keyword(description = "A list of thresholds that must be satisified for the entity to operate.",
			example = "EntityGenerator1 OperatingThresholdList { Server1 }")
	protected final EntityListInput<Threshold> operatingThresholdList;

	private boolean busy;
	protected final ProcessTarget endActionTarget = new EndActionTarget(this);

	{
		operatingThresholdList = new EntityListInput<Threshold>(Threshold.class, "OperatingThresholdList", "Key Inputs", new ArrayList<Threshold>());
		this.addInput( operatingThresholdList);
	}

	public LinkedService() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		this.setBusy(false);
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
		if (this.isBusy()) {
			this.setPresentState();
		}
		else {
			this.setBusy(true);
			this.setPresentState();
			this.startAction();
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

	/**
	 * Test whether any of the thresholds are closed.
	 * @return true if any threshold is closed.
	 */
	public boolean isClosed() {
		return !this.isOpen();
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

}
