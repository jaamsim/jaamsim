/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2011 Ausenco Engineering Canada Inc.
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

public class BooleanInput extends Input<Boolean> {

	/**
	 * Creates a new Boolean Input with the given keyword, category, units, and
	 * default value.
	 */
	public BooleanInput(String key, String cat, boolean def) {
		super(key, cat, Boolean.valueOf(def));
	}

	public void parse(StringVector input)
	throws InputErrorException {
		Input.assertCount(input, 1);
		value = Boolean.valueOf(Input.parseBoolean(input.get(0)));
		this.updateEditingFlags();
	}
}
