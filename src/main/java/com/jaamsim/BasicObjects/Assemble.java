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
import com.jaamsim.Thresholds.Threshold;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.EntityTarget;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.Queue;

public class Assemble extends LinkedComponent {

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

	private boolean busy;
	private DisplayEntity assembledEntity;	// the generated entity representing the assembled part
	private int numberGenerated = 0;  // Number of entities generated so far
	private final ProcessTarget completeProcessing = new CompletionOfProcessingTarget(this);

	{
		serviceTime = new SampleExpInput("ServiceTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
		serviceTime.setUnitType(TimeUnit.class );
		serviceTime.setEntity(this);
		this.addInput(serviceTime);

		waitQueueList = new EntityListInput<Queue>(Queue.class, "WaitQueueList", "Key Inputs", null);
		this.addInput(waitQueueList);

		choice = new SampleExpInput("Choice", "Key Inputs", new SampleConstant(DimensionlessUnit.class, 1));
		choice.setUnitType(DimensionlessUnit.class );
		choice.setEntity(this);
		this.addInput(choice);

		prototypeEntity = new EntityInput<DisplayEntity>(DisplayEntity.class, "PrototypeEntity", "Key Inputs", null);
		this.addInput(prototypeEntity);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the waiting queues have been specified
		if( waitQueueList.getValue() == null ) {
			throw new InputErrorException( "The keyword WaitQueueList must be set." );
		}

		// Confirm that prototype entity has been specified
		if( prototypeEntity.getValue() == null ) {
			throw new InputErrorException( "The keyword PrototypeEntity must be set." );
		}

		serviceTime.verifyUnit();
		choice.verifyUnit();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		busy = false;
		assembledEntity = null;
		numberGenerated = 0;
	}

	/**
	 * Add a DisplayEntity from upstream
	 */
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
		if (!busy) {
			busy = true;
			this.processEntities();
		}
	}

	/**
	* Process DisplayEntities from the Queue
	*/
	public void processEntities() {

		// Do all the queues have at least one entity?
		for (Queue que : waitQueueList.getValue()) {
			if (que.getCount() == 0) {
				busy =false;
				return;
			}
		}

		// Do any of the thresholds stop the generator?
		for (Threshold thr : this.getThresholds()) {
			if (thr.isClosed()) {
				busy = false;
				return;
			}
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

		// Position the assembled part over the Assemble object
		Vec3d pos = this.getPositionForAlignment(new Vec3d());
		assembledEntity.setPosition(pos);

		// Schedule the completion of processing
		double dt = serviceTime.getValue().getNextSample(getSimTime());
		this.scheduleProcess(dt, 5, completeProcessing);
	}

	private static class CompletionOfProcessingTarget extends EntityTarget<Assemble> {
		CompletionOfProcessingTarget(Assemble ent) {
			super(ent, "completionOfProcessing");
		}

		@Override
		public void process() {
			ent.completionOfProcessing();
		}
	}

	public void completionOfProcessing() {

		// Send the assembled part to the next element in the chain
		this.sendToNextComponent(assembledEntity);
		assembledEntity = null;

		// Try to assemble another part
		this.processEntities();

	}

	@Override
	public void thresholdChanged() {

		// Restart processing, if necessary
		if (!busy) {
			busy = true;
			this.processEntities();
		}
	}
}
