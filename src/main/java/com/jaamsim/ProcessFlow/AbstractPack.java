/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.StringProviders.StringProvInput;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

public abstract class AbstractPack extends LinkedService {

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
	private int numberInserted;   // Number of entities inserted to the EntityContainer
	private int numberToInsert;   // Number of entities to insert in the present EntityContainer
	private int numberToStartPacking;  // Number of entities required before packing can begin
	private boolean startedPacking;  // True if the packing process has already started
	private DisplayEntity packedEntity;  // the entity being packed

	{
		numberOfEntities = new SampleInput("NumberOfEntities", KEY_INPUTS, 1);
		numberOfEntities.setUnitType(DimensionlessUnit.class);
		numberOfEntities.setIntegerValue(true);
		numberOfEntities.setValidRange(1, Double.POSITIVE_INFINITY);
		this.addInput(numberOfEntities);

		serviceTime = new SampleInput("ServiceTime", KEY_INPUTS, 0.0d);
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(serviceTime);

		containerStateAssignment = new StringProvInput("ContainerStateAssignment", OPTIONS, null);
		containerStateAssignment.setUnitType(DimensionlessUnit.class);
		this.addInput(containerStateAssignment);

		numberToStart = new SampleInput("NumberToStart", OPTIONS, null);
		numberToStart.setUnitType(DimensionlessUnit.class);
		numberToStart.setIntegerValue(true);
		numberToStart.setDefaultText("NumberOfEntities Input");
		numberToStart.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(numberToStart);

		waitForEntities = new BooleanInput("WaitForEntities", OPTIONS, false);
		this.addInput(waitForEntities);
	}

	public AbstractPack() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		container = null;
		numberInserted = 0;
		numberToInsert = -1;
		numberToStartPacking = -1;
		startedPacking = false;
		packedEntity = null;
	}

	protected abstract boolean isContainerAvailable();

	protected abstract EntContainer getNextContainer();

	private void setContainerState() {
		if (!containerStateAssignment.isDefault()) {
			double simTime = getSimTime();
			String state = containerStateAssignment.getNextString(this, simTime);
			container.setPresentState(state);
		}
	}

	@Override
	protected boolean startProcessing(double simTime) {

		// A container must be available before packing can start
		if (container == null) {
			if (!isContainerAvailable())
				return false;

			// If necessary, get a new container
			if (!waitForEntities.getValue()) {
				container = this.getNextContainer();
				setContainerState();
			}
		}

		// Are there sufficient entities in the queue to start packing?
		if (!startedPacking) {
			String m = this.getNextMatchValue(simTime);
			if (numberToStartPacking < 0) {
				numberToInsert = this.getNumberToInsert(simTime);
				numberToStartPacking = this.getNumberToStart(simTime);
			}
			if (getQueue(simTime).getCount(m) < numberToStartPacking) {
				return false;
			}

			// If necessary, get a new container
			if (container == null) {
				container = this.getNextContainer();
				setContainerState();
			}

			// Start packing
			numberInserted = 0;
			startedPacking = true;
			this.setMatchValue(m);
		}

		// Select the next entity to pack and set its state
		if (numberInserted < numberToInsert) {
			packedEntity = this.removeNextEntity(getMatchValue());
			if (packedEntity == null)
				return false;

			receiveEntity(packedEntity);
			setEntityState(packedEntity);
			assignAttributesAtStart(simTime);
		}
		return true;
	}

	@Override
	protected void processStep(double simTime) {

		// Remove the next entity from the queue and pack the container
		if (packedEntity != null) {
			container.addEntity(packedEntity);
			releaseEntity(simTime);
			packedEntity = null;
			numberInserted++;
		}

		// If the container is full, send it to the next component
		if (numberInserted >= numberToInsert) {
			getNextComponent().addEntity((DisplayEntity) container);
			container = null;
			numberInserted = 0;
			numberToInsert = -1;
			numberToStartPacking = -1;
			startedPacking = false;
		}
	}

	protected int getNumberToInsert(double simTime) {
		return (int) numberOfEntities.getNextSample(this, simTime);
	}

	private int getNumberToStart(double simTime) {
		int ret = numberToInsert;
		if (!numberToStart.isDefault() && numberToInsert > 0) {
			ret = (int) numberToStart.getNextSample(this, simTime);
			ret = Math.max(ret, 1);
		}
		return ret;
	}

	@Override
	protected double getStepDuration(double simTime) {
		return serviceTime.getNextSample(this, simTime);
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
		super.updateGraphics(simTime);

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
