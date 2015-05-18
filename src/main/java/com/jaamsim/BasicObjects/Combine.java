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
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.TimeUnit;

public class Combine extends LinkedService {

	@Keyword(description = "The service time required to perform the assembly process.",
	         example = "EntityAssemble1 ServiceTime { 3.0 h }")
	private final SampleExpInput serviceTime;

	@Keyword(description = "A list of Queue objects in which to place the arriving sub-component entities.",
	         example = "EntityAssemble1 WaitQueueList { Queue1 }")
	private final EntityListInput<Queue> waitQueueList;

	private DisplayEntity processedEntity;	// the DisplayEntity being processed

	{
		waitQueue.setHidden(true);
		match.setHidden(true);

		serviceTime = new SampleExpInput("ServiceTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setEntity(this);
		serviceTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(serviceTime);

		waitQueueList = new EntityListInput<>(Queue.class, "WaitQueueList", "Key Inputs", null);
		waitQueueList.setRequired(true);
		this.addInput(waitQueueList);
	}

	public Combine() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		processedEntity = null;
	}

	@Override
	public void addEntity( DisplayEntity ent ) {
		error("An entity cannot be sent directly to an Combine object. It must be sent to the appropriate queue.");
	}

	@Override
	public ArrayList<Queue> getQueues() {
		return waitQueueList.getValue();
	}

	@Override
	public void startAction() {

		// Do any of the thresholds stop the generator?
		if (!this.isOpen()) {
			this.setBusy(false);
			this.setPresentState();
			return;
		}

		// Do the queues have enough entities?
		ArrayList<Queue> queueList = waitQueueList.getValue();
		Integer m = Queue.selectMatchValue(queueList, null);
		if (m == null) {
			this.setBusy(false);
			this.setPresentState();
			return;
		}
		this.setMatchValue(m);

		// Remove one entity from each queue
		for (int i=queueList.size()-1; i>=0; i--) {
			DisplayEntity ent = queueList.get(i).removeFirstForMatch(m);
			if (ent == null)
				error("An entity with the specified match value %s was not found in %s.",
						m, queueList.get(i));
			this.registerEntity(ent);

			// Destroy all the entities but the first
			if (i == 0)
				processedEntity = ent;
			else
				ent.kill();
		}

		// Position the processed entity relative to the Assemble object
		this.moveToProcessPosition(processedEntity);

		// Schedule the completion of processing
		double dt = serviceTime.getValue().getNextSample(getSimTime());
		this.scheduleProcess(dt, 5, endActionTarget);

	}

	@Override
	public void endAction() {

		// Send the first entity to the next element in the chain
		this.sendToNextComponent(processedEntity);
		processedEntity = null;

		// Try to combine another set of entities
		this.startAction();
	}

}
