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

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.Thresholds.Threshold;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.EntityTarget;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.IntegerInput;
import com.sandwell.JavaSimulation3D.DisplayEntity;

/**
 * EntityGenerator creates sequence of DisplayEntities at random intervals, which are placed in a target Queue.
 */
public class EntityGenerator extends LinkedComponent {

	@Keyword(description = "The arrival time for the first generated entity.\n" +
			"A constant value, a distribution to be sampled, or a time series can be entered.",
	         example = "EntityGenerator1 FirstArrivalTime { 1.0 h }")
	private final SampleExpInput firstArrivalTime;

	@Keyword(description = "The inter-arrival time between generated entities.\n" +
			"A constant value, a distribution to be sampled, or a time series can be entered.",
	         example = "EntityGenerator1 InterArrivalTime { 1.5 h }")
	private final SampleExpInput interArrivalTime;

	@Keyword(description = "The prototype for entities to be generated.\n" +
			"The generated entities will be copies of this entity.",
	         example = "EntityGenerator1 PrototypeEntity { Ship }")
	private final EntityInput<DisplayEntity> prototypeEntity;

	@Keyword(description = "The maximum number of entities to be generated.\n" +
			"Default is no limit.",
	         example = "EntityGenerator1 MaxNumber { 3 }")
	private final IntegerInput maxNumber;

	private int numberGenerated = 0;  // Number of entities generated so far
	private boolean busy;


	{
		testEntity.setHidden(true);

		firstArrivalTime = new SampleExpInput( "FirstArrivalTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
		firstArrivalTime.setUnitType( TimeUnit.class );
		firstArrivalTime.setEntity(this);
		this.addInput( firstArrivalTime);

		interArrivalTime = new SampleExpInput( "InterArrivalTime", "Key Inputs", new SampleConstant(TimeUnit.class, 1.0));
		interArrivalTime.setUnitType( TimeUnit.class );
		interArrivalTime.setEntity(this);
		this.addInput( interArrivalTime);

		prototypeEntity = new EntityInput<DisplayEntity>( DisplayEntity.class, "PrototypeEntity", "Key Inputs", null);
		this.addInput( prototypeEntity);

		maxNumber = new IntegerInput( "MaxNumber", "Key Inputs", null);
		maxNumber.setValidRange(1, Integer.MAX_VALUE);
		this.addInput( maxNumber);
	}

	public EntityGenerator() {
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that prototype entity has been specified
		if( prototypeEntity.getValue() == null ) {
			throw new InputErrorException( "The keyword PrototypeEntity must be set." );
		}

		interArrivalTime.verifyUnit();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		numberGenerated = 0;
		busy = false;
	}

	@Override
	public void startUp() {
		super.startUp();

		// Generate the first entity and start the recursive loop to continue the process
		double dt = firstArrivalTime.getValue().getNextSample(0.0);
		this.scheduleProcess(dt, 5, new CreateNextEntityTarget(this, "createNextEntity"));
	}

	private static class CreateNextEntityTarget extends EntityTarget<EntityGenerator> {
		public CreateNextEntityTarget(EntityGenerator ent, String method) {
			super(ent, method);
		}

		@Override
		public void process() {
			ent.createNextEntity();
		}
	}

	@Override
	public void thresholdChanged() {

		// Is restart required?
		if (busy) return;

		// Are all the thresholds satisfied?
		for( Threshold thr : this.getThresholds() ) {
			if( thr.isClosed() ) return;
		}

		// Has the last entity been generated?
		if( maxNumber.getValue() != null && numberGenerated >= maxNumber.getValue() )
			return;

		// Restart entity creation
		double dt = interArrivalTime.getValue().getNextSample(getSimTime());
		this.scheduleProcess(dt, 5, new CreateNextEntityTarget(this, "createNextEntity"));
	}

	/**
	* Loop recursively to generate each entity
	*/
	public void createNextEntity() {

		// Do any of the thresholds stop the generator?
		for( Threshold thr : this.getThresholds() ) {
			if( thr.isClosed() ) {
				busy = false;
				return;
			}
		}

		busy = true;

		// Create the new entity
		numberGenerated++;
		DisplayEntity proto = prototypeEntity.getValue();
		StringBuilder sb = new StringBuilder();
		sb.append(proto.getInputName()).append("_Copy").append(numberGenerated);
		DisplayEntity ent = InputAgent.defineEntityWithUniqueName(proto.getClass(), sb.toString(),"_", true);
		ent.copyInputs(proto);
		ent.setFlag(Entity.FLAG_GENERATED);

		// Send the entity to the next element in the chain
		this.sendToNextComponent( ent );

		// Stop if the last entity been generated
		if( maxNumber.getValue() != null && numberGenerated >= maxNumber.getValue() )
			return;

		// Schedule the next entity to be generated
		double dt = interArrivalTime.getValue().getNextSample(getSimTime());
		this.scheduleProcess(dt, 5, new CreateNextEntityTarget(this, "createNextEntity"));
	}

	@Output(name = "NumberGenerated",
	        description = "The number of entities generated by this generator.",
	        unitType = DimensionlessUnit.class)
	public Double getNumberGenerated(double simTime) {
		return (double)numberGenerated;
	}
}
