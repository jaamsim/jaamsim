/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2025 JaamSim Software Inc.
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
import com.jaamsim.EntityProviders.EntityProvInput;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.OverlayEntity;
import com.jaamsim.Graphics.TextBasics;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Output;
import com.jaamsim.input.StringInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

/**
 * EntityGenerator creates sequence of DisplayEntities at random intervals, which are placed in a target Queue.
 */
public class EntityGenerator extends LinkedService implements EntityGen {

	@Keyword(description = "The arrival time for the first generated entity.",
	         exampleList = { "3.0 h", "ExponentialDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput firstArrivalTime;

	@Keyword(description = "The inter-arrival time between generated entities.",
	         exampleList = { "3.0 h", "ExponentialDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput interArrivalTime;

	@Keyword(description = "The number of entities to be generated for each arrival.",
	         exampleList = {"3", "TimeSeries1", "'1 + 2*[DiscreteDistribution1].Value'"})
	private final SampleInput entitiesPerArrival;

	@Keyword(description = "The prototype for entities to be generated. "
	                     + "The generated entities will be copies of this entity.",
	         exampleList = {"Proto", "'choose( this.NumberGenerated%2+1, [Proto1], [Proto2])'"})
	private final EntityProvInput<DisplayEntity> prototypeEntity;

	@Keyword(description = "The base for the names assigned to the generated entities. "
	                     + "The generated entities will be named Name1, Name2, etc.",
	         exampleList = {"Customer", "Package"})
	private final StringInput baseName;

	@Keyword(description = "The maximum number of entities to be generated.",
	         exampleList = {"3", "InputValue1", "[InputValue1].Value"})
	private final SampleInput maxNumber;

	@Keyword(description = "The number of entities to be generated simultaneously at the start of the run.",
	         exampleList = {"3", "InputValue1", "[InputValue1].Value"})
	private final SampleInput initialNumber;

	private int numberGenerated = 0;  // Number of entities generated so far
	private double presentIAT;

	{
		defaultEntity.setHidden(true);
		stateAssignment.setHidden(true);
		waitQueue.setHidden(true);
		match.setHidden(true);
		watchList.setHidden(true);
		processPosition.setHidden(true);
		opportunisticMaintenanceList.setHidden(true);
		opportunisticBreakdownList.setHidden(true);
		selectionCondition.setHidden(true);
		nextEntity.setHidden(true);

		firstArrivalTime = new SampleInput("FirstArrivalTime", KEY_INPUTS, 0.0d);
		firstArrivalTime.setUnitType(TimeUnit.class);
		firstArrivalTime.setValidRange(0, Double.POSITIVE_INFINITY);
		firstArrivalTime.setOutput(true);
		this.addInput(firstArrivalTime);

		interArrivalTime = new SampleInput("InterArrivalTime", KEY_INPUTS, 1.0d);
		interArrivalTime.setUnitType(TimeUnit.class);
		interArrivalTime.setValidRange(0, Double.POSITIVE_INFINITY);
		interArrivalTime.setOutput(true);
		this.addInput(interArrivalTime);

		entitiesPerArrival = new SampleInput("EntitiesPerArrival", KEY_INPUTS, 1);
		entitiesPerArrival.setUnitType(DimensionlessUnit.class);
		entitiesPerArrival.setIntegerValue(true);
		entitiesPerArrival.setValidRange(0, Double.POSITIVE_INFINITY);
		entitiesPerArrival.setOutput(true);
		this.addInput(entitiesPerArrival);

		prototypeEntity = new EntityProvInput<>(DisplayEntity.class, "PrototypeEntity", KEY_INPUTS, null);
		prototypeEntity.setRequired(true);
		prototypeEntity.addInvalidClass(TextBasics.class);
		prototypeEntity.addInvalidClass(OverlayEntity.class);
		this.addInput(prototypeEntity);

		baseName = new StringInput("BaseName", KEY_INPUTS, null);
		baseName.setDefaultText("Generator Name");
		this.addInput(baseName);

		maxNumber = new SampleInput("MaxNumber", KEY_INPUTS, Double.POSITIVE_INFINITY);
		maxNumber.setUnitType(DimensionlessUnit.class);
		maxNumber.setIntegerValue(true);
		maxNumber.setValidRange(0, Double.POSITIVE_INFINITY);
		maxNumber.setDefaultText(Input.POSITIVE_INFINITY);
		maxNumber.setOutput(true);
		this.addInput(maxNumber);

		initialNumber = new SampleInput("InitialNumber", KEY_INPUTS, 0);
		initialNumber.setUnitType(DimensionlessUnit.class);
		initialNumber.setIntegerValue(true);
		initialNumber.setValidRange(0, Double.POSITIVE_INFINITY);
		initialNumber.setOutput(true);
		this.addInput(initialNumber);
	}

	public EntityGenerator() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		numberGenerated = 0;
		presentIAT = 0.0d;
	}

	@Override
	public void addEntity( DisplayEntity ent ) {
		error("An entity cannot be sent to an EntityGenerator.");
	}

	@Override
	public void startUp() {
		super.startUp();

		// Start generating entities
		this.restart();
	}

	@Override
	protected boolean startProcessing(double simTime) {

		// Stop if the last entity been generated
		if (!maxNumber.isDefault()
				&& numberGenerated >= maxNumber.getNextSample(this, simTime))
			return false;

		// Select the inter-arrival time for the next entity
		int initNumber = (int) initialNumber.getNextSample(this, simTime);
		if (numberGenerated < initNumber)
			presentIAT = 0.0d;
		else if (numberGenerated == initNumber)
			presentIAT = firstArrivalTime.getNextSample(this, simTime);
		else
			presentIAT = interArrivalTime.getNextSample(this, simTime);

		if (presentIAT == Double.POSITIVE_INFINITY)
			return false;

		return true;
	}

	@Override
	protected void processStep(double simTime) {

		// Do any of the thresholds stop the generator?
		if (!this.isOpen()) {
			return;
		}

		// Set the name for the entities
		String name = baseName.getValue();
		if (name == null) {
			name = this.getName() + "_";
			name = name.replace(".", "_");
		}

		// Create the new entities
		int num = (int) entitiesPerArrival.getNextSample(this, EventManager.simSeconds());
		for (int i=0; i<num; i++) {
			DisplayEntity proto = prototypeEntity.getNextEntity(this, simTime);
			numberGenerated++;
			String entName = name + numberGenerated;
			DisplayEntity ent = (DisplayEntity) InputAgent.getGeneratedClone(proto, entName);
			ent.earlyInit();
			ent.lateInit();

			// Set the obj output to the assembled part
			receiveEntity(ent);
			setEntityState(ent);

			// Assign attributes
			assignAttributesAtStart(simTime);

			// Send the entity to the next element in the chain
			this.sendToNextComponent(ent);
		}
	}

	@Override
	protected double getStepDuration(double simTime) {
		return presentIAT;
	}

	@Override
	public boolean isFinished() {
		return true;  // can always stop when isFinished is called in startStep
	}

	@Override
	public void setPrototypeEntity(DisplayEntity proto) {
		KeywordIndex kw = InputAgent.formatArgs(prototypeEntity.getKeyword(), proto.getName());
		getJaamSimModel().storeAndExecute(new KeywordCommand(this, kw));
	}

	@Override
	public ArrayList<DisplayEntity> getSourceEntities() {
		ArrayList<DisplayEntity> ret = super.getSourceEntities();
		try {
			DisplayEntity ent = prototypeEntity.getNextEntity(this, 0.0d);
			if (ent != null) {
				ret.add(ent);
			}
		}
		catch (Exception e) {}
		return ret;
	}

	@Override
	// Delete 'MatchValue' output
	public String getMatchValue(double simTime) {
		return null;
	}

	@Output(name = "NumberGenerated",
	 description = "The total number of entities generated, including the initialization period.",
	    unitType = DimensionlessUnit.class,
	    sequence = 1)
	public int getNumberGenerated(double simTime) {
		return numberGenerated;
	}

	@Output(name = "PresentIAT",
	 description = "The total working time required before the next entity is created.",
	    unitType = TimeUnit.class,
	    sequence = 2)
	public double getPresentIAT(double simTime) {
		return presentIAT;
	}

	@Output(name = "ElapsedTime",
	 description = "The working time that has been completed towards the creation of the next "
	             + "entity.",
	    unitType = TimeUnit.class,
	    sequence = 3)
	public double getElapsedTime(double simTime) {
		return presentIAT - getRemainingDuration(simTime);
	}

}
