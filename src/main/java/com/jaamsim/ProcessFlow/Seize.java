/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017 JaamSim Software Inc.
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
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;

public class Seize extends LinkedService implements ResourceUser {

	@Keyword(description = "The Resources from which units are to be seized.",
	         exampleList = {"Resource1 Resource2"})
	private final EntityListInput<Resource> resourceList;

	@Keyword(description = "The number of units to seize from the Resources specified by the "
	                     + "'ResourceList' keyword.",
	         exampleList = {"{ 2 } { 1 }", "{ DiscreteDistribution1 } { 'this.obj.attrib1 + 1' }"})
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

		resourceList = new EntityListInput<>(Resource.class, "ResourceList", "Key Inputs", null);
		resourceList.setRequired(true);
		this.addInput(resourceList);
		this.addSynonym(resourceList, "Resource");

		ArrayList<SampleProvider> def = new ArrayList<>();
		def.add(new SampleConstant(1));
		numberOfUnitsList = new SampleListInput("NumberOfUnits", "Key Inputs", def);
		numberOfUnitsList.setEntity(this);
		numberOfUnitsList.setValidRange(0, Double.POSITIVE_INFINITY);
		numberOfUnitsList.setDimensionless(true);
		numberOfUnitsList.setUnitType(DimensionlessUnit.class);
		this.addInput(numberOfUnitsList);
	}

	@Override
	public void validate() {
		super.validate();
		Input.validateInputSize(resourceList, numberOfUnitsList);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		seizedUnits = new int[resourceList.getListSize()];
	}

	@Override
	public void queueChanged() {
		if (this.isReadyToStart()) {
			startNextEntity();
		}
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
	protected boolean processStep(double simTime) {
		return true;
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
		for (Resource res : getResourceList()) {
			if (res.isStrictOrder()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isReadyToStart() {
		String m = this.getNextMatchValue(getSimTime());
		DisplayEntity ent = waitQueue.getValue().getFirstForMatch(m);
		if (ent == null)
			return false;
		return checkResources(ent) && isOpen();
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

		ArrayList<Resource> resList = getResourceList();
		ArrayList<SampleProvider> numberList = numberOfUnitsList.getValue();
		for (int i=0; i<resList.size(); i++) {
			if (resList.get(i).getAvailableUnits(simTime) < (int) numberList.get(i).getNextSample(simTime)) {
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

		// Set the number of resources to seize
		ArrayList<SampleProvider> numberList = numberOfUnitsList.getValue();
		for (int i=0; i<numberList.size(); i++) {
			seizedUnits[i] = (int)numberList.get(i).getNextSample(simTime);
		}

		// Seize the resources
		ArrayList<Resource> resList = getResourceList();
		for (int i=0; i<resList.size(); i++) {
			resList.get(i).seize(seizedUnits[i]);
		}
	}

	public Queue getQueue() {
		return waitQueue.getValue();
	}

	public ArrayList<Resource> getResourceList() {
		return resourceList.getValue();
	}

	@Override
	public boolean requiresResource(Resource res) {
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
