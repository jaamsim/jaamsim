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
package com.jaamsim.Samples;

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.ListInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class SampleExpListInput extends ListInput<ArrayList<SampleProvider>> {

	private Class<? extends Unit> unitType = DimensionlessUnit.class;
	private Entity thisEnt;
	private double minValue = Double.NEGATIVE_INFINITY;
	private double maxValue = Double.POSITIVE_INFINITY;

	public SampleExpListInput(String key, String cat, ArrayList<SampleProvider> def) {
		super(key, cat, def);
	}

	public void setUnitType(Class<? extends Unit> u) {
		if (u != unitType)
			this.reset();
		unitType = u;

		if (defValue == null)
			return;
		for (SampleProvider p : defValue) {
			if (p instanceof SampleConstant)
				((SampleConstant) p).setUnitType(unitType);
		}
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
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<SampleProvider> temp = new ArrayList<>(subArgs.size());
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			try {
				SampleProvider sp = Input.parseSampleExp(subArg, thisEnt, minValue, maxValue, unitType);
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
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<>();
		for (Entity each : Entity.getClonesOfIterator(Entity.class, SampleProvider.class)) {
			SampleProvider samp = (SampleProvider)each;
			if (samp.getUnitType() == unitType)
				list.add(each.getName());
		}
		Collections.sort(list);
		return list;
	}

	public void validate() {

		for (SampleProvider sp : value) {

			if (sp instanceof SampleExpression) continue;
			if (sp instanceof SampleConstant) continue;

			Input.assertUnitsMatch(unitType, sp.getUnitType());

			if (sp.getMinValue() < minValue)
				throw new InputErrorException("The minimum value allowed for keyword: '%s' is: %s.\n" +
						"The specified entity: '%s' can return values as small as: %s.",
						this.getKeyword(), minValue, ((Entity)sp).getName(), sp.getMinValue());

			if (sp.getMaxValue() > maxValue)
				throw new InputErrorException("The maximum value allowed for keyword: '%s' is: %s.\n" +
						"The specified entity: '%s' can return values as large as: %s.",
						this.getKeyword(), maxValue, ((Entity)sp).getName(), sp.getMaxValue());
		}
	}

	@Override
	public String getDefaultString() {
		if (defValue == null || defValue.size() == 0) {
			return "";
		}

		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < defValue.size(); i++) {
			if (i > 0)
				tmp.append(SEPARATOR);

			tmp.append("{ ");
			tmp.append(defValue.get(i));

			if (defValue.get(i) instanceof SampleConstant && unitType != DimensionlessUnit.class)
				tmp.append(SEPARATOR).append(Unit.getSIUnit(unitType));

			tmp.append(" }");
		}

		return tmp.toString();
	}

}
