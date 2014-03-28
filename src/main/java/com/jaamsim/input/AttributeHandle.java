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
package com.jaamsim.input;

import com.sandwell.JavaSimulation.Entity;

public class AttributeHandle extends OutputHandle {
	private String attributeName;

	public AttributeHandle(Entity e, String outputName) {
		super(e);
		this.attributeName = outputName;
	}

	@Override
	public <T> T getValue(double simTime, Class<T> klass) {
		if (!ent.hasAttribute(attributeName)) {
			return null;
		}
		if (!double.class.equals(klass)) {
			return null;
		}
		// This is kind of messy
		return klass.cast(ent.getAttribute(attributeName));
	}
	@Override
	public double getValueAsDouble(double simTime, double def) {
		if (!ent.hasAttribute(attributeName)) {
			return def;
		}

		return ent.getAttribute(attributeName);
	}

	@Override
	public Class<?> getReturnType() {
		return double.class;
	}
	@Override
	public Class<?> getDeclaringClass() {
		return Entity.class;
	}
	@Override
	public String getDescription() {
		return "User defined attribute";
	}
	@Override
	public String getName() {
		return attributeName;
	}
}
