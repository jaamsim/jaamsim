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

public class EntityInput<T extends Entity> extends Input<T> {

	private Class<T> entClass;

	public EntityInput(Class<T> aClass, String key, String cat, T def) {
		super(key, cat, def);
		entClass = aClass;
	}

	public void parse(StringVector input)
	throws InputErrorException {
		Input.assertCount(input, 0, 1);
		if(input.size() == 0)
			value = null;
		else
			value = Input.parseEntity(input.get(0), entClass);

		this.updateEditingFlags();
	}

	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<String>();
		for(T each: Simulation.getClonesOf(entClass) ) {
			if(each.testFlag(Entity.FLAG_GENERATED))
				continue;

			list.add(each.getInputName());
		}
		return list;
	}
}
