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

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class EntitySignal extends LinkedComponent {

	@Keyword(description = "The EntityGate controlled by this Signal.",
	         example = "EntitySignal1 TargetGate { Gate1 }")
	private final EntityInput<EntityGate> targetGate;

	@Keyword(description = "The new state for the target EntityGate: TRUE = Open, FALSE = Closed.",
	         example = "EntitySignal1 NewState { FALSE }")
	private final BooleanInput newState;

	{
		targetGate = new EntityInput<EntityGate>( EntityGate.class, "TargetGate", "Key Inputs", null);
		this.addInput( targetGate);

		newState = new BooleanInput( "NewState", "Key Inputs", true);
		this.addInput( newState);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the target gate has been specified
		if( targetGate.getValue() == null ) {
			throw new InputErrorException( "The keyword TargetGate must be set." );
		}
	}

	/**
	 * Add a DisplayEntity from upstream
	 * @param ent = entity to be added
	 */
	@Override
	public void addDisplayEntity( DisplayEntity ent ) {
		super.addDisplayEntity(ent);

		// Signal the target gate
		targetGate.getValue().setState(newState.getValue());

		// Send the entity to the next component
		this.sendToNextComponent( ent );
	}

}
