/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpListInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Keyword;

public class Release extends LinkedComponent {

	@Keyword(description = "The Resource(s) to be released.",
	         exampleList = {"Resource1 Resource2"})
	private final EntityListInput<Resource> resourceList;

	@Keyword(description = "The number of units to release from the Resource(s).",
	         exampleList = {"{ 2 } { 1 }", "{ DiscreteDistribution1 } { 'this.obj.attrib1 + 1' }"})
	private final SampleExpListInput numberOfUnitsList;

	{
		resourceList = new EntityListInput<>(Resource.class, "Resource", "Key Inputs", null);
		resourceList.setRequired(true);
		this.addInput( resourceList);

		ArrayList<SampleProvider> def = new ArrayList<>();
		def.add(new SampleConstant(1));
		numberOfUnitsList = new SampleExpListInput("NumberOfUnits", "Key Inputs", def);
		numberOfUnitsList.setEntity(this);
		numberOfUnitsList.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput( numberOfUnitsList);
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
