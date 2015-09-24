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
package com.jaamsim.StringProviders;

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.ListInput;
import com.jaamsim.units.Unit;

public class StringProvListInput extends ListInput<ArrayList<StringProvider>> {

	private ArrayList<Class<? extends Unit>> unitTypeList;
	private Entity thisEnt;

	public StringProvListInput(String key, String cat, ArrayList<StringProvider> def) {
		super(key, cat, def);
		// TODO Auto-generated constructor stub
	}

	public void setUnitTypeList(ArrayList<Class<? extends Unit>> utList) {
		unitTypeList = new ArrayList<>(utList);
	}

	public void setUnitType(Class<? extends Unit> u) {
		ArrayList<Class<? extends Unit>> utList = new ArrayList<>(1);
		utList.add(u);
		this.setUnitTypeList(utList);
	}

	/**
	 * Returns the unit type for the specified expression.
	 * <p>
	 * If the number of expressions exceeds the number of unit types
	 * then the last unit type in the list is returned.
	 * @param i - index of the expression
	 * @return unit type for the expression
	 */
	public Class<? extends Unit> getUnitType(int i) {
		if (unitTypeList.isEmpty())
			return null;
		int k = Math.min(i, unitTypeList.size()-1);
		return unitTypeList.get(k);
	}

	public void setEntity(Entity ent) {
		thisEnt = ent;
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<StringProvider> temp = new ArrayList<>(subArgs.size());
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			try {
				StringProvider sp = Input.parseStringProvider(subArg, thisEnt, getUnitType(i));
				temp.add(sp);
			}
			catch (InputErrorException e) {
				if (subArgs.size() == 1)
					throw new InputErrorException(e.getMessage());
				else
					throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}
		value = temp;
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<>();
		for (Entity each : Entity.getClonesOfIterator(Entity.class, SampleProvider.class)) {
			SampleProvider samp = (SampleProvider)each;
			if (unitTypeList.contains(samp.getUnitType()))
				list.add(each.getName());
		}
		Collections.sort(list);
		return list;
	}

}
