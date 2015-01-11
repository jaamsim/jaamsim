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
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.TimeUnit;

public class Assemble extends LinkedService {

	@Keyword(description = "The service time required to perform the assembly process.",
	         example = "EntityAssemble1 ServiceTime { 3.0 h }")
	private final SampleExpInput serviceTime;

	@Keyword(description = "A list of Queue objects in which to place the arriving sub-component entities.",
	         example = "EntityAssemble1 WaitQueueList { Queue1 }")
	private final EntityListInput<Queue> waitQueueList;

	@Keyword(description = "The prototype for entities representing the assembled part.",
	         example = "EntityAssemble1 PrototypeEntity { Proto }")
	private final EntityInput<DisplayEntity> prototypeEntity;

	private DisplayEntity assembledEntity;	// the generated entity representing the assembled part
	private int numberGenerated = 0;  // Number of entities generated so far

	{
		waitQueue.setHidden(true);

		serviceTime = new SampleExpInput("ServiceTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setEntity(this);
		serviceTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(serviceTime);

		waitQueueList = new EntityListInput<>(Queue.class, "WaitQueueList", "Key Inputs", null);
		this.addInput(waitQueueList);

		prototypeEntity = new EntityInput<>(DisplayEntity.class, "PrototypeEntity", "Key Inputs", null);
		this.addInput(prototypeEntity);
	}

	public Assemble() {}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the waiting queues have been specified
		if (waitQueueList.getValue() == null) {
			throw new InputErrorException("The keyword WaitQueueList must be set.");
		}

		// Confirm that prototype entity has been specified
		if (prototypeEntity.getValue() == null) {
			throw new InputErrorException("The keyword PrototypeEntity must be set.");
		}

		serviceTime.validate();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		assembledEntity = null;
		numberGenerated = 0;
	}

	@Override
	public void addEntity( DisplayEntity ent ) {
		super.addEntity(ent);

		// By default, add the entity to the first queue
		waitQueueList.getValue().get(0).addEntity(ent);
	}

	@Override
	public ArrayList<Queue> getQueues() {
		return waitQueueList.getValue();
	}

	/**
	* Process DisplayEntities from the Queue
	*/
	@Override
	public void startAction() {

		// Do all the queues have at least one entity?
		for (Queue que : waitQueueList.getValue()) {
			if (que.getCount() == 0) {
				this.setBusy(false);
				this.setPresentState();
				return;
			}
		}

		// Do any of the thresholds stop the generator?
		if (this.isClosed()) {
			this.setBusy(false);
			this.setPresentState();
			return;
		}

		// Remove and destroy one entity from each queue
		for (Queue que : waitQueueList.getValue()) {
			DisplayEntity ent = que.removeFirst();
			ent.kill();
		}

		// Create the entity representing the assembled part
		numberGenerated++;
		DisplayEntity proto = prototypeEntity.getValue();
		StringBuilder sb = new StringBuilder();
		sb.append(proto.getName()).append("_Copy").append(numberGenerated);
		assembledEntity = InputAgent.generateEntityWithName(proto.getClass(), sb.toString());
		assembledEntity.copyInputs(proto);
		assembledEntity.earlyInit();

		// Position the assembled part over the Assemble object
		Vec3d pos = this.getPositionForAlignment(new Vec3d());
		pos.add3(new Vec3d(0,0,0.01));
		assembledEntity.setPosition(pos);

		// Schedule the completion of processing
		double dt = serviceTime.getValue().getNextSample(getSimTime());
		this.scheduleProcess(dt, 5, endActionTarget);
	}

	@Override
	public void endAction() {

		// Send the assembled part to the next element in the chain
		this.sendToNextComponent(assembledEntity);
		assembledEntity = null;

		// Try to assemble another part
		this.startAction();
	}

}
