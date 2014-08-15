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

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.Queue;

public class Unpack extends LinkedService {

	@Keyword(description = "The queue in which the waiting containers will be placed.",
	         example = "Pack1 WaitQueue { Queue1 }")
	private final EntityInput<Queue> waitQueue;

	@Keyword(description = "The service time required to unpacking each entity.",
	         example = "Pack1 ServiceTime { 3.0 h }")
	private final SampleExpInput serviceTime;

	{
		waitQueue = new EntityInput<Queue>(Queue.class, "WaitQueue", "Key Inputs", null);
		this.addInput(waitQueue);

		serviceTime = new SampleExpInput("ServiceTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setEntity(this);
		this.addInput(serviceTime);
	}

	private EntityContainer container;	// the received EntityContainer

	public Unpack() {}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the waiting queue has been specified
		if (waitQueue.getValue() == null) {
			throw new InputErrorException("The keyword WaitQueue must be set.");
		}

		serviceTime.verifyUnit();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		container = null;
		this.setPresentState();
	}

	@Override
	public void addDisplayEntity(DisplayEntity ent) {
		super.addDisplayEntity(ent);

		// Add the entity to the queue
		waitQueue.getValue().addLast(ent);

		// If necessary, restart processing
		if (!this.isBusy()) {
			this.setBusy(true);
			this.setPresentState();
			this.startAction();
		}
	}

	@Override
	public void startAction() {

		// Are there sufficient entities in the queue?
		Queue que = waitQueue.getValue();
		if (container == null && que.getCount() == 0) {
			this.setBusy(false);
			this.setPresentState();
			return;
		}

		// Do any of the thresholds stop the generator?
		if (this.isClosed()) {
			this.setBusy(false);
			this.setPresentState();
			return;
		}

		if (container == null) {

			// Remove the container from the queue
			container = (EntityContainer)que.removeFirst();

			// Position the container over the unpack object
			Vec3d tmp = this.getPositionForAlignment(new Vec3d());
			tmp.add3(new Vec3d(0,0,0.01));
			container.setPosition(tmp);
		}

		// Schedule the removal of the next entity
		double dt = serviceTime.getValue().getNextSample(getSimTime());
		this.scheduleProcess(dt, 5, endActionTarget);
	}

	@Override
	public void endAction() {

		// Remove the next entity from the container
		this.sendToNextComponent(container.removeEntity());

		// If all the entities have been removed, then destroy the container
		if (container.getCount() == 0) {
			container.kill();
			container = null;
		}

		// Continue the process
		this.startAction();
	}

}
