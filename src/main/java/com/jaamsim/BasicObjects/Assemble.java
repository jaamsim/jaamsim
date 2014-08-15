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
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.Queue;

public class Assemble extends LinkedService {

	@Keyword(description = "The service time required to perform the assembly process.",
	         example = "EntityAssemble1 ServiceTime { 3.0 h }")
	private final SampleExpInput serviceTime;

	@Keyword(description = "A list of Queue objects in which to place the arriving sub-component entities.",
	         example = "EntityAssemble1 WaitQueueList { Queue1 }")
	private final EntityListInput<Queue> waitQueueList;

	@Keyword(description = "An index that determines the Queue in which to place the incoming entity:" +
			"1 = first queue, 2 = second queue, etc.",
	         example = "EntityAssemble1 Choice { 1 }")
	private final SampleExpInput choice;

	@Keyword(description = "The prototype for entities representing the assembled part.",
	         example = "EntityAssemble1 PrototypeEntity { Proto }")
	private final EntityInput<DisplayEntity> prototypeEntity;

	private DisplayEntity assembledEntity;	// the generated entity representing the assembled part
	private int numberGenerated = 0;  // Number of entities generated so far

	{
		serviceTime = new SampleExpInput("ServiceTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setEntity(this);
		this.addInput(serviceTime);

		waitQueueList = new EntityListInput<Queue>(Queue.class, "WaitQueueList", "Key Inputs", null);
		this.addInput(waitQueueList);

		choice = new SampleExpInput("Choice", "Key Inputs", new SampleConstant(DimensionlessUnit.class, 1));
		choice.setUnitType(DimensionlessUnit.class);
		choice.setEntity(this);
		this.addInput(choice);

		prototypeEntity = new EntityInput<DisplayEntity>(DisplayEntity.class, "PrototypeEntity", "Key Inputs", null);
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

		serviceTime.verifyUnit();
		choice.verifyUnit();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		assembledEntity = null;
		numberGenerated = 0;
		this.setPresentState();
	}

	@Override
	public void addDisplayEntity( DisplayEntity ent ) {
		super.addDisplayEntity(ent);

		// Choose the queue in which to place the entity
		int ind = (int) choice.getValue().getNextSample(getSimTime());
		ind = Math.max(ind, 1);
		ind = Math.min(ind, waitQueueList.getValue().size());

		// Add the entity to the queue
		waitQueueList.getValue().get(ind-1).addLast(ent);

		// If necessary, wake up the server
		if (!this.isBusy()) {
			this.setBusy(true);
			this.setPresentState();
			this.startAction();
		}
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
		sb.append(proto.getInputName()).append("_Copy").append(numberGenerated);
		assembledEntity = InputAgent.defineEntityWithUniqueName(proto.getClass(), sb.toString(),"_", true);
		assembledEntity.copyInputs(proto);
		assembledEntity.setFlag(Entity.FLAG_GENERATED);
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
