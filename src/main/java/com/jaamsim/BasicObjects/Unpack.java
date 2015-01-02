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
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.TimeUnit;

public class Unpack extends LinkedService {

	@Keyword(description = "The queue in which the waiting containers will be placed.",
	         example = "Pack1 WaitQueue { Queue1 }")
	private final EntityInput<Queue> waitQueue;

	@Keyword(description = "The service time required to unpacking each entity.",
	         example = "Pack1 ServiceTime { 3.0 h }")
	private final SampleExpInput serviceTime;

	private int numberToRemove;   // Number of entities to remove from the present EntityContainer

	{
		waitQueue = new EntityInput<>(Queue.class, "WaitQueue", "Key Inputs", null);
		this.addInput(waitQueue);

		serviceTime = new SampleExpInput("ServiceTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setEntity(this);
		serviceTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(serviceTime);
	}

	private EntityContainer container;	// the received EntityContainer
	private int numberRemoved;   // Number of entities removed from the received EntityContainer

	public Unpack() {}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the waiting queue has been specified
		if (waitQueue.getValue() == null) {
			throw new InputErrorException("The keyword WaitQueue must be set.");
		}

		serviceTime.validate();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		container = null;
		numberRemoved = 0;
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);

		// Add the entity to the queue
		waitQueue.getValue().addEntity(ent);

		// If necessary, restart processing
		if (!this.isBusy()) {
			this.setBusy(true);
			this.setPresentState();
			this.startAction();
		}
	}

	@Override
	public void startAction() {

		// Is there a container waiting to be unpacked?
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
			numberToRemove = this.getNumberToRemove();
			numberRemoved = 0;

			// Position the container over the unpack object
			Vec3d tmp = this.getPositionForAlignment(new Vec3d());
			tmp.add3(new Vec3d(0,0,0.01));
			container.setPosition(tmp);
		}

		// Schedule the removal of the next entity
		double dt = 0.0;
		if (numberRemoved < numberToRemove && container.getCount() > 0)
			dt = serviceTime.getValue().getNextSample(getSimTime());
		this.scheduleProcess(dt, 5, endActionTarget);
	}

	protected void disposeContainer(EntityContainer c) {
		c.kill();
	}

	protected int getNumberToRemove() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void endAction() {

		// Remove the next entity from the container
		if (numberRemoved < numberToRemove && container.getCount() > 0) {
			this.sendToNextComponent(container.removeEntity());
			numberRemoved++;
		}

		// Stop when the desired number of entities have been removed
		if (container.getCount() == 0 || numberRemoved == numberToRemove) {
			this.disposeContainer(container);
			container = null;
		}

		// Continue the process
		this.startAction();
	}

}
