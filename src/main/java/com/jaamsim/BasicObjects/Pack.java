/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

public class Pack extends LinkedService {

	@Keyword(description = "The prototype for EntityContainers to be generated.\n" +
			"The generated EntityContainers will be copies of this entity.",
	         exampleList = {"EntityContainer1"})
	protected final EntityInput<EntityContainer> prototypeEntityContainer;

	@Keyword(description = "The number of entities to pack into the container.",
	         exampleList = {"2", "DiscreteDistribution1", "'1 + [TimeSeries1].PresentValue'"})
	private final SampleExpInput numberOfEntities;

	@Keyword(description = "The service time required to pack each entity in the container.",
	         exampleList = { "3.0 h", "ExponentialDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleExpInput serviceTime;

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

		numberOfEntities = new SampleExpInput("NumberOfEntities", "Key Inputs", new SampleConstant(1.0));
		numberOfEntities.setUnitType(DimensionlessUnit.class);
		numberOfEntities.setEntity(this);
		numberOfEntities.setValidRange(1, Double.POSITIVE_INFINITY);
		this.addInput(numberOfEntities);

		serviceTime = new SampleExpInput("ServiceTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
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
		sb.append(proto.getName()).append("_Copy").append(numberGenerated);
		EntityContainer ret = Entity.fastCopy(proto, sb.toString());
		ret.earlyInit();
		return ret;
	}

	@Override
	public void startAction() {

		// Do any of the thresholds stop the generator?
		if (!this.isOpen()) {
			this.setBusy(false);
			this.setPresentState();
			return;
		}

		// If necessary, get a new container
		if (container == null) {
			container = this.getNextContainer();
			numberInserted = 0;

			// Position the container over the pack object
			this.moveToProcessPosition(container);
		}

		// Are there sufficient entities in the queue to start packing?
		if (!startedPacking) {
			Integer m = this.getNextMatchValue(getSimTime());
			numberToInsert = (int) numberOfEntities.getValue().getNextSample(this.getSimTime());
			if (waitQueue.getValue().getMatchCount(m) < numberToInsert) {
				this.setBusy(false);
				this.setPresentState();
				return;
			}
			startedPacking = true;
			this.setMatchValue(m);
		}

		// Schedule the insertion of the next entity
		packedEntity = this.getNextEntityForMatch(getMatchValue());
		double dt = serviceTime.getValue().getNextSample(getSimTime());
		this.scheduleProcess(dt, 5, endActionTarget);
	}

	@Override
	public void endAction() {

		// Remove the next entity from the queue and pack the container
		container.addEntity(packedEntity);
		packedEntity = null;
		numberInserted++;

		// If the container is full, send it to the next component
		if (numberInserted == numberToInsert) {
			this.sendToNextComponent(container);
			container = null;
			numberInserted = 0;
			startedPacking = false;
		}

		// Insert the next entity
		this.startAction();
	}

	@Output(name = "Container",
	 description = "The EntityContainer that is being filled.")
	public DisplayEntity getContainer(double simTime) {
		return container;
	}

}
