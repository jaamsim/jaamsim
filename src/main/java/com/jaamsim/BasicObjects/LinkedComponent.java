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

import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation3D.DisplayEntity;

/**
 * LinkedComponents are used to form a chain of components that process DisplayEntities that pass through the system.
 * Sub-classes for EntityGenerator, Server, and EntitySink.
 */
public class LinkedComponent extends DisplayEntity {

	@Keyword(description = "The next object to which the processed DisplayEntity is passed.",
	         example = "EntityGenerator1 NextComponent { Server1 }")
	private final EntityInput<LinkedComponent> nextComponentInput;

	int numberProcessed; // Number of entities processed by this component

	{
		nextComponentInput = new EntityInput<LinkedComponent>( LinkedComponent.class, "NextComponent", "Key Inputs", null);
		this.addInput( nextComponentInput, true);
	}

	public LinkedComponent() {
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		numberProcessed = 0;
	}


	/**
	 * MUST BE OVERWRITTEN BY EACH SUB-CLASS
	 * Receive a DisplayEntity from the upstream LinkedEntity.
	 * @param ent = the DisplayEntity that is received.
	 */
	public void addDisplayEntity( DisplayEntity ent ) {
	}

	public LinkedComponent getNextComponent(){
		return nextComponentInput.getValue();
	}

}
