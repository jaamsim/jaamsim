/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016-2020 JaamSim Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.ProcessFlow;

import java.util.ArrayList;

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.StringProviders.StringProvInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.InterfaceEntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Output;
import com.jaamsim.states.StateEntity;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.RateUnit;
import com.jaamsim.units.TimeUnit;

public abstract class LinkedDevice extends Device implements Linkable {

	@Keyword(description = "The default value for the output obj.\n"
	                     + "Normally, obj is set to the last entity received by this object. "
	                     + "Prior to receiving its first entity, obj is set to the object "
	                     + "provided by DefaultEntity. If an input for DefaultEntity is not "
	                     + "provided, then obj is set to null until the first entity is received.",
	         exampleList = {"SimEntity1"})
	protected final EntityInput<DisplayEntity> defaultEntity;

	@Keyword(description = "The next object to which the processed DisplayEntity is passed.",
	         exampleList = {"Queue1"})
	protected final InterfaceEntityInput<Linkable> nextComponent;

	@Keyword(description = "The state to be assigned to each entity on arrival at this object.\n"
                         + "No state is assigned if the entry is blank.",
	         exampleList = {"Service"})
	protected final StringProvInput stateAssignment;

	private ProcessorData processor = new ProcessorData();

	{
		attributeDefinitionList.setHidden(false);
		workingStateListInput.setHidden(true);

		defaultEntity = new EntityInput<>(DisplayEntity.class, "DefaultEntity", KEY_INPUTS, null);
		this.addInput(defaultEntity);
		this.addSynonym(defaultEntity, "TestEntity");
		defaultEntity.setHidden(true);

		nextComponent = new InterfaceEntityInput<>(Linkable.class, "NextComponent", KEY_INPUTS, null);
		nextComponent.setRequired(true);
		this.addInput(nextComponent);

		stateAssignment = new StringProvInput("StateAssignment", OPTIONS, null);
		stateAssignment.setUnitType(DimensionlessUnit.class);
		this.addInput(stateAssignment);
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == defaultEntity) {
			processor.setReceivedEntity(defaultEntity.getValue());
			return;
		}
	}

	@Override
	public void validate() {
		super.validate();

		// If a state is to be assigned, ensure that the prototype is a StateEntity
		if (defaultEntity.getValue() != null && !stateAssignment.isDefault()) {
			if (!(defaultEntity.getValue() instanceof StateEntity)) {
				throw new InputErrorException("Only a SimEntity can be specified for the TestEntity keyword if a state is be be assigned.");
			}
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		processor.clear();
	}

	@Override
	public boolean isValidState(String state) {
		return true;
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		this.registerEntity(ent);
	}

	protected void registerEntity(DisplayEntity ent) {
		receiveEntity(ent);
		setEntityState(ent);
	}

	protected void receiveEntity(DisplayEntity ent) {
		processor.receiveEntity(ent);
	}

	protected void setReceivedEntity(DisplayEntity ent) {
		processor.setReceivedEntity(ent);
	}

	protected void releaseEntity(double simTime) {
		processor.releaseEntity(simTime);
	}

	/**
	 * Sends the specified entity to the next component downstream.
	 * @param ent - the entity to be sent downstream.
	 */
	public void sendToNextComponent(DisplayEntity ent) {
		releaseEntity(getSimTime());
		if( nextComponent.getValue() != null )
			nextComponent.getValue().addEntity(ent);
	}

	protected void setEntityState(DisplayEntity ent) {
		if (stateAssignment.isDefault() || !(ent instanceof StateEntity))
			return;
		String state = stateAssignment.getValue().getNextString(getSimTime());
		((StateEntity) ent).setPresentState(state);
	}

	@Override
	public void clearStatistics() {
		super.clearStatistics();
		processor.clearStatistics();
	}

	@Override
	public void linkTo(DisplayEntity nextEnt) {
		if (nextComponent.getHidden() || !(nextEnt instanceof Linkable)
				|| nextEnt instanceof EntityGenerator) {
			return;
		}

		ArrayList<String> toks = new ArrayList<>();
		toks.add(nextEnt.getName());
		KeywordIndex kw = new KeywordIndex(nextComponent.getKeyword(), toks, null);
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

	@Override
	public ArrayList<DisplayEntity> getDestinationEntities() {
		ArrayList<DisplayEntity> ret = new ArrayList<>();
		Linkable l = nextComponent.getValue();
		if (l != null && (l instanceof DisplayEntity)) {
			ret.add((DisplayEntity)l);
		}
		return ret;
	}

	// ******************************************************************************************************
	// OUTPUT METHODS
	// ******************************************************************************************************

	@Output(name = "obj",
	 description = "The entity that was received most recently.",
	    sequence = 0)
	public DisplayEntity getReceivedEntity(double simTime) {
		return processor.getReceivedEntity();
	}

	@Output(name = "NumberAdded",
	 description = "The number of entities received from upstream after the initialization period.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 1)
	public long getNumberAdded(double simTime) {
		return processor.getNumberReceived();
	}

	@Output(name = "NumberProcessed",
	 description = "The number of entities processed by this component after the initialization period.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 2)
	public long getNumberProcessed(double simTime) {
		return processor.getNumberProcessed();
	}

	@Output(name = "NumberInProgress",
	 description = "The number of entities that have been received but whose processing has not been completed yet.",
	    unitType = DimensionlessUnit.class,
	    sequence = 3)
	public long getNumberInProgress(double simTime) {
		return  processor.getNumberInProgress();
	}

	@Output(name = "ProcessingRate",
	 description = "The number of entities processed per unit time by this component after the initialization period.",
	    unitType = RateUnit.class,
	    sequence = 4)
	public double getProcessingRate(double simTime) {
		double dur = simTime - getSimulation().getInitializationTime();
		if (dur <= 0.0)
			return 0.0;
		return processor.getNumberProcessed()/dur;
	}

	@Output(name = "ReleaseTime",
	 description = "The time at which the last entity was released.",
	    unitType = TimeUnit.class,
	    sequence = 5)
	public double getReleaseTime(double simTime) {
		return processor.getReleaseTime();
	}

}
