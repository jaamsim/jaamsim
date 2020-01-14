/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017-2020 JaamSim Software Inc.
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
import java.util.Arrays;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleListInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.input.Input;
import com.jaamsim.input.InterfaceEntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.resourceObjects.AbstractResourceProvider;
import com.jaamsim.resourceObjects.ResourceProvider;
import com.jaamsim.resourceObjects.ResourceUser;
import com.jaamsim.units.DimensionlessUnit;

public class Seize extends LinkedService implements ResourceUser {

	@Keyword(description = "The Resources from which units are to be seized. "
	                     + "All the resource units must be available to be seized before any one "
	                     + "unit is seized.\n\n"
	                     + "When more than one object attempts to seize the same resource, the "
	                     + "resource is assigned based on the priorities and arrival times of "
	                     + "entities waiting in the objects' Queues. "
	                     + "An entity's priority is determined by the 'Priority' input for its "
	                     + "Queue, and is assigned to the entity when it first arrives to the "
	                     + "Queue. "
	                     + "This priority determines both the position of the entity in the queue "
	                     + "and its priority for seizing a resource. "
	                     + "If several entities have the same priority, the resource is assigned "
	                     + "to entity that arrived first to its Queue.",
	         exampleList = {"Resource1 Resource2"})
	protected final InterfaceEntityListInput<ResourceProvider> resourceList;

	@Keyword(description = "The number of units to seize from the Resources specified by the "
	                     + "'ResourceList' keyword. "
	                     + "Only an integer number of resource units can be seized. "
	                     + "A decimal value will be truncated to an integer.",
	         exampleList = {"2 1", "{ 2 } { 1 }", "{ DiscreteDistribution1 } { 'this.obj.attrib1 + 1' }"})
	private final SampleListInput numberOfUnitsList;

	private int[] seizedUnits = new int[1];  // resource units seized by the last entity

	{
		processPosition.setHidden(true);
		workingStateListInput.setHidden(true);
		immediateMaintenanceList.setHidden(true);
		forcedMaintenanceList.setHidden(true);
		opportunisticMaintenanceList.setHidden(true);
		immediateBreakdownList.setHidden(true);
		forcedBreakdownList.setHidden(true);
		opportunisticBreakdownList.setHidden(true);

		immediateThresholdList.setHidden(true);
		immediateReleaseThresholdList.setHidden(true);

		ArrayList<ResourceProvider> resDef = new ArrayList<>();
		resourceList = new InterfaceEntityListInput<>(ResourceProvider.class, "ResourceList", KEY_INPUTS, resDef);
		resourceList.setRequired(true);
		this.addInput(resourceList);
		this.addSynonym(resourceList, "Resource");

		ArrayList<SampleProvider> def = new ArrayList<>();
		def.add(new SampleConstant(1));
		numberOfUnitsList = new SampleListInput("NumberOfUnits", KEY_INPUTS, def);
		numberOfUnitsList.setValidRange(0, Double.POSITIVE_INFINITY);
		numberOfUnitsList.setDimensionless(true);
		numberOfUnitsList.setUnitType(DimensionlessUnit.class);
		this.addInput(numberOfUnitsList);
	}

	public Seize() {}

	@Override
	public void validate() {
		super.validate();
		if (!resourceList.getValue().isEmpty()) {
			Input.validateInputSize(resourceList, numberOfUnitsList);
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		seizedUnits = new int[resourceList.getListSize()];
	}

	@Override
	public void queueChanged() {
		if (isReadyToStart()) {
			AbstractResourceProvider.notifyResourceUsers(getResourceList());
		}
	}

	@Override
	public void thresholdChanged() {
		if (isReadyToStart()) {
			AbstractResourceProvider.notifyResourceUsers(getResourceList());
		}
		super.thresholdChanged();
	}

	@Override
	protected boolean startProcessing(double simTime) {
		return false;
	}

	@Override
	protected double getStepDuration(double simTime) {
		return 0.0d;
	}

	@Override
	protected void processStep(double simTime) {}

	@Override
	public boolean isFinished() {
		return true;  // can always stop when isFinished is called in startStep
	}

	@Override
	public boolean hasWaitingEntity() {
		return !getQueue().isEmpty();
	}

	@Override
	public int getPriority() {
		return getQueue().getFirstPriority();
	}

	@Override
	public double getWaitTime() {
		return getQueue().getQueueTime();
	}

	@Override
	public void startNextEntity() {
		if (isTraceFlag()) trace(2, "startNextEntity");

		// Remove the first entity from the queue
		String m = this.getNextMatchValue(getSimTime());
		DisplayEntity ent = waitQueue.getValue().removeFirstForMatch(m);
		if (ent == null)
			error("Entity not found for specified Match value: %s", m);
		this.registerEntity(ent);

		// Seize the resources and pass the entity to the next component
		this.seizeResources();
		this.sendToNextComponent(ent);
	}

	@Override
	public boolean hasStrictResource() {
		for (ResourceProvider res : getResourceList()) {
			if (res.isStrictOrder()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isReadyToStart() {
		if (!isAvailable() || isForcedDowntimePending() || isImmediateDowntimePending()) {
			return false;
		}
		String m = this.getNextMatchValue(getSimTime());
		DisplayEntity ent = waitQueue.getValue().getFirstForMatch(m);
		return ent != null && checkResources(ent);
	}

	/**
	 * Determine whether the required Resources are available.
	 * @return = TRUE if all the resources are available
	 */
	public boolean checkResources(DisplayEntity ent) {
		double simTime = this.getSimTime();

		// Temporarily set the obj entity to the first one in the queue
		DisplayEntity oldEnt = this.getReceivedEntity(simTime);
		this.setReceivedEntity(ent);

		ArrayList<ResourceProvider> resList = getResourceList();
		ArrayList<SampleProvider> numberList = numberOfUnitsList.getValue();
		for (int i=0; i<resList.size(); i++) {
			int n = (int) numberList.get(i).getNextSample(simTime);
			if (!resList.get(i).canSeize(n, ent)) {
				this.setReceivedEntity(oldEnt);
				return false;
			}
		}
		this.setReceivedEntity(oldEnt);
		return true;
	}

	/**
	 * Seize the required Resources.
	 */
	public void seizeResources() {
		double simTime = this.getSimTime();
		if (getResourceList().isEmpty())
			return;

		// Set the number of resources to seize
		ArrayList<SampleProvider> numberList = numberOfUnitsList.getValue();
		for (int i=0; i<numberList.size(); i++) {
			seizedUnits[i] = (int)numberList.get(i).getNextSample(simTime);
		}

		// Seize the resources
		DisplayEntity ent = getReceivedEntity(simTime);
		ArrayList<ResourceProvider> resList = getResourceList();
		for (int i=0; i<resList.size(); i++) {
			resList.get(i).seize(seizedUnits[i], ent);
		}
	}

	public Queue getQueue() {
		return waitQueue.getValue();
	}

	public ArrayList<ResourceProvider> getResourceList() {
		return resourceList.getValue();
	}

	@Override
	public boolean requiresResource(ResourceProvider res) {
		if (getResourceList() == null)
			return false;
		return getResourceList().contains(res);
	}

	@Output(name = "SeizedUnits",
	 description = "The number of resource units seized by the last entity.",
	    unitType = DimensionlessUnit.class,
	    sequence = 1)
	public int[] getSeizedUnits(double simTime) {
		return Arrays.copyOf(seizedUnits, seizedUnits.length);
	}

}
