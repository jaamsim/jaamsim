/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.DimensionlessUnit;

public class Release extends LinkedComponent {

	@Keyword(description = "The Resource(s) to be released.",
	         exampleList = {"Resource1 Resource2"})
	private final EntityListInput<Resource> resourceList;

	@Keyword(description = "The number of units to release from the Resource(s).",
	         exampleList = {"{ 2 } { 1 }", "{ DiscreteDistribution1 } { 'this.obj.attrib1 + 1' }"})
	private final SampleListInput numberOfUnitsList;

	{
		resourceList = new EntityListInput<>(Resource.class, "ResourceList", "Key Inputs", null);
		resourceList.setRequired(true);
		this.addInput( resourceList);
		this.addSynonym(resourceList, "Resource");

		ArrayList<SampleProvider> def = new ArrayList<>();
		def.add(new SampleConstant(1));
		numberOfUnitsList = new SampleListInput("NumberOfUnits", "Key Inputs", def);
		numberOfUnitsList.setEntity(this);
		numberOfUnitsList.setValidRange(1, Double.POSITIVE_INFINITY);
		numberOfUnitsList.setUnitType(DimensionlessUnit.class);
		this.addInput( numberOfUnitsList);
	}

	@Override
	public void validate() {
		super.validate();
		Input.validateInputSize(resourceList, numberOfUnitsList);
	}

	@Override
	public void addEntity( DisplayEntity ent ) {
		super.addEntity(ent);
		this.releaseResources();
		this.sendToNextComponent( ent );
	}

	/**
	 * Release the specified Resources.
	 * @return
	 */
	public void releaseResources() {
		double simTime = this.getSimTime();
		ArrayList<Resource> resList = resourceList.getValue();
		ArrayList<SampleProvider> numberList = numberOfUnitsList.getValue();

		// Release the Resources
		for(int i=0; i<resList.size(); i++) {
			int n = (int) numberList.get(i).getNextSample(simTime);
			resList.get(i).release(n);
		}

		// Notify any Seize objects that are waiting for these Resources
		for(int i=0; i<resList.size(); i++) {
			resList.get(i).notifySeizeObjects();
		}
	}

}
