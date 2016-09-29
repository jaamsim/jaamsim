/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.OverlayEntity;
import com.jaamsim.Graphics.TextBasics;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Input;
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
	         exampleList = { "3.0 h", "ExponentialDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput firstArrivalTime;

	@Keyword(description = "The inter-arrival time between generated entities.\n" +
			"A constant value, a distribution to be sampled, or a time series can be entered.",
	         exampleList = { "3.0 h", "ExponentialDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput interArrivalTime;

	@Keyword(description = "The number of entities to be generated for each arrival.\n" +
			"A constant value, a distribution to be sampled, or a time series can be entered.",
	         exampleList = {"3", "TimeSeries1", "'1 + 2*[DiscreteDistribution1].Value'"})
	private final SampleInput entitiesPerArrival;

	@Keyword(description = "The prototype for entities to be generated.\n" +
			"The generated entities will be copies of this entity.",
	         exampleList = {"Proto"})
	private final EntityInput<DisplayEntity> prototypeEntity;

	@Keyword(description = "The maximum number of entities to be generated.",
	         exampleList = {"3", "InputValue1", "[InputValue1].Value"})
	private final SampleInput maxNumber;

	private int numberGenerated = 0;  // Number of entities generated so far

	{
		defaultEntity.setHidden(true);
		stateAssignment.setHidden(true);
		waitQueue.setHidden(true);
		match.setHidden(true);
		processPosition.setHidden(true);
		opportunisticMaintenanceList.setHidden(true);
		opportunisticBreakdownList.setHidden(true);

		firstArrivalTime = new SampleInput("FirstArrivalTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
		firstArrivalTime.setUnitType(TimeUnit.class);
		firstArrivalTime.setEntity(this);
		firstArrivalTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(firstArrivalTime);

		interArrivalTime = new SampleInput("InterArrivalTime", "Key Inputs", new SampleConstant(TimeUnit.class, 1.0));
		interArrivalTime.setUnitType(TimeUnit.class);
		interArrivalTime.setEntity(this);
		interArrivalTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(interArrivalTime);

		entitiesPerArrival = new SampleInput("EntitiesPerArrival", "Key Inputs", new SampleConstant(DimensionlessUnit.class, 1.0));
		entitiesPerArrival.setUnitType(DimensionlessUnit.class);
		entitiesPerArrival.setEntity(this);
		entitiesPerArrival.setValidRange(1, Double.POSITIVE_INFINITY);
		this.addInput(entitiesPerArrival);

		prototypeEntity = new EntityInput<>(DisplayEntity.class, "PrototypeEntity", "Key Inputs", null);
		prototypeEntity.setRequired(true);
		ArrayList<Class<? extends Entity>> list = new ArrayList<>();
		list.add(TextBasics.class);
		list.add(OverlayEntity.class);
		prototypeEntity.setInvalidClasses(list);
		this.addInput(prototypeEntity);

		maxNumber = new SampleInput("MaxNumber", "Key Inputs", null);
		maxNumber.setUnitType(DimensionlessUnit.class);
		maxNumber.setEntity(this);
		maxNumber.setValidRange(1, Double.POSITIVE_INFINITY);
		maxNumber.setDefaultText(Input.POSITIVE_INFINITY);
		this.addInput(maxNumber);
	}

	public EntityGenerator() {}

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
		this.startStep();
	}

	@Override
	protected boolean startProcessing(double simTime) {

		// Stop if the last entity been generated
		return (maxNumber.getValue() == null
				|| numberGenerated < maxNumber.getValue().getNextSample(simTime));
	}

	@Override
	protected void endProcessing(double simTime) {

		// Do any of the thresholds stop the generator?
		if (!this.isOpen()) {
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
	}

	@Override
	protected double getStepDuration(double simTime) {

		// Use a separate input for the first arrival time
		if (numberGenerated == 0)
			return firstArrivalTime.getValue().getNextSample(simTime);

		return interArrivalTime.getValue().getNextSample(simTime);
	}

	@Override
	public long getNumberInProgress() {
		return  0;
	}

	@Output(name = "NumberGenerated",
	 description = "The total number of entities generated, including the initialization period.",
	    unitType = DimensionlessUnit.class)
	public int getNumberGenerated(double simTime) {
		return numberGenerated;
	}

}
