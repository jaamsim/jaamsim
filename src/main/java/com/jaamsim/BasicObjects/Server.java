/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.TimeUnit;

/**
 * Server processes entities one by one from a queue.  When finished with an entity, it passes it to the next
 * LinkedComponent in the chain.
 */
public class Server extends LinkedService implements QueueUser {

	@Keyword(description = "The service time required to process an entity.\n" +
			"A constant value, a distribution to be sampled, or a time series can be entered.",
	         example = "Server1 ServiceTime { 3.0 h }")
	private final SampleExpInput serviceTime;

	@Keyword(description = "The queue in which the waiting DisplayEntities will be placed.",
	         example = "Server1 WaitQueue { Queue1 }")
	private final EntityInput<Queue> waitQueue;

	private DisplayEntity servedEntity;	// the DisplayEntity being server

	{
		serviceTime = new SampleExpInput("ServiceTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setEntity(this);
		serviceTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(serviceTime);

		waitQueue = new EntityInput<>(Queue.class, "WaitQueue", "Key Inputs", null);
		this.addInput(waitQueue);
	}

	public Server() {}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the target queue has been specified
		if (waitQueue.getValue() == null) {
			throw new InputErrorException("The keyword WaitQueue must be set.");
		}

		serviceTime.validate();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		servedEntity = null;
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);

		// Add the entity to the queue
		waitQueue.getValue().addEntity(ent);
	}

	@Override
	public ArrayList<Queue> getQueues() {
		ArrayList<Queue> ret = new ArrayList<>();
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

	@Override
	public void startAction() {

		// Stop if the queue is empty or a threshold is closed
		if (waitQueue.getValue().getCount() == 0 || this.isClosed()) {
			servedEntity = null;
			this.setBusy(false);
			this.setPresentState();
			return;
		}

		// Remove the first entity from the queue
		servedEntity = waitQueue.getValue().removeFirst();
		double dt = serviceTime.getValue().getNextSample(getSimTime());

		// Schedule the completion of service
		this.scheduleProcess(dt, 5, endActionTarget);
	}

	@Override
	public void endAction() {

		// Send the entity to the next component in the chain
		this.sendToNextComponent(servedEntity);

		// Remove the next entity from the queue and start processing
		this.startAction();
	}

	@Override
	public void updateGraphics(double simTime) {

		// If an entity is being served, show it at the center of the Server
		if (servedEntity != null) {
			Vec3d serverCenter = this.getPositionForAlignment(new Vec3d());
			servedEntity.setPosition(serverCenter);
		}
	}

}
