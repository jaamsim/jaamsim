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

import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;
import com.sandwell.JavaSimulation.Entity;

public class SampleInput extends Input<SampleProvider> {
	private Class<? extends Unit> unitType = DimensionlessUnit.class;

	public SampleInput(String key, String cat, SampleProvider def) {
		super(key, cat, def);
	}

	public void setUnitType(Class<? extends Unit> u) {
		unitType = u;
		if (defValue instanceof SampleConstant)
			((SampleConstant)defValue).setUnitType(unitType);
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		// Try to parse as a constant value
		try {
			DoubleVector tmp = Input.parseDoubles(kw, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, unitType);
			Input.assertCount(tmp, 1);
			value = new SampleConstant(unitType, tmp.get(0));
			return;
		}
		catch (InputErrorException e) {}

		// If not a constant, try parsing a SampleProvider
		Input.assertCount(kw, 1);
		Entity ent = Input.parseEntity(kw.getArg(0), Entity.class);
		SampleProvider s = Input.castImplements(ent, SampleProvider.class);
		if( s.getUnitType() != UserSpecifiedUnit.class )
			Input.assertUnitsMatch(unitType, s.getUnitType());
		value = s;
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<String>();
		for (Entity each : Entity.getClonesOfIterator(Entity.class, SampleProvider.class)) {
			SampleProvider sp = (SampleProvider)each;
			if (sp.getUnitType() == unitType)
				list.add(each.getInputName());
		}
		Collections.sort(list);
		return list;
	}

	@Override
	public String getValueString() {
		if (value == null || defValue == value)
			return "";
		if (value instanceof SampleConstant)
			return super.getValueString();
		return value.toString();
	}

	public void verifyUnit() {
		Input.assertUnitsMatch( unitType, value.getUnitType());
	}
}
