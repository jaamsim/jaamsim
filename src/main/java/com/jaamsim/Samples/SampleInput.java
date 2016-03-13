/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.Samples;

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.units.Unit;

public class SampleInput extends Input<SampleProvider> {
	private Class<? extends Unit> unitType;
	private Entity thisEnt;
	private double minValue = Double.NEGATIVE_INFINITY;
	private double maxValue = Double.POSITIVE_INFINITY;

	public SampleInput(String key, String cat, SampleProvider def) {
		super(key, cat, def);
	}

	public void setUnitType(Class<? extends Unit> u) {
		if (u != unitType)
			this.reset();
		unitType = u;
		if (defValue instanceof SampleConstant)
			((SampleConstant)defValue).setUnitType(unitType);
		if (defValue instanceof TimeSeriesConstantDouble)
			((TimeSeriesConstantDouble)defValue).setUnitType(unitType);
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
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}


	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null) return;

		if (value instanceof SampleExpression ||
				value instanceof SampleConstant ||
				value instanceof SampleOutput) {
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
