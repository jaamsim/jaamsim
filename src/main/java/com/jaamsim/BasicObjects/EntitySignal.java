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

import com.jaamsim.Thresholds.SignalThreshold;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class EntitySignal extends LinkedComponent {

	@Keyword(description = "The Threshold controlled by this Signal.",
	         example = "EntitySignal1 TargetSignalThreshold { SignalThreshold1 }")
	private final EntityInput<SignalThreshold> targetSignalThreshold;

	@Keyword(description = "The new state for the target SignalThreshold: TRUE = Open, FALSE = Closed.",
	         example = "EntitySignal1 NewState { FALSE }")
	private final BooleanInput newState;

	{
		targetSignalThreshold = new EntityInput<SignalThreshold>( SignalThreshold.class, "TargetSignalThreshold", "Key Inputs", null);
		this.addInput( targetSignalThreshold);

		newState = new BooleanInput( "NewState", "Key Inputs", true);
		this.addInput( newState);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the target threshold has been specified
		if( targetSignalThreshold.getValue() == null ) {
			throw new InputErrorException( "The keyword TargetThreshold must be set." );
		}
	}

	@Override
	public void addDisplayEntity( DisplayEntity ent ) {
		super.addDisplayEntity(ent);

		// Signal the target threshold
		targetSignalThreshold.getValue().setOpen(newState.getValue());

		// Send the entity to the next component
		this.sendToNextComponent( ent );
	}

}
