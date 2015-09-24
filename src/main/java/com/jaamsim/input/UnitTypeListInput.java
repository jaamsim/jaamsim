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

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.units.Unit;

public class UnitTypeListInput extends ListInput<ArrayList<ObjectType>> {
	private ArrayList<Class<? extends Unit>> unitTypeList;
	private ArrayList<Class<? extends Unit>> defaultUnitTypeList;

	public UnitTypeListInput(String key, String cat, ArrayList<Class<? extends Unit>> utList) {
		super(key, cat, null);
		defaultUnitTypeList = new ArrayList<>(utList);
		if (utList == null)
			return;
		ArrayList<ObjectType> otList = new ArrayList<>(utList.size());
		for (Class<? extends Unit> ut : utList) {
			otList.add(ObjectType.getObjectTypeForClass(ut));
		}
		setDefaultValue(otList);
		setUnitTypeList(value);
	}

	private void setUnitTypeList(ArrayList<ObjectType> otList) {
		unitTypeList = new ArrayList<>(otList.size());
		for (ObjectType ot : otList) {
			Class<? extends Unit> ut = Input.checkCast(ot.getJavaClass(), Unit.class);
			unitTypeList.add(ut);
		}
	}

	public ArrayList<Class<? extends Unit>> getUnitTypeList() {
		return unitTypeList;
	}

	@Override
	public int getListSize() {
		if (unitTypeList == null)
			return 0;
		return unitTypeList.size();
	}

	@Override
	public void reset() {
		super.reset();
		unitTypeList = defaultUnitTypeList;
	}

	@Override
	public void copyFrom(Input<?> in) {
		super.copyFrom(in);
		setUnitTypeList(value);
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {
		Input.assertCountRange(kw, minCount, maxCount);
		value = Input.parseEntityList(kw, ObjectType.class, false);
		setUnitTypeList(value);
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<>();
		for (ObjectType each: Entity.getClonesOfIterator(ObjectType.class)) {
			Class<? extends Entity> klass = each.getJavaClass();
			if (klass == null)
				continue;

			if (Unit.class.isAssignableFrom(klass))
				list.add(each.getName());
		}
		Collections.sort(list);
		return list;
	}

}
