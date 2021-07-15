/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017-2021 JaamSim Software Inc.
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
import com.jaamsim.input.InterfaceEntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.resourceObjects.ResourceProvider;
import com.jaamsim.resourceObjects.ResourceUser;
import com.jaamsim.units.DimensionlessUnit;

public abstract class AbstractLinkedResourceUser extends LinkedService implements ResourceUser {

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
	                     + "The last value in the list is used if the number of resources is "
	                     + "greater than the number of values. "
	                     + "Only an integer number of resource units can be seized. "
	                     + "A decimal value will be truncated to an integer.",
	         exampleList = {"2 1", "{ 2 } { 1 }", "{ DiscreteDistribution1 } { 'this.obj.attrib1 + 1' }"})
	private final SampleListInput numberOfUnitsList;

	private int[] seizedUnits = new int[1];  // resource units seized by the last entity

	{
		ArrayList<ResourceProvider> resDef = new ArrayList<>();
		resourceList = new InterfaceEntityListInput<>(ResourceProvider.class, "ResourceList", KEY_INPUTS, resDef);
		this.addInput(resourceList);
		this.addSynonym(resourceList, "Resource");

		ArrayList<SampleProvider> def = new ArrayList<>();
		def.add(new SampleConstant(1));
		numberOfUnitsList = new SampleListInput("NumberOfUnits", KEY_INPUTS, def);
		numberOfUnitsList.setValidRange(0, Double.POSITIVE_INFINITY);
		numberOfUnitsList.setDimensionless(true);
		numberOfUnitsList.setUnitType(DimensionlessUnit.class);
		numberOfUnitsList.setIntegerValue(true);
		this.addInput(numberOfUnitsList);
	}

	public AbstractLinkedResourceUser() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		seizedUnits = new int[resourceList.getListSize()];
	}

	@Override
	public boolean hasWaitingEntity() {
		double simTime = getSimTime();
		return !getQueue(simTime).isEmpty();
	}

	@Override
	public int getPriority() {
		double simTime = getSimTime();
		return getQueue(simTime).getFirstPriority();
	}

	@Override
	public double getWaitTime() {
		double simTime = getSimTime();
		return getQueue(simTime).getQueueTime();
	}

	@Override
	public void startNextEntity() {
		if (isTraceFlag()) trace(2, "startNextEntity");

		// Remove the first entity from the queue
		double simTime = getSimTime();
		String m = this.getNextMatchValue(simTime);
		this.setMatchValue(m);
		DisplayEntity ent = removeNextEntity(m);
		if (ent == null)
			error("Entity not found for specified Match value: %s", m);
		receiveEntity(ent);
		setEntityState(ent);

		// Seize the resources
		this.seizeResources();
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
		double simTime = getSimTime();
		String m = this.getNextMatchValue(simTime);
		DisplayEntity ent = getNextEntity(m);
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
			int ind = Math.min(i, numberList.size() - 1);
			int n = (int) numberList.get(ind).getNextSample(simTime);
			if (!resList.get(i).canSeize(simTime, n, ent)) {
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
		ArrayList<ResourceProvider> resList = getResourceList();
		ArrayList<SampleProvider> numberList = numberOfUnitsList.getValue();
		for (int i = 0; i < resList.size(); i++) {
			int ind = Math.min(i, numberList.size() - 1);
			seizedUnits[i] = (int) numberList.get(ind).getNextSample(simTime);
		}

		// Seize the resources
		DisplayEntity ent = getReceivedEntity(simTime);
		for (int i=0; i<resList.size(); i++) {
			resList.get(i).seize(seizedUnits[i], ent);
		}
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
