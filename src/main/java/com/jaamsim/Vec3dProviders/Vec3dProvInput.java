/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2026 JaamSim Software Inc.
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
package com.jaamsim.Vec3dProviders;

import java.util.ArrayList;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Parser;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class Vec3dProvInput extends Input<Vec3dProvider> {

	private Class<? extends Unit> unitType = DimensionlessUnit.class;
	private double minValue = Double.NEGATIVE_INFINITY;
	private double maxValue = Double.POSITIVE_INFINITY;

	public Vec3dProvInput(String key, String cat, Vec3dProvider def) {
		super(key, cat, def);
	}

	public Vec3dProvInput(String key, String cat, Vec3d def) {
		this(key, cat, new Vec3dProvConstant(def));
	}

	@Override
	public String applyConditioning(String str) {

		// No changes required if the input is a list of numbers with or without a unit
		ArrayList<String> tokens = new ArrayList<>();
		Parser.tokenize(tokens, str, true);
		if (tokens.size() == 2 && Input.isDouble(tokens.get(0)))
			return str;
		if (tokens.size() == 3 && Input.isDouble(tokens.get(0)) && Input.isDouble(tokens.get(1)))
			return str;
		if (tokens.size() == 4 && Input.isDouble(tokens.get(0)) && Input.isDouble(tokens.get(1)) && Input.isDouble(tokens.get(2)))
			return str;

		return Parser.addQuotesIfNeeded(str);
	}

	public void setUnitType(Class<? extends Unit> ut) {
		unitType = ut;
	}

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	public void setDefaultValue(Vec3d def) {
		setDefaultValue(new Vec3dProvConstant(def));
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		value = Input.parseVec3dProvider(kw, thisEnt, minValue, maxValue, unitType);
		this.setValid(true);
	}

	@Override
	public String getValidInputDesc() {
		if (unitType == DimensionlessUnit.class) {
			return Input.VALID_VEC3D_PROV_DIMLESS;
		}
		return String.format(Input.VALID_VEC3D_PROV, unitType.getSimpleName());
	}

	@Override
	public String[] getExamples() {
		return Input.EXAMPLE_COLOUR_PROV;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null || isDef)
			return;

		// Preserve the exact text for a constant value input
		if (isConstant()) {
			super.getValueTokens(toks);
			return;
		}

		// All other inputs can be built from scratch
		toks.add(value.toString());
	}

	@Override
	public void appendEntityReferences(ArrayList<Entity> list) {
		value.appendEntityReferences(list);
	}

	@Override
	public boolean useExpressionBuilder() {
		return true;
	}

	@Override
	public String getPresentValueString(Entity thisEnt, double simTime) {
		if (value == null)
			return "";
		JaamSimModel simModel = thisEnt.getJaamSimModel();
		double factor = simModel.getDisplayedUnitFactor(unitType);
		String unitString = simModel.getDisplayedUnit(unitType);
		if (!unitString.isEmpty())
			unitString = "[" + unitString + "]";
		Vec3d vec = value.getNextVec3d(thisEnt, simTime);
		return InputAgent.getOutputString(simModel, vec, "%s", factor, unitString);
	}

	public Vec3d getNextVec3d(Entity thisEnt, double simTime) {
		try {
			return getValue().getNextVec3d(thisEnt, simTime);
		}
		catch (ErrorException e) {
			e.keyword = getKeyword();
			throw e;
		}
	}

	public boolean isConstant() {
		return (value instanceof Vec3dProvConstant);
	}

	@Override
	public String getDefaultString(JaamSimModel simModel) {
		if (defValue == null)
			return "";
		Vec3d vec = defValue.getNextVec3d(null, 0.0d);
		return Vec3dInput.toString(simModel, unitType, vec);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <V> V getValue(Entity thisEnt, double simTime, Class<V> klass) {
		return (V) getNextVec3d(thisEnt, simTime);
	}

	@Override
	public Class<?> getReturnType() {
		return Vec3d.class;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType;
	}
}
