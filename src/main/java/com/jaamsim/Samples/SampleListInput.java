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
package com.jaamsim.Samples;

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.ListInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;
import com.sandwell.JavaSimulation.Entity;

/**
 * SampleListInput is an object for parsing inputs consisting of a list of SampleProviders using the syntax:\n
 * Entity keyword { SampleProvider-1  SampleProvider-2 ... }\n
 * where SampleProvider-1 is an Entity that implements the SampleProvider interface, etc.
 * @author Harry King
 */
public class SampleListInput extends ListInput<ArrayList<SampleProvider>> {

	private Class<? extends Unit> unitType = DimensionlessUnit.class;

	public SampleListInput(String key, String cat, ArrayList<SampleProvider> def) {
		super(key, cat, def);
	}

	public void setUnitType(Class<? extends Unit> u) {
		unitType = u;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {

		ArrayList<SampleProvider> temp = new ArrayList<SampleProvider>(kw.numArgs());
		for (int i = 0; i < kw.numArgs(); i++) {

			Entity ent = Input.parseEntity(kw.getArg(i), Entity.class);
			SampleProvider s = Input.castImplements(ent, SampleProvider.class);
			if( s.getUnitType() != UserSpecifiedUnit.class )
				Input.assertUnitsMatch(unitType, s.getUnitType());

			temp.add(s);
		}

		value = temp;
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<String>();
		for (Entity each: Entity.getAll()) {
			if( (SampleProvider.class).isAssignableFrom(each.getClass()) ) {
			    list.add(each.getInputName());
			}
		}
		Collections.sort(list);
		return list;
	}

	@Override
	public String getValueString() {
		if( value == null)
			return "";

		StringBuilder tmp = new StringBuilder();
		tmp.append(value.get(0).toString());
		for (int i = 1; i < value.size(); i++) {
			tmp.append(SEPARATOR);
			tmp.append(value.get(i).toString());
		}
		return tmp.toString();
	}

}
