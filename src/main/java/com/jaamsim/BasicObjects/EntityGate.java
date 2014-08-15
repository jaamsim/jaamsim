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

import com.jaamsim.input.EntityInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.Queue;

public class EntityGate extends LinkedService {

	@Keyword(description = "The queue in which the waiting DisplayEntities will be placed.",
	         example = "EntityGate1 WaitQueue { Queue1 }")
	private final EntityInput<Queue> waitQueue;

	@Keyword(description = "The time delay before each queued entity is released.\n" +
			"Entities arriving at an open gate are not delayed.",
	         example = "EntityGate1 ReleaseDelay { 5.0 s }")
	private final ValueInput releaseDelay;

	{
		waitQueue = new EntityInput<Queue>(Queue.class, "WaitQueue", "Key Inputs", null);
		this.addInput(waitQueue);

		releaseDelay = new ValueInput("ReleaseDelay", "Key Inputs", 0.0);
		releaseDelay.setUnitType(TimeUnit.class);
		releaseDelay.setValidRange(0.0, Double.POSITIVE_INFINITY);
		this.addInput(releaseDelay);
	}

	public EntityGate() {}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the target queue has been specified
		if (waitQueue.getValue() == null) {
			throw new InputErrorException("The keyword WaitQueue must be set.");
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		this.setPresentState();
	}

	@Override
	public void addDisplayEntity(DisplayEntity ent) {
		super.addDisplayEntity(ent);

		// If the gate is closed or other entities are already queued, then add the entity to the queue
		Queue queue = waitQueue.getValue();
		if (queue.getCount() > 0 || this.isClosed()) {
			queue.addLast(ent);
			return;
		}

		// If the gate is open and there are no other entities still in the queue, then send the entity to the next component
		this.sendToNextComponent(ent);
	}

	@Override
	public void startAction() {

		// Stop if the gate has closed or the queue has become empty
		if (this.isClosed() || waitQueue.getValue().getCount() == 0) {
			this.setBusy(false);
			this.setPresentState();
			return;
		}

		// Schedule the release of the next entity
		this.scheduleProcess(releaseDelay.getValue(), 5, endActionTarget);
	}

	/**
	 * Loop recursively through the queued entities, releasing them one by one.
	 */
	@Override
	public void endAction() {

		// Release the first element in the queue and send to the next component
		this.sendToNextComponent(waitQueue.getValue().removeFirst());

		// Try to release another entity
		this.startAction();
	}

}
