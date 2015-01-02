/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.DimensionlessUnit;

public class RemoveFrom extends Unpack {

	@Keyword(description = "The maximum number of entities to remove from the container.",
	         example = "RemoveFrom1 NumberOfEntities { 2 }")
	private final SampleExpInput numberOfEntities;

	@Keyword(description = "The next object to which the processed EntityContainer is passed.",
			example = "RemoveFrom1 NextForContainers { Server1 }")
	protected final EntityInput<LinkedComponent> nextForContainers;

	{
		numberOfEntities = new SampleExpInput("NumberOfEntities", "Key Inputs", new SampleConstant(1.0));
		numberOfEntities.setUnitType(DimensionlessUnit.class);
		numberOfEntities.setEntity(this);
		numberOfEntities.setValidRange(1, Double.POSITIVE_INFINITY);
		this.addInput(numberOfEntities);

		nextForContainers = new EntityInput<>(LinkedComponent.class, "NextForContainers", "Key Inputs", null);
		this.addInput(nextForContainers);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the waiting queue has been specified
		if (nextForContainers.getValue() == null) {
			throw new InputErrorException("The keyword NextForContainers must be set.");
		}
	}

	@Override
	protected void disposeContainer(EntityContainer c) {
		if( nextForContainers.getValue() != null )
			nextForContainers.getValue().addDisplayEntity(c);
	}

	@Override
	protected int getNumberToRemove() {
		return (int) numberOfEntities.getValue().getNextSample(this.getSimTime());
	}

}
