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

import com.jaamsim.Samples.SampleInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.EntityTarget;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.Process;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.Queue;

/**
 * Server processes entities one by one from a queue.  When finished with an entity, it passes it to the next
 * LinkedEntity in the chain.
 */
public class Server extends LinkedComponent {

	@Keyword(description = "The probability distribution object used to select the service time for each DisplayEntity.",
	         example = "Server1 ServiceTimeDistribution { Dist1 }")
	private final SampleInput serviceTimeDistributionInput;

	@Keyword(description = "The queue in which the waiting DisplayEntities will be placed.",
	         example = "Server1 WaitQueue { Queue1 }")
	private final EntityInput<Queue> waitQueueInput;

	private boolean busy;  				// true if the server is busy serving a DisplayEntity
	private DisplayEntity servedEntity;	// the DisplayEntity being server

	{
		serviceTimeDistributionInput = new SampleInput( "ServiceTimeDistribution", "Key Inputs", null);
		serviceTimeDistributionInput.setUnitType( TimeUnit.class );
		this.addInput( serviceTimeDistributionInput, true);

		waitQueueInput = new EntityInput<Queue>( Queue.class, "WaitQueue", "Key Inputs", null);
		this.addInput( waitQueueInput, true);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that probability distribution has been specified
		if( serviceTimeDistributionInput.getValue() == null ) {
			throw new InputErrorException( "The keyword ServiceTimeDistribution must be set." );
		}

		// Confirm that the target queue has been specified
		if( waitQueueInput.getValue() == null ) {
			throw new InputErrorException( "The keyword WaitQueue must be set." );
		}

		serviceTimeDistributionInput.verifyUnit();
		// Confirm that the next entity in the chain has been specified
		if( getNextComponent() == null ) {
			throw new InputErrorException( "The keyword NextEntity must be set." );
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		busy = false;
		if( servedEntity != null ) {
			servedEntity.kill();
		}
		servedEntity = null;
	}

	/**
	 * Add a DisplayEntity from upstream
	 */
	@Override
	public void addDisplayEntity( DisplayEntity ent ) {

		// Add the entity to the queue
		waitQueueInput.getValue().addLast( ent );

		// If necessary, wake up the server
		if ( !busy ) {
			Process.start(new ProcessEntitiesTarget(this));
		}
	}

	private static class ProcessEntitiesTarget extends EntityTarget<Server> {
		ProcessEntitiesTarget(Server ent) {
			super(ent, "processEntities");
		}

		@Override
		public void process() {
			ent.processEntities();
		}
	}

	/**
	* Process DisplayEntities from the Queue
	*/
	public void processEntities() {

		// Server should not be busy already
		if( busy ) {
			throw new ErrorException( "Server should not be busy already." );
		}
		busy = true;


		// Loop through the queued entities
		while( waitQueueInput.getValue().getCount() > 0 ) {

			// Remove the first entity from the queue
			servedEntity = waitQueueInput.getValue().removeFirst();
			this.setGraphicsDataDirty();

			// Select the processing time and wait for it to be completed
			double dt = serviceTimeDistributionInput.getValue().getNextSample(0.0);
			this.simWait( dt );

			// Increment the total number served
			numberProcessed++;

			// Send the entity to the next component in the chain
			getNextComponent().addDisplayEntity( servedEntity );
			servedEntity = null;
			this.setGraphicsDataDirty();
		}

		// Queue is empty, stop work
		busy = false;
	}

	@Override
	public void updateGraphics( double simTime ) {

		// If an entity is being served, show it at the center of the Server
		if( servedEntity != null ) {

			Vec3d serverCenter = this.getPositionForAlignment(new Vec3d());
			servedEntity.setPosition( serverCenter);

			// Set the alignment and orientation of the entity
			servedEntity.setAlignment( new Vec3d() );
			servedEntity.setOrientation( this.getOrientation() );
		}
	}
}
