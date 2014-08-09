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

import com.sandwell.JavaSimulation3D.DisplayEntity;

/**
 * EntitySink kills the DisplayEntities sent to it.
 */
public class EntitySink extends LinkedComponent {

	{
		nextComponentInput.setHidden(true);
		testEntity.setHidden(true);
		stateAssignment.setHidden(true);
	}

	@Override
	public void addDisplayEntity( DisplayEntity ent ) {
		super.addDisplayEntity(ent);

		// Only increments the number process when there is no next entity
		this.sendToNextComponent(ent);

		// Kill the added entity
		ent.kill();
	}

}
