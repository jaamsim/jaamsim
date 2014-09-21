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
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpParser;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;
import com.sandwell.JavaSimulation.Entity;

public class SampleExpInput extends Input<SampleProvider> {
	private Class<? extends Unit> unitType = DimensionlessUnit.class;
	private Entity thisEnt;

	public SampleExpInput(String key, String cat, SampleProvider def) {
		super(key, cat, def);
	}

	public void setUnitType(Class<? extends Unit> u) {
		unitType = u;
		if (defValue instanceof SampleConstant)
			((SampleConstant)defValue).setUnitType(unitType);
	}

	public void setEntity(Entity ent) {
		thisEnt = ent;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {

		// Try parsing a SampleProvider
		try {
			Input.assertCount(kw, 1);
			Entity ent = Input.parseEntity(kw.getArg(0), Entity.class);
			SampleProvider s = Input.castImplements(ent, SampleProvider.class);
			if( s.getUnitType() != UserSpecifiedUnit.class )
				Input.assertUnitsMatch(unitType, s.getUnitType());
			value = s;
			return;
		}
		catch (InputErrorException e) {}

		// Try parsing a constant value
		try {
			DoubleVector tmp = Input.parseDoubles(kw, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, unitType);
			Input.assertCount(tmp, 1);
			value = new SampleConstant(unitType, tmp.get(0));
			return;
		}
		catch (InputErrorException e) {}

		// Try parsing an expression
		try {
			Input.assertCount(kw, 1);
			Expression exp = ExpParser.parseExpression(ExpEvaluator.getContext(), kw.getArg(0));
			// Assume that the expression returns the correct unit type
			//Input.assertUnitsMatch(unitType, DimensionlessUnit.class);
			value = new SampleExpression(exp, thisEnt);
		}
		catch (ExpParser.Error e) {
			throw new InputErrorException(e.toString());
		}
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
		if (value instanceof SampleExpression || value instanceof SampleConstant)
			return super.getValueString();
		return value.toString();
	}

	public void verifyUnit() {
		// Assume that an expression has the correct unit type
		if (value instanceof SampleExpression)
			return;
		Input.assertUnitsMatch( unitType, value.getUnitType());
	}
}
