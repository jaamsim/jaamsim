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
import com.jaamsim.units.Unit;

public class StringProvInput extends Input<StringProvider> {

	private Class<? extends Unit> unitType;
	private Entity thisEnt;

	public StringProvInput(String key, String cat, StringProvider def) {
		super(key, cat, def);
		unitType = null;
		thisEnt = null;
	}

	public void setUnitType(Class<? extends Unit> ut) {
		if (ut != unitType)
			this.reset();
		unitType = ut;
	}

	public void setEntity(Entity ent) {
		thisEnt = ent;
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {
		value = Input.parseStringProvider(kw, thisEnt, unitType);
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<>();
		for (Entity each : Entity.getClonesOfIterator(Entity.class, SampleProvider.class)) {
			SampleProvider sp = (SampleProvider)each;
			if (sp.getUnitType() == unitType)
				list.add(each.getName());
		}
		Collections.sort(list);
		return list;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null) return;

		if (value instanceof StringProvOutput) {
			toks.add(value.toString());
			return;
		}

		super.getValueTokens(toks);
	}

}
