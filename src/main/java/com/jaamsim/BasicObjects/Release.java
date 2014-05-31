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

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.IntegerListInput;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class Release extends LinkedComponent {

	@Keyword(description = "The Resource(s) to be released.",
	         example = "Release1 Resource { Resource1 }")
	private final EntityListInput<Resource> resourceList;

	@Keyword(description = "The number of units to release from the Resource(s).",
	         example = "Release1 NumberOfUnits { 2 }")
	private final IntegerListInput numberOfUnitsList;

	{
		operatingThresholdList.setHidden(true);

		resourceList = new EntityListInput<Resource>(Resource.class, "Resource", "Key Inputs", null);
		this.addInput( resourceList);

		IntegerVector defNum = new IntegerVector();
		defNum.add(1);
		numberOfUnitsList = new IntegerListInput("NumberOfUnits", "Key Inputs", defNum);
		this.addInput( numberOfUnitsList);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the resource has been specified
		if( resourceList.getValue() == null ) {
			throw new InputErrorException( "The keyword Resource must be set." );
		}
	}

	/**
	 * Add a DisplayEntity from upstream
	 * @param ent = entity to be added
	 */
	@Override
	public void addDisplayEntity( DisplayEntity ent ) {
		super.addDisplayEntity(ent);
		this.releaseResources();
		this.sendToNextComponent( ent );
	}

	/**
	 * Release the specified Resources.
	 * @return
	 */
	public void releaseResources() {
		ArrayList<Resource> resList = resourceList.getValue();
		IntegerVector numberList = numberOfUnitsList.getValue();

		// Release the Resources
		for(int i=0; i<resList.size(); i++) {
			resList.get(i).release(numberList.get(i));
		}

		// Notify any Seize objects that are waiting for these Resources
		for(int i=0; i<resList.size(); i++) {
			resList.get(i).notifySeizeObjects();
		}
	}

}
