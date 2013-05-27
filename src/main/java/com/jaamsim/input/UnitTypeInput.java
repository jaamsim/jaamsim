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

import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation.StringVector;

public class UnitTypeInput extends Input<Class<? extends Unit>>{
	public UnitTypeInput(String key, String cat) {
		super(key, cat, Unit.class);
	}

	@Override
	public void parse(StringVector input)
	throws InputErrorException {
		Input.assertCount(input, 1);
		if (value != Unit.class)
			throw new InputErrorException("Value has already been set to %s", value.getName());

		ObjectType t = Input.parseEntity(input.get(0), ObjectType.class);
		value = Input.checkCast(t.getJavaClass(), Unit.class);
		this.updateEditingFlags();
	}
}
