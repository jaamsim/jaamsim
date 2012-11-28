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

/**
 * Base class for a JaamSim observer.
 * @author Matt.Chudleigh
 *
 */
public class Observer {
	protected Entity _observee;

	public Observer(Entity observee) {
		_observee = observee;
	}

	/**
	 * Is true if this observer has an entity and that entity is not dead
	 * @return
	 */
	public boolean hasObservee() {
		return (_observee != null && !_observee.testFlag(Entity.FLAG_DEAD));
	}

	/**
	 * Is this observer currently observing this entity (mostly a convenience function
	 * @param ent
	 * @return
	 */
	public boolean isObserving(Entity ent) {
		if (_observee == null) {
			return false;
		}
		return (_observee == ent);
	}
}
