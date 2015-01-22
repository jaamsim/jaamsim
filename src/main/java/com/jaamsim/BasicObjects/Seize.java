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
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.IntegerListInput;
import com.jaamsim.input.Keyword;

public class Seize extends LinkedService {

	@Keyword(description = "The Resource(s) to be seized.",
	         example = "Seize1 Resource { Resource1 Resource2 }")
	private final EntityListInput<Resource> resourceList;

	@Keyword(description = "The number of units to seize from the Resource(s).",
	         example = "Seize1 NumberOfUnits { 2 1 }")
	private final IntegerListInput numberOfUnitsList;

	{
		resourceList = new EntityListInput<>(Resource.class, "Resource", "Key Inputs", null);
		this.addInput(resourceList);

		IntegerVector defNum = new IntegerVector();
		defNum.add(1);
		numberOfUnitsList = new IntegerListInput("NumberOfUnits", "Key Inputs", defNum);
		this.addInput(numberOfUnitsList);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the resource has been specified
		if (resourceList.getValue() == null) {
			throw new InputErrorException("The keyword Resource must be set.");
		}
	}

	@Override
	public void queueChanged() {
		this.startAction();
	}

	@Override
	public void startAction() {

		// Stop if the queue is empty, there are insufficient resources, or a threshold is closed
		while (waitQueue.getValue().getCount() != 0 && this.checkResources() && this.isOpen()) {

			// If sufficient units are available, then seize them and pass the entity to the next component
			this.seizeResources();
			DisplayEntity ent = this.getNextEntity();
			this.sendToNextComponent(ent);
		}
		this.setBusy(false);
	}

	@Override
	public void endAction() {
		// not required
	}

	/**
	 * Determine whether the required Resources are available.
	 * @return = TRUE if all the resources are available
	 */
	public boolean checkResources() {
		ArrayList<Resource> resList = resourceList.getValue();
		IntegerVector numberList = numberOfUnitsList.getValue();
		for (int i=0; i<resList.size(); i++) {
			if (resList.get(i).getAvailableUnits() < numberList.get(i))
				return false;
		}
		return true;
	}

	/**
	 * Seize the required Resources.
	 * @return
	 */
	public void seizeResources() {
		ArrayList<Resource> resList = resourceList.getValue();
		IntegerVector numberList = numberOfUnitsList.getValue();
		for (int i=0; i<resList.size(); i++) {
			resList.get(i).seize(numberList.get(i));
		}
	}

	public Queue getQueue() {
		return waitQueue.getValue();
	}

	/**
	 * Is the specified Resource required by this Seize object?
	 * @param res = the specified Resource.
	 * @return = TRUE if the Resource is required.
	 */
	public boolean requiresResource(Resource res) {
		return resourceList.getValue().contains(res);
	}

}
