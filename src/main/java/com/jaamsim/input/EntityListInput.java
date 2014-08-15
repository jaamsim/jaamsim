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
package com.jaamsim.input;

import java.util.ArrayList;
import java.util.Collections;

import com.sandwell.JavaSimulation.Entity;

public class EntityListInput<T extends Entity> extends ListInput<ArrayList<T>> {
	private Class<T> entClass;
	private boolean unique; // flag to determine if list must be unique or not
	private boolean even;  // flag to determine if there must be an even number of entries

	public EntityListInput(Class<T> aClass, String key, String cat, ArrayList<T> def) {
		super(key, cat, def);
		entClass = aClass;
		unique = true;
		even = false;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		Input.assertCountRange(kw, minCount, maxCount);
		if( even )
			Input.assertCountEven(kw);

		value = Input.parseEntityList(kw, entClass, unique);
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
	public void setEven(boolean bool) {
		this.even = bool;
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<String>();
		for(T each: Entity.getClonesOfIterator(entClass) ) {
			if(each.testFlag(Entity.FLAG_GENERATED))
				continue;

			list.add(each.getInputName());
		}
		Collections.sort(list);
		return list;
	}

	private String getInputString(ArrayList<T> val) {

		if (val.size() == 0)
			return NO_VALUE;

		StringBuilder tmp = new StringBuilder();
		tmp.append(val.get(0).getInputName());
		for (int i = 1; i < val.size(); i++) {
			tmp.append(SEPARATOR);
			tmp.append(val.get(i).getInputName());
		}
		return tmp.toString();
	}


	@Override
	public String getDefaultString() {
		if (defValue == null || defValue.size() == 0)
			return NO_VALUE;
		return this.getInputString(defValue);
	}

	@Override
	public String getValueString() {
		if (value == null || value.size() == 0)
			return "";
		return this.getInputString(value);
	}
}
