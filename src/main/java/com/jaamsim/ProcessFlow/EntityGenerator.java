/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2022 JaamSim Software Inc.
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
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.basicsim.Entity;
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

		firstArrivalTime = new SampleInput("FirstArrivalTime", KEY_INPUTS, new SampleConstant(TimeUnit.class, 0.0));
		firstArrivalTime.setUnitType(TimeUnit.class);
		firstArrivalTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(firstArrivalTime);

		interArrivalTime = new SampleInput("InterArrivalTime", KEY_INPUTS, new SampleConstant(TimeUnit.class, 1.0));
		interArrivalTime.setUnitType(TimeUnit.class);
		interArrivalTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(interArrivalTime);

		entitiesPerArrival = new SampleInput("EntitiesPerArrival", KEY_INPUTS, new SampleConstant(1));
		entitiesPerArrival.setUnitType(DimensionlessUnit.class);
		entitiesPerArrival.setIntegerValue(true);
		entitiesPerArrival.setValidRange(1, Double.POSITIVE_INFINITY);
		this.addInput(entitiesPerArrival);

		prototypeEntity = new EntityProvInput<>(DisplayEntity.class, "PrototypeEntity", KEY_INPUTS, null);
		prototypeEntity.setRequired(true);
		prototypeEntity.addInvalidClass(TextBasics.class);
		prototypeEntity.addInvalidClass(OverlayEntity.class);
		this.addInput(prototypeEntity);

		baseName = new StringInput("BaseName", KEY_INPUTS, null);
		baseName.setDefaultText("Generator Name");
		this.addInput(baseName);

		maxNumber = new SampleInput("MaxNumber", KEY_INPUTS, null);
		maxNumber.setUnitType(DimensionlessUnit.class);
		maxNumber.setIntegerValue(true);
		maxNumber.setValidRange(1, Double.POSITIVE_INFINITY);
		maxNumber.setDefaultText(Input.POSITIVE_INFINITY);
		this.addInput(maxNumber);
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
		if (maxNumber.getValue() != null
				&& numberGenerated >= maxNumber.getValue().getNextSample(simTime))
			return false;

		// Select the inter-arrival time for the next entity
		if (numberGenerated == 0)
			presentIAT = firstArrivalTime.getValue().getNextSample(simTime);
		else
			presentIAT = interArrivalTime.getValue().getNextSample(simTime);

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
		int num = (int) entitiesPerArrival.getValue().getNextSample(getSimTime());
		for (int i=0; i<num; i++) {
			numberGenerated++;
			DisplayEntity proto = prototypeEntity.getValue().getNextEntity(simTime);
			StringBuilder sb = new StringBuilder();
			sb.append(name).append(numberGenerated);
			DisplayEntity ent = InputAgent.generateEntityWithName(getJaamSimModel(), proto.getClass(), sb.toString());
			Entity.fastCopyInputs(proto, ent);
			ent.earlyInit();

			// Set the obj output to the assembled part
			receiveEntity(ent);
			setEntityState(ent);

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
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

	@Override
	public ArrayList<DisplayEntity> getSourceEntities() {
		ArrayList<DisplayEntity> ret = new ArrayList<>();
		if (prototypeEntity.getValue() == null)
			return ret;
		DisplayEntity ent = prototypeEntity.getValue().getNextEntity(0.0d);
		if (ent != null) {
			ret.add(ent);
		}
		return ret;
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
		double ret = presentIAT - getRemainingDuration();
		if (isBusy()) {
			ret += simTime - getLastUpdateTime();
		}
		return ret;
	}

	@Output(name = "FractionCompleted",
	 description = "The portion of the total working time towards the creation of the next entity "
	             + "that has been completed.",
	    unitType = DimensionlessUnit.class,
	    sequence = 4)
	public double getFractionCompleted(double simTime) {
		if (presentIAT == 0.0d)
			return 0.0d;
		return getElapsedTime(simTime)/presentIAT;
	}

}
