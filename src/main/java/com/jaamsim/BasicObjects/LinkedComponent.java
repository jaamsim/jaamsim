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
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.StringInput;
import com.jaamsim.states.StateEntity;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.RateUnit;
import com.jaamsim.units.TimeUnit;

/**
 * LinkedComponents are used to form a chain of components that process DisplayEntities that pass through the system.
 * Sub-classes for EntityGenerator, Server, and EntitySink.
 */
public abstract class LinkedComponent extends StateEntity {

	@Keyword(description = "The prototype for entities that will be received by this object.\n" +
			"This input must be set if the expression 'this.obj' is used in the input to any keywords.",
	         exampleList = {"Proto"})
	protected final EntityInput<DisplayEntity> testEntity;

	@Keyword(description = "The next object to which the processed DisplayEntity is passed.",
			exampleList = {"Queue1"})
	protected final EntityInput<LinkedComponent> nextComponent;

	@Keyword(description = "The state to be assigned to each entity on arrival at this object.\n" +
			"No state is assigned if the entry is blank.",
	         exampleList = {"Service"})
	protected final StringInput stateAssignment;

	private int numberAdded;     // Number of entities added to this component from upstream
	private int numberProcessed; // Number of entities processed by this component
	private DisplayEntity receivedEntity; // Entity most recently received by this component
	private double releaseTime = Double.NaN;

	{
		attributeDefinitionList.setHidden(false);

		testEntity = new EntityInput<>(DisplayEntity.class, "TestEntity", "Key Inputs", null);
		this.addInput(testEntity);

		nextComponent = new EntityInput<>(LinkedComponent.class, "NextComponent", "Key Inputs", null);
		nextComponent.setRequired(true);
		this.addInput(nextComponent);

		stateAssignment = new StringInput("StateAssignment", "Key Inputs", "");
		this.addInput(stateAssignment);
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == testEntity) {
			receivedEntity = testEntity.getValue();
			return;
		}
	}

	@Override
	public void validate() {
		super.validate();

		// If a state is to be assigned, ensure that the prototype is a StateEntity
		if (testEntity.getValue() != null && !stateAssignment.getValue().isEmpty()) {
			if (!(testEntity.getValue() instanceof StateEntity)) {
				throw new InputErrorException("Only a SimEntity can be specified for the TestEntity keyword if a state is be be assigned.");
			}
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		numberAdded = 0;
		numberProcessed = 0;
		receivedEntity = testEntity.getValue();
		releaseTime = Double.NaN;
	}

	@Override
	public String getInitialState() {
		return "None";
	}

	@Override
	public boolean isValidState(String state) {
		return true;
	}

	/**
	 * Receives the specified entity from an upstream component.
	 * @param ent - the entity received from upstream.
	 */
	public void addEntity(DisplayEntity ent) {
		this.registerEntity(ent);
	}

	protected void registerEntity(DisplayEntity ent) {

		receivedEntity = ent;
		numberAdded++;

		// Assign a new state to the received entity
		if (!stateAssignment.getValue().isEmpty() && ent instanceof StateEntity)
			((StateEntity)ent).setPresentState(stateAssignment.getValue());
	}

	protected void setReceivedEntity(DisplayEntity ent) {
		receivedEntity = ent;
	}

	/**
	 * Sends the specified entity to the next component downstream.
	 * @param ent - the entity to be sent downstream.
	 */
	public void sendToNextComponent(DisplayEntity ent) {
		numberProcessed++;
		releaseTime = this.getSimTime();
		if( nextComponent.getValue() != null )
			nextComponent.getValue().addEntity(ent);
	}

	public int getNumberAdded() {
		return numberAdded;
	}

	public void incrementNumberProcessed() {
		numberProcessed++;
	}

	// ******************************************************************************************************
	// OUTPUT METHODS
	// ******************************************************************************************************

	@Output(name = "obj",
	 description = "The entity that was received most recently.",
	    sequence = 0)
	public DisplayEntity getReceivedEntity(double simTime) {
		return receivedEntity;
	}

	@Output(name = "NumberAdded",
	 description = "The number of entities received from upstream.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 1)
	public Double getNumberAdded(double simTime) {
		return (double)numberAdded;
	}

	@Output(name = "NumberProcessed",
	 description = "The number of entities processed by this component.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 2)
	public Double getNumberProcessed(double simTime) {
		return (double)numberProcessed;
	}

	@Output(name = "ProcessingRate",
	 description = "The number of entities processed per unit time by this component.",
	    unitType = RateUnit.class,
	    sequence = 3)
	public Double getProcessingRate( double simTime) {
		return numberProcessed/simTime;
	}

	@Output(name = "ReleaseTime",
	 description = "The time at which the last entity was released.",
	    unitType = TimeUnit.class,
	    sequence = 4)
	public Double getReleaseTime(double simTime) {
		return releaseTime;
	}

}
