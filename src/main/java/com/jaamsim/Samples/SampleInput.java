/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2019 JaamSim Software Inc.
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
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class SampleInput extends Input<SampleProvider> {
	private Class<? extends Unit> unitType;
	private Entity thisEnt;
	private double minValue = Double.NEGATIVE_INFINITY;
	private double maxValue = Double.POSITIVE_INFINITY;

	public SampleInput(String key, String cat, SampleProvider def) {
		super(key, cat, def);
	}

	public void setUnitType(Class<? extends Unit> u) {

		if (u == unitType)
			return;

		unitType = u;
		this.setValid(false);

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
	public void copyFrom(Entity thisEnt, Input<?> in) {
		super.copyFrom(thisEnt, in);

		// SampleExpressions must be re-parsed to reset the entity referred to by "this"
		if (value instanceof SampleExpression) {
			parseFrom(thisEnt, in);
		}
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		value = Input.parseSampleExp(kw, thisEnt, minValue, maxValue, unitType);
		this.setValid(true);
	}

	@Override
	public String getValidInputDesc() {
		if (unitType == UserSpecifiedUnit.class) {
			return Input.VALID_SAMPLE_PROV_UNIT;
		}
		if (unitType == DimensionlessUnit.class) {
			return Input.VALID_SAMPLE_PROV_DIMLESS;
		}
		return String.format(Input.VALID_SAMPLE_PROV, unitType.getSimpleName());
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		ArrayList<String> list = new ArrayList<>();
		JaamSimModel simModel = thisEnt.getJaamSimModel();
		for (Entity each : simModel.getClonesOfIterator(Entity.class, SampleProvider.class)) {
			SampleProvider sp = (SampleProvider)each;
			if (sp.getUnitType() == unitType && sp != thisEnt)
				list.add(each.getName());
		}
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null) return;

		// Preserve the exact text for a constant value input
		if (value instanceof SampleConstant) {
			super.getValueTokens(toks);
			return;
		}

		// All other inputs can be built from scratch
		toks.add(value.toString());
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
	public boolean useExpressionBuilder() {
		return true;
	}

	@Override
	public String getPresentValueString(double simTime) {
		if (value == null)
			return "";

		StringBuilder sb = new StringBuilder();
		Class<? extends Unit> ut = value.getUnitType();
		if (ut == DimensionlessUnit.class) {
			sb.append(Double.toString(value.getNextSample(simTime)));
		}
		else {
			String unitString = Unit.getDisplayedUnit(ut);
			double sifactor = Unit.getDisplayedUnitFactor(ut);
			sb.append(Double.toString(value.getNextSample(simTime)/sifactor));
			sb.append("[").append(unitString).append("]");
		}
		return sb.toString();
	}

}
