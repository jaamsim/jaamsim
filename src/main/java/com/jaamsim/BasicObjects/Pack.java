/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.BasicObjects;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.states.StateEntity;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

public class Pack extends LinkedService {

	@Keyword(description = "The prototype for EntityContainers to be generated.\n" +
			"The generated EntityContainers will be copies of this entity.",
	         exampleList = {"EntityContainer1"})
	protected final EntityInput<EntityContainer> prototypeEntityContainer;

	@Keyword(description = "The number of entities to pack into the container.",
	         exampleList = {"2", "DiscreteDistribution1", "'1 + [TimeSeries1].PresentValue'"})
	protected final SampleInput numberOfEntities;

	@Keyword(description = "The service time required to pack each entity in the container.",
	         exampleList = { "3.0 h", "ExponentialDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput serviceTime;

	protected EntityContainer container;	// the generated EntityContainer
	private int numberGenerated;  // Number of EntityContainers generated so far
	private int numberInserted;   // Number of entities inserted to the EntityContainer
	private int numberToInsert;   // Number of entities to insert in the present EntityContainer
	private boolean startedPacking;  // True if the packing process has already started
	private DisplayEntity packedEntity;  // the entity being packed

	{
		prototypeEntityContainer = new EntityInput<>(EntityContainer.class, "PrototypeEntityContainer", "Key Inputs", null);
		prototypeEntityContainer.setRequired(true);
		this.addInput(prototypeEntityContainer);

		numberOfEntities = new SampleInput("NumberOfEntities", "Key Inputs", new SampleConstant(1.0));
		numberOfEntities.setUnitType(DimensionlessUnit.class);
		numberOfEntities.setEntity(this);
		numberOfEntities.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(numberOfEntities);

		serviceTime = new SampleInput("ServiceTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setEntity(this);
		serviceTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(serviceTime);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		container = null;
		numberGenerated = 0;
		numberInserted = 0;
		startedPacking = false;
		packedEntity = null;
	}

	protected EntityContainer getNextContainer() {
		numberGenerated++;
		EntityContainer proto = prototypeEntityContainer.getValue();
		StringBuilder sb = new StringBuilder();
		sb.append(this.getName()).append("_").append(numberGenerated);
		EntityContainer ret = Entity.fastCopy(proto, sb.toString());
		ret.earlyInit();
		return ret;
	}

	@Override
	protected boolean startProcessing(double simTime) {

		// If necessary, get a new container
		if (container == null) {
			container = this.getNextContainer();
			numberInserted = 0;

			// Set the state for the container and its contents
			if (!stateAssignment.getValue().isEmpty())
				container.setPresentState(stateAssignment.getValue());

			// Position the container over the pack object
			this.moveToProcessPosition(container);
		}

		// Are there sufficient entities in the queue to start packing?
		if (!startedPacking) {
			Integer m = this.getNextMatchValue(simTime);
			numberToInsert = this.getNumberToInsert(simTime);
			if (waitQueue.getValue().getMatchCount(m) < numberToInsert) {
				return false;
			}
			startedPacking = true;
			this.setMatchValue(m);
		}

		// Select the next entity to pack and set its state
		if (numberInserted < numberToInsert) {
			packedEntity = this.getNextEntityForMatch(getMatchValue());
			if (!stateAssignment.getValue().isEmpty() && packedEntity instanceof StateEntity)
				((StateEntity)packedEntity).setPresentState(stateAssignment.getValue());

			// Move the entity into position for processing
			this.moveToProcessPosition(packedEntity);
		}
		return true;
	}

	@Override
	protected void endProcessing(double simTime) {

		// Remove the next entity from the queue and pack the container
		if (packedEntity != null) {
			container.addEntity(packedEntity);
			packedEntity = null;
			numberInserted++;
		}

		// If the container is full, send it to the next component
		if (numberInserted >= numberToInsert) {
			this.sendToNextComponent(container);
			container = null;
			numberInserted = 0;
			startedPacking = false;
		}
	}

	protected int getNumberToInsert(double simTime) {
		int ret = (int)numberOfEntities.getValue().getNextSample(simTime);
		ret = Math.max(ret, 1);
		return ret;
	}

	@Override
	protected double getProcessingTime(double simTime) {
		return serviceTime.getValue().getNextSample(simTime);
	}

	@Output(name = "Container",
	 description = "The EntityContainer that is being filled.")
	public DisplayEntity getContainer(double simTime) {
		return container;
	}

}
