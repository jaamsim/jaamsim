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

import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.EntityTarget;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.Queue;

public class EntityGate extends LinkedComponent {

	@Keyword(description = "The queue in which the waiting DisplayEntities will be placed.",
	         example = "EntityGate1 WaitQueue { Queue1 }")
	private final EntityInput<Queue> waitQueue;

	@Keyword(description = "The time delay before each queued entity is released.\n" +
			"Entities arriving at an open gate are not delayed.",
	         example = "EntityGate1 ReleaseDelay { 5.0 s }")
	private final ValueInput releaseDelay;

	@Keyword(description = "The initial state of the EntityGate: TRUE = Open, FALSE = Closed.",
	         example = "EntityGate1 InitialState { FALSE }")
	private final BooleanInput initialState;

	private boolean gateOpen;  // TRUE if the gate is open
	private boolean busy;  // TRUE if the process of emptying the queue has started

	{
		waitQueue = new EntityInput<Queue>( Queue.class, "WaitQueue", "Key Inputs", null);
		this.addInput( waitQueue);

		releaseDelay = new ValueInput( "ReleaseDelay", "Key Inputs", null);
		releaseDelay.setUnitType(TimeUnit.class);
		releaseDelay.setValidRange(0.0, Double.POSITIVE_INFINITY);
		this.addInput( releaseDelay);

		initialState = new BooleanInput( "InitialState", "Key Inputs", true);
		this.addInput( initialState);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the target queue has been specified
		if( waitQueue.getValue() == null ) {
			throw new InputErrorException( "The keyword WaitQueue must be set." );
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		busy = false;
		gateOpen = initialState.getValue();
	}

	/**
	 * Add a DisplayEntity from upstream
	 * @param ent = entity to be added
	 */
	@Override
	public void addDisplayEntity( DisplayEntity ent ) {
		super.addDisplayEntity(ent);

		// If the gate is closed or other entities are already queued, then add the entity to the queue
		Queue queue = waitQueue.getValue();
		if( queue.getCount() > 0 || !gateOpen ) {
			queue.addLast( ent );
			return;
		}

		// If the gate is open and there are no other entities still in the queue, then send the entity to the next component
		this.sendToNextComponent( ent );
	}

	/**
	 * Set the gate to the given state (open or closed).
	 * @param bool = new state (true = open, false = closed)
	 */
	public void setState(boolean bool) {
		if( bool ) {
			if( !gateOpen )
				this.open();
		}
		else {
			this.close();
		}
	}

	/**
	 * Close the gate.
	 */
	public void close() {
		gateOpen = false;
	}

	/**
	 * Open the gate and release any entities in the queue.
	 */
	public void open() {
		gateOpen = true;

		// Release any entities that are in the queue
		if( busy || waitQueue.getValue().getCount() == 0 )
			return;
		busy = true;
		this.scheduleProcess(releaseDelay.getValue(), 5, new ReleaseQueuedEntityTarget(this, "removeDisplayEntity"));
	}

	private static class ReleaseQueuedEntityTarget extends EntityTarget<EntityGate> {

		public ReleaseQueuedEntityTarget(EntityGate gate, String method) {
			super(gate,method);
		}

		@Override
		public void process() {
			ent.releaseQueuedEntity();
		}
	}

	/**
	 * Loop recursively through the queued entities, releasing them one by one.
	 */
	private void releaseQueuedEntity() {

		// Stop the recursive loop if the gate has closed or the queue has become empty
		Queue queue = waitQueue.getValue();
		if( !gateOpen || queue.getCount() == 0 ) {
			busy = false;
			return;
		}

		// Release the first element in the queue and send to the next component
		DisplayEntity ent = queue.removeFirst();
		this.sendToNextComponent( ent );

		// Stop the recursive loop if the queue is now empty
		if( queue.getCount() == 0 ) {
			busy = false;
			return;
		}

		// Continue the recursive loop by scheduling the release of the next queued entity
		this.scheduleProcess(releaseDelay.getValue(), 5, new ReleaseQueuedEntityTarget(this, "removeDisplayEntity"));
	}

}
