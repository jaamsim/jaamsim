/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2024 JaamSim Software Inc.
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
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

public abstract class AbstractUnpack extends LinkedService {

	@Keyword(description = "An expression returning a string value that determines which of the "
	                     + "entities in the container are eligible to be removed. "
	                     + "If used, the only entities eligible for selection are the ones whose "
	                     + "inputs for the container's Match keyword are equal to value returned by "
	                     + "the expression entered for this Match keyword. "
	                     + "Expressions that return a dimensionless integer or an object are also "
	                     + "valid. The returned number or object is converted to a string "
	                     + "automatically. A floating point number is truncated to an integer.",
	         exampleList = {"this.obj.Attrib1"})
	private final StringProvInput matchForEntities;

	@Keyword(description = "The service time required to unpack each entity.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput serviceTime;

	@Keyword(description = "The state to be assigned to container on arrival at this object.\n"
	                     + "No state is assigned if the entry is blank.",
	         exampleList = {"Service"})
	protected final StringProvInput containerStateAssignment;

	private String entityMatch;   // Match value for the entities to be removed from the container
	private int numberToRemove;   // Number of entities to remove from the present EntityContainer
	private EntContainer container;	// the received EntityContainer
	private int numberRemoved;   // Number of entities removed from the received EntityContainer
	private DisplayEntity unpackedEntity;  // the entity being unpacked

	{
		matchForEntities = new StringProvInput("MatchForEntities", KEY_INPUTS, "");
		matchForEntities.setUnitType(DimensionlessUnit.class);
		this.addInput(matchForEntities);

		serviceTime = new SampleInput("ServiceTime", KEY_INPUTS, 0.0d);
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setValidRange(0, Double.POSITIVE_INFINITY);
		serviceTime.setOutput(true);
		this.addInput(serviceTime);

		containerStateAssignment = new StringProvInput("ContainerStateAssignment", OPTIONS, "");
		containerStateAssignment.setUnitType(DimensionlessUnit.class);
		this.addInput(containerStateAssignment);
	}


	public AbstractUnpack() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		entityMatch = "";
		numberToRemove = 0;
		container = null;
		numberRemoved = 0;
		unpackedEntity = null;
	}

	private void setContainerState() {
		if (!containerStateAssignment.isDefault()) {
			double simTime = getSimTime();
			String state = containerStateAssignment.getNextString(this, simTime);
			container.setPresentState(state);
		}
	}

	@Override
	protected boolean startProcessing(double simTime) {

		// If a container has not been started yet, remove one from the queue
		if (container == null) {

			// Set the match value for the container
			String m = getNextMatchValue(getSimTime());
			setMatchValue(m);

			// Remove the container from the queue
			container = (EntContainer)this.removeNextEntity(m);
			if (container == null)
				return false;

			setContainerState();

			// Set the match value for the entities to remove
			entityMatch = null;
			if (!matchForEntities.isDefault())
				entityMatch = matchForEntities.getNextString(this, simTime, 1.0d, true);

			// Set the number of entities to remove
			numberToRemove = getNumberToRemove();
			numberRemoved = 0;
		}

		// Remove the next entity to unpack and set its state
		if (numberRemoved < numberToRemove && !container.isEmpty(entityMatch)) {
			unpackedEntity = container.removeEntity(entityMatch);
			receiveEntity(unpackedEntity);
			setEntityState(unpackedEntity);
			assignAttributesAtStart(simTime);
		}

		return true;
	}

	protected abstract void disposeContainer(EntContainer c);

	protected abstract int getNumberToRemove();

	@Override
	protected void processStep(double simTime) {

		// Send the unpacked entity to the next component
		if (unpackedEntity != null) {
			sendToNextComponent(unpackedEntity);
			unpackedEntity = null;
			numberRemoved++;
		}

		// Stop when the desired number of entities have been removed
		if (numberRemoved >= numberToRemove || container.isEmpty(entityMatch)) {
			this.disposeContainer(container);
			container = null;
			numberRemoved = 0;
		}
	}

	@Override
	public boolean isFinished() {
		return container == null;
	}

	@Override
	public void thresholdChanged() {

		// If an immediate release closure, stop packing and release the container
		if (isImmediateReleaseThresholdClosure())
			numberToRemove = 0;

		super.thresholdChanged();
	}

	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);

		if (container != null)
			moveToProcessPosition((DisplayEntity)container);
		if (unpackedEntity != null)
			moveToProcessPosition(unpackedEntity);
	}

	@Override
	protected double getStepDuration(double simTime) {
		double dur = 0.0;
		if (unpackedEntity != null)
			dur = serviceTime.getNextSample(this, simTime);
		return dur;
	}

	@Output(name = "Container",
	 description = "The EntityContainer that is being unpacked.")
	public DisplayEntity getContainer(double simTime) {
		return (DisplayEntity)container;
	}

}
