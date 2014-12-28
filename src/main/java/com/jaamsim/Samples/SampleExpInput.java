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

import com.jaamsim.basicsim.Entity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpParser;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.input.ExpValidator;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class SampleExpInput extends Input<SampleProvider> {
	private Class<? extends Unit> unitType = DimensionlessUnit.class;
	private Entity thisEnt;
	private double minValue = Double.NEGATIVE_INFINITY;
	private double maxValue = Double.POSITIVE_INFINITY;

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

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {

		Input.assertCount(kw, 1, 2);

		// If there are two inputs, it must be a number and its unit
		if (kw.numArgs() == 2) {
			DoubleVector tmp = null;
			tmp = Input.parseDoubles(kw, minValue, maxValue, unitType);
			value = new SampleConstant(unitType, tmp.get(0));
			return;
		}

		// If there is only one input, it could be a SampleProvider, a dimensionless constant, or an expression

		// 1) Try parsing a SampleProvider
		SampleProvider s = null;
		try {
			Entity ent = Input.parseEntity(kw.getArg(0), Entity.class);
			s = Input.castImplements(ent, SampleProvider.class);
		}
		catch (InputErrorException e) {}

		if (s != null) {
			if (s.getUnitType() != UserSpecifiedUnit.class)
				Input.assertUnitsMatch(unitType, s.getUnitType());
			value = s;
			return;
		}

		// 2) Try parsing a constant value
		DoubleVector tmp = null;
		try {
			tmp = Input.parseDoubles(kw, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, DimensionlessUnit.class);
		}
		catch (InputErrorException e) {}

		if (tmp != null) {
			if (unitType != DimensionlessUnit.class)
				throw new InputErrorException(INP_ERR_UNITNOTFOUND, unitType.getSimpleName());
			if (tmp.get(0) < minValue || tmp.get(0) > maxValue)
				throw new InputErrorException(INP_ERR_DOUBLERANGE, minValue, maxValue, tmp.get(0));
			value = new SampleConstant(unitType, tmp.get(0));
			return;
		}

		// 3) Try parsing an expression
		try {
			Expression exp = ExpParser.parseExpression(ExpEvaluator.getParseContext(), kw.getArg(0));
			ExpValidator.validateExpression(exp, thisEnt, unitType);
			value = new SampleExpression(exp, thisEnt, unitType);
		}
		catch (ExpError e) {
			throw new InputErrorException(e.toString());
		}
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

		if (value instanceof SampleExpression || value instanceof SampleConstant) {
			super.getValueTokens(toks);
			return;
		}
		else {
			toks.add(((Entity)value).getName());
		}
	}

	public void verifyUnit() {
		// Assume that an expression has the correct unit type
		if (value instanceof SampleExpression)
			return;
		Input.assertUnitsMatch( unitType, value.getUnitType());
	}
}
