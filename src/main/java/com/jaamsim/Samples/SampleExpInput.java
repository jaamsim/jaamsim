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
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class SampleExpInput extends Input<SampleProvider> {
	private Class<? extends Unit> unitType = DimensionlessUnit.class;
	private Entity thisEnt;
	private double minValue = Double.NEGATIVE_INFINITY;
	private double maxValue = Double.POSITIVE_INFINITY;

	public SampleExpInput(String key, String cat, SampleProvider def) {
		super(key, cat, def);
	}

	public void setUnitType(Class<? extends Unit> u) {
		if (u != unitType)
			this.reset();
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
		value = Input.parseSampleExp(kw, thisEnt, minValue, maxValue, unitType);
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

	@Override
	public void validate() {
		super.validate();

		if (value == null) return;
		if (value instanceof SampleExpression) return;
		if (value instanceof SampleConstant) return;

		Input.assertUnitsMatch(unitType, value.getUnitType());

		if (value.getMinValue() < minValue)
			throw new InputErrorException("The minimum value allowed for keyword: '%s' is: %s.\n" +
					"The specified entity: '%s' can return values as small as: %s.",
					this.getKeyword(), minValue, ((Entity)value).getName(), value.getMinValue());

		if (value.getMaxValue() > maxValue)
			throw new InputErrorException("The maximum value allowed for keyword: '%s' is: %s.\n" +
					"The specified entity: '%s' can return values as large as: %s.",
					this.getKeyword(), maxValue, ((Entity)value).getName(), value.getMaxValue());
	}
}
