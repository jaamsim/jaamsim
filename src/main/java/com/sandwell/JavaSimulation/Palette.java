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

import java.util.ArrayList;

public class Palette extends Entity {
	private static final ArrayList<Palette> allInstances;

	static {
		allInstances = new ArrayList<Palette>();
	}

	public Palette() {
		allInstances.add(this);
	}

	public static ArrayList<Palette> getAll() {
		return allInstances;
	}

	@Override
	public void kill() {
		super.kill();
		allInstances.remove(this);
	}
}
