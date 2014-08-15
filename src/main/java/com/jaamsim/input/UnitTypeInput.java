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
package com.jaamsim.input;

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.ObjectType;

public class UnitTypeInput extends Input<ObjectType> {
	private Class<? extends Unit> unitType;

	public UnitTypeInput(String key, String cat, Class<? extends Unit> ut) {
		super(key, cat, null);
		unitType = ut;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);
		if (value != null)
			throw new InputErrorException("Value has already been set to %s", value.getInputName());

		ObjectType t = Input.parseEntity(kw.getArg(0), ObjectType.class);
		Class<? extends Unit> type = Input.checkCast(t.getJavaClass(), Unit.class);

		value = t;
		unitType = type;
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<String>();
		for (ObjectType each: Entity.getClonesOfIterator(ObjectType.class)) {
			Class<? extends Entity> klass = each.getJavaClass();
			if (klass == null)
				continue;

			if (Unit.class.isAssignableFrom(klass))
				list.add(each.getInputName());
		}
		Collections.sort(list);
		return list;
	}

	public Class<? extends Unit> getUnitType() {
		return unitType;
	}
}
