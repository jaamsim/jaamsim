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
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.Queue;

public class Pack extends LinkedService {

	@Keyword(description = "The prototype for EntityContainers to be generated.\n" +
			"The generated EntityContainers will be copies of this entity.",
	         example = "Pack1 PrototypeEntityContainer { ProtoContainer }")
	private final EntityInput<EntityContainer> prototypeEntityContainer;

	@Keyword(description = "The queue in which the waiting entities will be placed.",
	         example = "Pack1 WaitQueue { Queue1 }")
	private final EntityInput<Queue> waitQueue;

	@Keyword(description = "The number of entities to pack into the container.",
	         example = "Pack1 NumberOfEntities { 2 }")
	private final IntegerInput numberOfEntities;

	@Keyword(description = "The service time required to pack each entity in the container.",
	         example = "Pack1 ServiceTime { 3.0 h }")
	private final SampleExpInput serviceTime;

	private EntityContainer container;	// the generated EntityContainer
	private int numberGenerated;  // Number of EntityContainers generated so far
	private int numberInserted;   // Number of entities inserted to the EntityContainer

	{
		prototypeEntityContainer = new EntityInput<EntityContainer>(EntityContainer.class, "PrototypeEntityContainer", "Key Inputs", null);
		this.addInput(prototypeEntityContainer);

		waitQueue = new EntityInput<Queue>(Queue.class, "WaitQueue", "Key Inputs", null);
		this.addInput(waitQueue);

		numberOfEntities = new IntegerInput("NumberOfEntities", "Key Inputs", 1);
		this.addInput(numberOfEntities);

		serviceTime = new SampleExpInput("ServiceTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setEntity(this);
		this.addInput(serviceTime);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the waiting queue has been specified
		if (waitQueue.getValue() == null) {
			throw new InputErrorException("The keyword WaitQueue must be set.");
		}

		// Confirm that prototype entity has been specified
		if (prototypeEntityContainer.getValue() == null) {
			throw new InputErrorException("The keyword PrototypeEntityContainer must be set.");
		}

		serviceTime.verifyUnit();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		container = null;
		numberGenerated = 0;
		numberInserted = 0;
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
		if (container == null && que.getCount() < numberOfEntities.getValue()) {
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

			// Create the container
			numberGenerated++;
			EntityContainer proto = prototypeEntityContainer.getValue();
			StringBuilder sb = new StringBuilder();
			sb.append(proto.getInputName()).append("_Copy").append(numberGenerated);
			container = InputAgent.defineEntityWithUniqueName(proto.getClass(), sb.toString(),"_", true);
			container.copyInputs(proto);
			container.setFlag(Entity.FLAG_GENERATED);
			container.earlyInit();
			numberInserted = 0;

			// Position the container over the pack object
			Vec3d tmp = this.getPositionForAlignment(new Vec3d());
			tmp.add3(new Vec3d(0,0,0.01));
			container.setPosition(tmp);
		}

		// Schedule the insertion of the next entity
		double dt = serviceTime.getValue().getNextSample(getSimTime());
		this.scheduleProcess(dt, 5, endActionTarget);
	}

	@Override
	public void endAction() {

		// Remove the next entity from the queue and pack the container
		container.addEntity(waitQueue.getValue().removeFirst());
		numberInserted++;

		// If the container is full, send it to the next component
		if (numberInserted == numberOfEntities.getValue()) {
			this.sendToNextComponent(container);
			container = null;
			numberInserted = 0;
		}

		// Insert the next entity
		this.startAction();
	}

}
