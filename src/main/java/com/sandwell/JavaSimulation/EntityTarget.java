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
package com.sandwell.JavaSimulation;

import com.jaamsim.events.ProcessTarget;

public abstract class EntityTarget<T extends Entity> extends ProcessTarget {
	protected final T ent;
	private final String desc;

	public EntityTarget(T ent, String method) {
		this.ent = ent;
		this.desc = method;
	}

	@Override
	public String getDescription() {
		return ent.getInputName() + "." + desc;
	}
}
