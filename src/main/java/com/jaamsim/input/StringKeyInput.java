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
import java.util.HashMap;

import com.jaamsim.basicsim.Entity;

public class StringKeyInput<T extends Entity> extends Input<HashMap<String,T>> {

	private Class<T> entClass;

	public StringKeyInput(Class<T> klass, String keyword, String cat) {
		super(keyword, cat, null);
		entClass = klass;
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {
		HashMap<String,T> hashMap = new HashMap<>();
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			Input.assertCount(subArg, 2);
			T ent = Input.tryParseEntity(subArg.getArg(1), entClass );
			hashMap.put(subArg.getArg(0), ent);
		}
		value = hashMap;
	}

	public T getValueFor(String str) {
		if (value == null)
			return null;
		return value.get(str);
	}

}
