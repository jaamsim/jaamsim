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

import com.jaamsim.events.ProcessTarget;
import com.sandwell.JavaSimulation.EntityTarget;

public abstract class LinkedService extends LinkedComponent {

	private boolean busy;
	protected final ProcessTarget endActionTarget = new EndActionTarget(this);

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
