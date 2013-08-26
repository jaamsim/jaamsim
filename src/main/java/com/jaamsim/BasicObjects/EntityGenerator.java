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
import com.jaamsim.input.InputAgent;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.EntityTarget;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.Process;
import com.sandwell.JavaSimulation3D.DisplayEntity;

/**
 * EntityGenerator creates sequence of DisplayEntities at random intervals, which are placed in a target Queue.
 */
public class EntityGenerator extends LinkedComponent {

	@Keyword(description = "The probability distribution object used to select the inter-arrival time between generated DisplayEntities.",
	         example = "EntityGenerator1 IATdistribution { Dist1 }")
	private final SampleInput iatDistributionInput;

	@Keyword(description = "The prototype for entities to be generated.\n" +
			"The generated entities will be copies of this entity.",
	         example = "EntityGenerator-1 PrototypeEntity { Ship }")
	private final EntityInput<DisplayEntity> prototypeEntityInput;

	{
		iatDistributionInput = new SampleInput( "IATdistribution", "Key Inputs", null);
		iatDistributionInput.setUnitType( TimeUnit.class );
		this.addInput( iatDistributionInput, true);

		prototypeEntityInput = new EntityInput<DisplayEntity>( DisplayEntity.class, "PrototypeEntity", "Key Inputs", null);
		this.addInput( prototypeEntityInput, true);
	}

	public EntityGenerator() {
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that probability distribution has been specified
		if( iatDistributionInput.getValue() == null ) {
			throw new InputErrorException( "The keyword IATdistribution must be set." );
		}

		// Confirm that prototype entity has been specified
		if( prototypeEntityInput.getValue() == null ) {
			throw new InputErrorException( "The keyword PrototypeEntity must be set." );
		}

		iatDistributionInput.verifyUnit();
	}

	@Override
	public void startUp() {
		super.startUp();

		// Start a new process for the Generator's main creation loop
		Process.start(new CreateEntitiesTarget(this));
	}

	private static class CreateEntitiesTarget extends EntityTarget<EntityGenerator> {
		public CreateEntitiesTarget(EntityGenerator ent) {
			super(ent, "createEntities");
		}

		@Override
		public void process() {
			ent.createEntities();
		}
	}

	/**
	* Loop endlessly creating DisplayEntities
	*/
	public void createEntities() {

		int numberGenerated = 0;

		while( true ) {

			// Determine the interarrival time for the next creation event
			double dt = iatDistributionInput.getValue().getNextSample(getSimTime());

			// Schedule the creation event at this time
			this.simWait( dt );

			// Create the new DisplayEntity
			numberGenerated++;
			DisplayEntity proto = prototypeEntityInput.getValue();
			String name = String.format("Copy_of_%s-%s", proto.getInputName(), numberGenerated);
			DisplayEntity ent = InputAgent.defineEntityWithUniqueName(proto.getClass(), name, true);
			ent.copyInputs(proto);
			ent.setFlag(Entity.FLAG_GENERATED);

			//  Send the entity to the next element in the chain
			this.sendToNextComponent( ent );
		}
	}

}
