/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.observers;

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityInput;

public abstract class ObserverProto extends Entity {

	protected EntityInput<Entity> observeeInput;

	public ObserverProto() {

		observeeInput = new EntityInput<Entity>(Entity.class, "Observee", "Key Inputs", null);
		this.addInput(observeeInput, true);
	}

	/**
	 * Instantiate this observer using the input entity
	 * @return
	 */
	abstract public Observer instantiate();
	/**
	 * Instantiate an observer using the supplied Entity
	 * @param observee - the entity to observe
	 * @return
	 */
	abstract public Observer instantiate(Entity observee);
}
