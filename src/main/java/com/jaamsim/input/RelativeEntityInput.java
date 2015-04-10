/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.EntityInput;

public class RelativeEntityInput extends EntityInput<DisplayEntity> {

	private DisplayEntity thisEnt;  // entity that owns this input

	public RelativeEntityInput(String key, String cat, DisplayEntity def) {
		super(DisplayEntity.class, key, cat, def);
	}

	public void setEntity(DisplayEntity ent) {
		thisEnt = ent;
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {
		Input.assertCount(kw, 1);
		DisplayEntity ent = Input.parseEntity(kw.getArg(0), DisplayEntity.class);
		if (isCircular(ent))
			throw new InputErrorException("The assignment of %s to RelativeEntity would create a circular loop.", ent);
		value = ent;
	}

	private boolean isCircular(DisplayEntity ent) {
		while (ent != null) {
			if (ent == thisEnt)
				return true;
			ent = ent.getRelativeEntity();
		}
		return false;
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<>();
		for (DisplayEntity each: Entity.getClonesOfIterator(DisplayEntity.class)) {
			if (each.testFlag(Entity.FLAG_GENERATED))
				continue;

			if (isCircular(each))
				continue;

			list.add(each.getName());
		}
		Collections.sort(list);
		return list;
	}

}
