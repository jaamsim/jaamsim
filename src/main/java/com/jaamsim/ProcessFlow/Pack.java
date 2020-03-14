/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.StringProviders.StringProvInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InterfaceEntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.states.StateEntity;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

public class Pack extends LinkedService {

	@Keyword(description = "The prototype for EntityContainers to be generated. "
	                     + "The generated EntityContainers will be copies of this entity.",
	         exampleList = {"EntityContainer1"})
	protected final InterfaceEntityInput<EntContainer> prototypeEntityContainer;

	@Keyword(description = "The number of entities to pack into the container.",
	         exampleList = {"2", "DiscreteDistribution1", "'1 + [TimeSeries1].PresentValue'"})
	protected final SampleInput numberOfEntities;

	@Keyword(description = "The service time required to pack each entity in the container.",
	         exampleList = { "3.0 h", "ExponentialDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput serviceTime;

	@Keyword(description = "The state to be assigned to container on arrival at this object.\n"
                         + "No state is assigned if the entry is blank.",
	         exampleList = {"Service"})
	protected final StringProvInput containerStateAssignment;

	@Keyword(description = "The minimum number of entities required to start packing.",
	         exampleList = {"2", "DiscreteDistribution1", "'1 + [TimeSeries1].PresentValue'"})
	private final SampleInput numberToStart;

	@Keyword(description = "If TRUE, the EntityContainer will be held in its queue until "
	                     + "sufficient entities are available to start packing.",
	         exampleList = {"TRUE"})
	private final BooleanInput waitForEntities;

	protected EntContainer container;	// the generated EntityContainer
	private int numberGenerated;  // Number of EntityContainers generated so far
	private int numberInserted;   // Number of entities inserted to the EntityContainer
	private int numberToInsert;   // Number of entities to insert in the present EntityContainer
	private boolean startedPacking;  // True if the packing process has already started
	private DisplayEntity packedEntity;  // the entity being packed

	{
		prototypeEntityContainer = new InterfaceEntityInput<>(EntContainer.class, "PrototypeEntityContainer", KEY_INPUTS, null);
		prototypeEntityContainer.setRequired(true);
		this.addInput(prototypeEntityContainer);

		numberOfEntities = new SampleInput("NumberOfEntities", KEY_INPUTS, new SampleConstant(1.0));
		numberOfEntities.setUnitType(DimensionlessUnit.class);
		numberOfEntities.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(numberOfEntities);

		serviceTime = new SampleInput("ServiceTime", KEY_INPUTS, new SampleConstant(TimeUnit.class, 0.0));
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(serviceTime);

		containerStateAssignment = new StringProvInput("ContainerStateAssignment", OPTIONS, null);
		containerStateAssignment.setUnitType(DimensionlessUnit.class);
		this.addInput(containerStateAssignment);

		numberToStart = new SampleInput("NumberToStart", OPTIONS, null);
		numberToStart.setUnitType(DimensionlessUnit.class);
		numberToStart.setDefaultText("NumberOfEntities Input");
		numberToStart.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(numberToStart);

		waitForEntities = new BooleanInput("WaitForEntities", OPTIONS, false);
		this.addInput(waitForEntities);
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

	protected boolean isContainerAvailable() {
		return true;
	}

	protected EntContainer getNextContainer() {
		numberGenerated++;
		DisplayEntity proto = (DisplayEntity)prototypeEntityContainer.getValue();
		StringBuilder sb = new StringBuilder();
		sb.append(this.getName()).append("_").append(numberGenerated);
		DisplayEntity ret = InputAgent.generateEntityWithName(getJaamSimModel(), proto.getClass(), sb.toString());
		Entity.fastCopyInputs(proto, ret);
		ret.earlyInit();
		return (EntContainer)ret;
	}

	private void setContainerState() {
		if (!containerStateAssignment.isDefault()) {
			double simTime = getSimTime();
			String state = containerStateAssignment.getValue().getNextString(simTime);
			container.setPresentState(state);
		}
	}

	@Override
	protected boolean startProcessing(double simTime) {

		// If necessary, get a new container
		if (container == null && !waitForEntities.getValue() && isContainerAvailable()) {
			container = this.getNextContainer();
			numberInserted = 0;
			setContainerState();
		}

		// Are there sufficient entities in the queue to start packing?
		if (!startedPacking) {
			String m = this.getNextMatchValue(simTime);
			numberToInsert = this.getNumberToInsert(simTime);
			if (getQueue().getMatchCount(m) < getNumberToStart(simTime)) {
				return false;
			}

			// If necessary, get a new container
			if (container == null) {
				if (!isContainerAvailable())
					return false;
				container = this.getNextContainer();
				numberInserted = 0;
				setContainerState();
			}
			startedPacking = true;
			this.setMatchValue(m);
		}

		// Select the next entity to pack and set its state
		if (numberInserted < numberToInsert) {
			if (getQueue().getMatchCount(getMatchValue()) == 0)
				return false;
			packedEntity = this.getNextEntityForMatch(getMatchValue());
			if (!stateAssignment.isDefault() && packedEntity instanceof StateEntity) {
				String state = stateAssignment.getValue().getNextString(simTime);
				((StateEntity)packedEntity).setPresentState(state);
			}
		}
		return true;
	}

	@Override
	protected void processStep(double simTime) {

		// Remove the next entity from the queue and pack the container
		if (packedEntity != null) {
			container.addEntity(packedEntity);
			packedEntity = null;
			numberInserted++;
		}

		// If the container is full, send it to the next component
		if (numberInserted >= numberToInsert) {
			this.sendToNextComponent((DisplayEntity)container);
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

	private int getNumberToStart(double simTime) {
		int ret = numberToInsert;
		if (!numberToStart.isDefault() && numberToInsert > 0) {
			ret = (int) numberToStart.getValue().getNextSample(simTime);
			ret = Math.max(ret, 1);
		}
		return ret;
	}

	@Override
	protected double getStepDuration(double simTime) {
		return serviceTime.getValue().getNextSample(simTime);
	}

	@Override
	public boolean isFinished() {
		return container == null;
	}

	@Override
	public void thresholdChanged() {

		// If an immediate release closure, stop packing and release the container
		if (isImmediateReleaseThresholdClosure()) {
			numberToInsert = 0;
			if (!isBusy() && !isFinished()) {
				processStep(getSimTime());
				return;
			}
		}

		super.thresholdChanged();
	}

	@Override
	public void updateGraphics(double simTime) {
		if (container != null)
			moveToProcessPosition((DisplayEntity)container);
		if (packedEntity != null)
			moveToProcessPosition(packedEntity);
	}

	@Output(name = "Container",
	 description = "The EntityContainer that is being filled.")
	public DisplayEntity getContainer(double simTime) {
		return (DisplayEntity)container;
	}

}
