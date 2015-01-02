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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;

public class AddTo extends Pack {

	@Keyword(description = "The queue in which the waiting containers will be placed.",
	         example = "AddTo1 ContainerQueue { Queue1 }")
	private final EntityInput<Queue> containerQueue;

	{
		prototypeEntityContainer.setHidden(true);

		containerQueue = new EntityInput<>(Queue.class, "ContainerQueue", "Key Inputs", null);
		this.addInput(containerQueue);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the container queue has been specified
		if (containerQueue.getValue() == null) {
			throw new InputErrorException("The keyword ContainerQueue must be set.");
		}
	}

	@Override
	public void addEntity(DisplayEntity ent) {

		// Add an incoming container to its queue
		if (ent instanceof EntityContainer) {
			containerQueue.getValue().addLast(ent);

			// If necessary, restart processing
			if (!this.isBusy()) {
				this.setBusy(true);
				this.setPresentState();
				this.startAction();
			}
		}
		else {
			super.addEntity(ent);
		}
	}

	@Override
	protected EntityContainer getNextContainer() {
		return (EntityContainer) containerQueue.getValue().removeFirst();
	}

	@Override
	public void startAction() {

		// Is there a container waiting to be filled?
		if (container == null && containerQueue.getValue().getCount() == 0) {
			this.setBusy(false);
			this.setPresentState();
			return;
		}

		super.startAction();
	}

}
