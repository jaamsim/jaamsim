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
package com.jaamsim.basicsim;

import com.sandwell.JavaSimulation.Entity;

public class InstanceIterable<T extends Entity> extends EntityIterator<T> {
	public InstanceIterable(Class<T> aClass) {
		super(aClass);
	}

	@Override
	public boolean matches(Class<?> entklass) {
		return entClass == entklass;
	}
}
