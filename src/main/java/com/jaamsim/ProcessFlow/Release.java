/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2020 JaamSim Software Inc.
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
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleListInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.input.InterfaceEntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.resourceObjects.AbstractResourceProvider;
import com.jaamsim.resourceObjects.ResourceProvider;
import com.jaamsim.units.DimensionlessUnit;

public class Release extends LinkedComponent {

	@Keyword(description = "The Resources from which units are to be released.",
	         exampleList = {"Resource1 Resource2"})
	private final InterfaceEntityListInput<ResourceProvider> resourceList;

	@Keyword(description = "The number of units to release from the Resources specified by the "
	                     + "'ResourceList' keyword. "
	                     + "The last value in the list is used if the number of resources is "
	                     + "greater than the number of values. "
	                     + "Only an integer number of resource units can be released. "
	                     + "A decimal value will be truncated to an integer.",
	         exampleList = {"2 1", "{ 2 } { 1 }", "{ DiscreteDistribution1 } { 'this.obj.attrib1 + 1' }"})
	private final SampleListInput numberOfUnitsList;

	{
		resourceList = new InterfaceEntityListInput<>(ResourceProvider.class, "ResourceList", KEY_INPUTS, null);
		resourceList.setRequired(true);
		this.addInput( resourceList);
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

	public Release() {}

	@Override
	public void addEntity( DisplayEntity ent ) {
		super.addEntity(ent);
		this.releaseResources(ent);
		this.sendToNextComponent( ent );
	}

	/**
	 * Release the specified Resources.
	 */
	public void releaseResources(DisplayEntity ent) {
		double simTime = this.getSimTime();
		ArrayList<ResourceProvider> resList = resourceList.getValue();
		ArrayList<SampleProvider> numberList = numberOfUnitsList.getValue();

		// Release the Resources
		for(int i=0; i<resList.size(); i++) {
			int ind = Math.min(i, numberList.size() - 1);
			int n = (int) numberList.get(ind).getNextSample(simTime);
			if (n == 0)
				continue;
			resList.get(i).release(n, ent);
		}

		// Notify any resource users that are waiting for these Resources
		AbstractResourceProvider.notifyResourceUsers(resList);
	}

}
