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

import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.DimensionlessUnit;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class Branch extends LinkedComponent {

	@Keyword(description = "The list of possible next objects to which the processed DisplayEntity can be passed.",
	         example = "Branch1 NextComponentList { Server1 Server2 }")
	protected final EntityListInput<LinkedComponent> nextComponentList;

	@Keyword(description = "A number that determines the choice of next component:\n" +
			"     1 = first branch, 2 = second branch, etc.\n" +
			"A constant value, a distribution to be sampled, or a time series can be entered.",
	         example = "Branch1 Choice { 2 }")
	private final SampleExpInput choice;

	{
		nextComponentInput.setHidden(true);
		operatingThresholdList.setHidden(true);

		nextComponentList = new EntityListInput<LinkedComponent>( LinkedComponent.class, "NextComponentList", "Key Inputs", null);
		this.addInput( nextComponentList);

		choice = new SampleExpInput( "Choice", "Key Inputs", null);
		choice.setUnitType( DimensionlessUnit.class );
		choice.setEntity(this);
		this.addInput( choice);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that at least one next component has been specified
		if( nextComponentList.getValue() == null ) {
			throw new InputErrorException( "The keyword NextComponentList must be set." );
		}

		// Confirm that a choice input has been specified
		if( choice.getValue() == null ) {
			throw new InputErrorException( "The keyword Choice must be set." );
		}
	}

	@Override
	public void addDisplayEntity( DisplayEntity ent ) {
		super.addDisplayEntity(ent);

		// Choose the next component for this entity
		int i = (int) choice.getValue().getNextSample(this.getSimTime());

		// Pass the entity to the next component
		nextComponentList.getValue().get(i-1).addDisplayEntity(ent);
	}

}
