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

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.IntegerListInput;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.Queue;

public class Seize extends LinkedComponent {

	@Keyword(description = "The Resource(s) to be seized.",
	         example = "Seize1 Resource { Resource1 Resource2 }")
	private final EntityListInput<Resource> resourceList;

	@Keyword(description = "The number of units to seize from the Resource(s).",
	         example = "Seize1 NumberOfUnits { 2 1 }")
	private final IntegerListInput numberOfUnitsList;

	@Keyword(description = "The queue in which the waiting DisplayEntities will be placed.",
	         example = "Seize1 WaitQueue { Queue1 }")
	private final EntityInput<Queue> waitQueue;

	{
		resourceList = new EntityListInput<Resource>(Resource.class, "Resource", "Key Inputs", null);
		this.addInput( resourceList);

		IntegerVector defNum = new IntegerVector();
		defNum.add(1);
		numberOfUnitsList = new IntegerListInput("NumberOfUnits", "Key Inputs", defNum);
		this.addInput( numberOfUnitsList);

		waitQueue = new EntityInput<Queue>( Queue.class, "WaitQueue", "Key Inputs", null);
		this.addInput( waitQueue);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the target queue has been specified
		if( waitQueue.getValue() == null ) {
			throw new InputErrorException( "The keyword WaitQueue must be set." );
		}

		// Confirm that the resource has been specified
		if( resourceList.getValue() == null ) {
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
		Queue queue = waitQueue.getValue();

		// If other entities are queued already or insufficient units are available, then add the entity to the queue
		if( queue.getCount() > 0 || !this.checkResources() ) {
			queue.addLast( ent );
			return;
		}

		// If sufficient units are available, then seize them and pass the entity to the next component
		this.seizeResources();
		this.sendToNextComponent( ent );
	}

	/**
	 * Determine whether the required Resources are available.
	 * @return = TRUE if all the resources are available
	 */
	public boolean checkResources() {
		ArrayList<Resource> resList = resourceList.getValue();
		IntegerVector numberList = numberOfUnitsList.getValue();
		for(int i=0; i<resList.size(); i++) {
			if( resList.get(i).getAvailableUnits() < numberList.get(i) )
				return false;
		}
		return true;
	}

	/**
	 * Seize the required Resources.
	 * @return
	 */
	public void seizeResources() {
		ArrayList<Resource> resList = resourceList.getValue();
		IntegerVector numberList = numberOfUnitsList.getValue();
		for(int i=0; i<resList.size(); i++) {
			resList.get(i).seize(numberList.get(i));
		}
	}

	/**
	 * Remove an object from the queue, seize the specified resources, and pass to the next component
	 * @param i = position in the queue of the object to be removed
	 */
	public void processQueuedEntity(int i) {

		if( this.checkResources() ) {
			this.seizeResources();
			DisplayEntity ent = waitQueue.getValue().remove(i);
			this.sendToNextComponent( ent );
		}
	}

	public Queue getQueue() {
		return waitQueue.getValue();
	}

	/**
	 * Is the specified Resource required by this Seize object?
	 * @param res = the specified Resource.
	 * @return = TRUE if the Resource is required.
	 */
	public boolean requiresResource(Resource res) {
		return resourceList.getValue().contains(res);
	}

}
