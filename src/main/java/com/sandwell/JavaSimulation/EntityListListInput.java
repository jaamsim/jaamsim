/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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

public class EntityListListInput<T extends Entity> extends ListInput<ArrayList<ArrayList<T>>> {
	private Class<T> entClass;
	private boolean unique; // flag to determine if inner lists must be unique or not

	public EntityListListInput(Class<T> aClass, String key, String cat, ArrayList<ArrayList<T>> def) {
		super(key, cat, def);
		entClass = aClass;
		unique = true;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		// Check if number of outer lists violate minCount or maxCount
		if (subArgs.size() < minCount || subArgs.size() > maxCount)
			throw new InputErrorException(INP_ERR_RANGECOUNT, minCount, maxCount, kw.argString());

		value = Input.parseListOfEntityLists(kw, entClass, unique);
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return NO_VALUE;

		if (defValue.size() ==0)
			return NO_VALUE;

		StringBuilder tmp = new StringBuilder();
		for (ArrayList<T> each: defValue) {

			// blank space between elements
			if (tmp.length() > 0)
				tmp.append(SEPARATOR);

			if (each == null) {
				tmp.append(NO_VALUE);
				continue;
			}
			if (each.isEmpty()) {
				tmp.append(NO_VALUE);
				continue;
			}

			tmp.append("{");
			tmp.append(SEPARATOR);
			for (T ent:each) {
				tmp.append(ent.getInputName());
				tmp.append(SEPARATOR);
			}
			tmp.append("}");
		}
		return tmp.toString();
	}
}
