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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

/**
 * EntityGenerator creates sequence of DisplayEntities at random intervals, which are placed in a target Queue.
 */
public class EntityGenerator extends LinkedService {

	@Keyword(description = "The arrival time for the first generated entity.\n" +
			"A constant value, a distribution to be sampled, or a time series can be entered.",
	         example = "EntityGenerator1 FirstArrivalTime { 1.0 h }")
	private final SampleExpInput firstArrivalTime;

	@Keyword(description = "The inter-arrival time between generated entities.\n" +
			"A constant value, a distribution to be sampled, or a time series can be entered.",
	         example = "EntityGenerator1 InterArrivalTime { 1.5 h }")
	private final SampleExpInput interArrivalTime;

	@Keyword(description = "The number of entities to be generated for each arrival.\n" +
			"A constant value, a distribution to be sampled, or a time series can be entered.",
	         example = "EntityGenerator1 EntitiesPerArrival { 3 }")
	private final SampleExpInput entitiesPerArrival;

	@Keyword(description = "The prototype for entities to be generated.\n" +
			"The generated entities will be copies of this entity.",
	         example = "EntityGenerator1 PrototypeEntity { Ship }")
	private final EntityInput<DisplayEntity> prototypeEntity;

	@Keyword(description = "The maximum number of entities to be generated.\n" +
			"Default is no limit.",
	         example = "EntityGenerator1 MaxNumber { 3 }")
	private final IntegerInput maxNumber;

	private int numberGenerated = 0;  // Number of entities generated so far

	{
		testEntity.setHidden(true);
		stateAssignment.setHidden(true);
		waitQueue.setHidden(true);

		firstArrivalTime = new SampleExpInput("FirstArrivalTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
		firstArrivalTime.setUnitType(TimeUnit.class);
		firstArrivalTime.setEntity(this);
		firstArrivalTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(firstArrivalTime);

		interArrivalTime = new SampleExpInput("InterArrivalTime", "Key Inputs", new SampleConstant(TimeUnit.class, 1.0));
		interArrivalTime.setUnitType(TimeUnit.class);
		interArrivalTime.setEntity(this);
		interArrivalTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(interArrivalTime);

		entitiesPerArrival = new SampleExpInput("EntitiesPerArrival", "Key Inputs", new SampleConstant(DimensionlessUnit.class, 1.0));
		entitiesPerArrival.setUnitType(DimensionlessUnit.class);
		entitiesPerArrival.setEntity(this);
		entitiesPerArrival.setValidRange(1, Double.POSITIVE_INFINITY);
		this.addInput(entitiesPerArrival);

		prototypeEntity = new EntityInput<>(DisplayEntity.class, "PrototypeEntity", "Key Inputs", null);
		this.addInput(prototypeEntity);

		maxNumber = new IntegerInput("MaxNumber", "Key Inputs", null);
		maxNumber.setValidRange(1, Integer.MAX_VALUE);
		this.addInput(maxNumber);
	}

	public EntityGenerator() {}

	@Override
	public void validate() {
		super.validate();

		// Confirm that prototype entity has been specified
		if (prototypeEntity.getValue() == null) {
			throw new InputErrorException("The keyword PrototypeEntity must be set.");
		}

		firstArrivalTime.validate();
		interArrivalTime.validate();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		numberGenerated = 0;
	}

	@Override
	public void addEntity( DisplayEntity ent ) {
		error("An entity cannot be sent to an EntityGenerator.");
	}

	@Override
	public void startUp() {
		super.startUp();

		// Start generating entities
		this.setBusy(true);
		this.setPresentState();
		this.startAction();
	}

	@Override
	public void startAction() {

		// Stop if the gate is closed or the last entity been generated
		if (!this.isOpen() || (maxNumber.getValue() != null && numberGenerated >= maxNumber.getValue())) {
			this.setBusy(false);
			this.setPresentState();
			return;
		}

		// Schedule the next entity to be generated
		double dt;
		if (numberGenerated == 0)
			dt = firstArrivalTime.getValue().getNextSample(getSimTime());
		else
			dt = interArrivalTime.getValue().getNextSample(getSimTime());
		this.scheduleProcess(dt, 5, endActionTarget);
	}

	@Override
	public void endAction() {

		// Do any of the thresholds stop the generator?
		if (!this.isOpen()) {
			this.setBusy(false);
			this.setPresentState();
			return;
		}

		// Create the new entities
		int num = (int) entitiesPerArrival.getValue().getNextSample(getSimTime());
		for (int i=0; i<num; i++) {
			numberGenerated++;
			DisplayEntity proto = prototypeEntity.getValue();
			StringBuilder sb = new StringBuilder();
			sb.append(this.getName()).append("_").append(numberGenerated);
			DisplayEntity ent = Entity.fastCopy(proto, sb.toString());
			ent.earlyInit();

			// Send the entity to the next element in the chain
			this.sendToNextComponent(ent);
		}

		// Try to generate another entity
		this.startAction();
	}

	@Output(name = "NumberGenerated",
	        description = "The number of entities generated by this generator.",
	        unitType = DimensionlessUnit.class)
	public Double getNumberGenerated(double simTime) {
		return (double)numberGenerated;
	}

}
