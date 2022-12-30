/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2021 JaamSim Software Inc.
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
package com.jaamsim.input;

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.Samples.TimeSeriesConstantDouble;
import com.jaamsim.Samples.TimeSeriesProvider;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class TimeSeriesInput extends Input<TimeSeriesProvider> {
	private Class<? extends Unit> unitType = DimensionlessUnit.class;

	public TimeSeriesInput(String key, String cat, TimeSeriesProvider def) {
		super(key, cat, def);
	}

	public void setUnitType(Class<? extends Unit> u) {
		if (u != unitType)
			this.reset();
		unitType = u;
		if (defValue instanceof TimeSeriesConstantDouble)
			((TimeSeriesConstantDouble)defValue).setUnitType(unitType);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		Input.assertCountRange(kw, 1, 2);

		// Try to parse as a constant value
		try {
			DoubleVector tmp = Input.parseDoubles(thisEnt.getJaamSimModel(), kw, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, unitType);
			Input.assertCount(tmp, 1);
			value = new TimeSeriesConstantDouble(unitType, tmp.get(0));
			return;
		}
		catch (InputErrorException e) {}

		// If not a constant, try parsing a TimeSeriesProvider
		Input.assertCount(kw, 1);
		Entity ent = Input.parseEntity(thisEnt.getJaamSimModel(), kw.getArg(0), Entity.class);
		TimeSeriesProvider s = Input.castImplements(ent, TimeSeriesProvider.class);
		if( s.getUnitType() != UserSpecifiedUnit.class )
			Input.assertUnitsMatch(unitType, s.getUnitType());
		value = s;
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		ArrayList<String> list = new ArrayList<>();
		JaamSimModel simModel = ent.getJaamSimModel();
		for (Entity each : simModel.getClonesOfIterator(Entity.class, TimeSeriesProvider.class)) {
			TimeSeriesProvider tsp = (TimeSeriesProvider)each;
			if (tsp.getUnitType() == unitType)
				list.add(each.getName());
		}
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null || isDef)
			return;

		if (value instanceof TimeSeriesConstantDouble) {
			super.getValueTokens(toks);
			return;
		}
		else {
			toks.add(((Entity)value).getName());
		}
	}

	@Override
	public boolean removeReferences(Entity ent) {
		if (value == ent) {
			this.reset();
			return true;
		}
		return false;
	}

	@Override
	public void appendEntityReferences(ArrayList<Entity> list) {
		if (!(value instanceof Entity) || list.contains(value))
			return;
		list.add((Entity) value);
	}

	@Override
	public String getDefaultString(JaamSimModel simModel) {
		if (defValue instanceof TimeSeriesConstantDouble) {
			return ((TimeSeriesConstantDouble) defValue).getValueString(simModel);
		}
		return super.getDefaultString(simModel);
	}

}
