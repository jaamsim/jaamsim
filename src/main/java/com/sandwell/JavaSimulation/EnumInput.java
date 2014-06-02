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
package com.sandwell.JavaSimulation;

import java.util.ArrayList;

import com.jaamsim.input.Input;
import com.jaamsim.input.KeywordIndex;

public class EnumInput<T extends Enum<T>> extends Input<T> {
	private final Class<T> type;

	public EnumInput(Class<T> atype, String key, String cat, T def) {
		super(key, cat, def);
		type = atype;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);
		value = Input.parseEnum(type, kw.getArg(0));
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> tmp = new ArrayList<String>();
		for (T each : type.getEnumConstants())
			tmp.add(each.name());
		return tmp;
	}
}
