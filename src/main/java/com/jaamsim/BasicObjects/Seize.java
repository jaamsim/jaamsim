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

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.IntegerInput;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.Queue;

public class Seize extends LinkedComponent {

	@Keyword(description = "The Resource to be seized.",
	         example = "Seize-1 Resource { Resource-1 }")
	private final EntityInput<Resource> resource;

	@Keyword(description = "The number of resource units to seize.",
	         example = "Seize-1 NumberOfUnits { 2 }")
	private final IntegerInput numberOfUnits;

	@Keyword(description = "The queue in which the waiting DisplayEntities will be placed.",
	         example = "Seize-1 WaitQueue { Queue-1 }")
	private final EntityInput<Queue> waitQueue;

	{
		resource = new EntityInput<Resource>(Resource.class, "Resource", "Key Inputs", null);
		this.addInput( resource, true);

		numberOfUnits = new IntegerInput("NumberOfUnits", "Key Inputs", 1);
		this.addInput( numberOfUnits, true);

		waitQueue = new EntityInput<Queue>( Queue.class, "WaitQueue", "Key Inputs", null);
		this.addInput( waitQueue, true);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the target queue has been specified
		if( waitQueue.getValue() == null ) {
			throw new InputErrorException( "The keyword WaitQueue must be set." );
		}

		// Confirm that the resource has been specified
		if( resource.getValue() == null ) {
			throw new InputErrorException( "The keyword Resource must be set." );
		}
	}

	/**
	 * Add a DisplayEntity from upstream
	 * @param ent = entity to be added
	 */
	@Override
	public void addDisplayEntity( DisplayEntity ent ) {
		super.addDisplayEntity(ent);

		Resource res = resource.getValue();
		int n = numberOfUnits.getValue();
		Queue queue = waitQueue.getValue();

		// If other entities are queued already or insufficient units are available, then add the entity to the queue
		if( queue.getCount() > 0 || res.getAvailableUnits() < n ) {
			queue.addLast( ent );
			return;
		}

		// If sufficient units are available, then seize them and pass the entity to the next component
		res.seize(n);
		this.sendToNextComponent( ent );
	}

	/**
	 * Remove an object from the queue, seize the specified resources, and pass to the next component
	 * @param i = position in the queue of the object to be removed
	 */
	public void processQueuedEntity(int i) {

		Resource res = resource.getValue();
		int n = numberOfUnits.getValue();

		if( res.getAvailableUnits() >= n ) {
			res.seize(n);
			DisplayEntity ent = waitQueue.getValue().remove(i);
			this.sendToNextComponent( ent );
		}
	}

	public Resource getResource() {
		return resource.getValue();
	}

	public Queue getQueue() {
		return waitQueue.getValue();
	}

	public int getNumberOfUnits() {
		return numberOfUnits.getValue();
	}

}
