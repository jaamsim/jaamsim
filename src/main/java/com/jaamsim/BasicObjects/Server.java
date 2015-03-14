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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.TimeUnit;

/**
 * Server processes entities one by one from a queue.  When finished with an entity, it passes it to the next
 * LinkedComponent in the chain.
 */
public class Server extends LinkedService {

	@Keyword(description = "The service time required to process an entity.\n" +
			"A constant value, a distribution to be sampled, or a time series can be entered.",
	         example = "Server1 ServiceTime { 3.0 h }")
	private final SampleExpInput serviceTime;

	private DisplayEntity servedEntity;	// the DisplayEntity being server

	{
		serviceTime = new SampleExpInput("ServiceTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setEntity(this);
		serviceTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(serviceTime);
	}

	public Server() {}

	@Override
	public void validate() {
		super.validate();

		serviceTime.validate();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		servedEntity = null;
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
		servedEntity = this.getNextEntity();
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
			Vec3d serverCenter = this.getGlobalPositionForAlignment(new Vec3d());
			servedEntity.setGlobalPosition(serverCenter);
		}
	}

}
