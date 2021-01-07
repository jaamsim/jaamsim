/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2021 JaamSim Software Inc.
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

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;

public class KeyedVec3dInput extends Input<Vec3d> {
	private Class<? extends Unit> unitType = DimensionlessUnit.class;
	private KeyedVec3dCurve curve = new KeyedVec3dCurve();

	public KeyedVec3dInput(String key, String cat) {
		super(key, cat, null);
	}

	public void setUnitType(Class<? extends Unit> units) {
		unitType = units;
	}

	@Override
	public void copyFrom(Entity thisEnt, Input<?> in) {
		super.copyFrom(thisEnt, in);
		curve = ((KeyedVec3dInput) in).curve;
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		ArrayList<String> strings = new ArrayList<>(kw.numArgs());
		for (int i = 0; i < kw.numArgs(); i++) {
			strings.add(kw.getArg(i));
		}
		ArrayList<ArrayList<String>> keys = InputAgent.splitForNestedBraces(strings);
		for( ArrayList<String> key : keys) {
			parseKey(thisEnt.getJaamSimModel(), key);
		}
	}

	private void parseKey(JaamSimModel simModel, ArrayList<String> key) throws InputErrorException {
		if (key.size() <= 2 || !key.get(0).equals("{") || !key.get(key.size()-1).equals("}")) {
			throw new InputErrorException("Malformed key entry: %s", key.toString());
		}

		ArrayList<ArrayList<String>> keyEntries = InputAgent.splitForNestedBraces(key.subList(1, key.size()-1));
		if (keyEntries.size() != 2) {
			throw new InputErrorException("Expected two values in keyed input for key entry: %s", key.toString());
		}
		ArrayList<String> timeInput = keyEntries.get(0);
		ArrayList<String> valInput = keyEntries.get(1);

		// Validate
		if (timeInput.size() != 4 || !timeInput.get(0).equals("{") || !timeInput.get(timeInput.size()-1).equals("}")) {
			throw new InputErrorException("Time entry not formated correctly: %s", timeInput.toString());
		}
		if (valInput.size() != 6 || !valInput.get(0).equals("{") || !valInput.get(valInput.size()-1).equals("}")) {
			throw new InputErrorException("Value entry not formated correctly: %s", valInput.toString());
		}

		KeywordIndex timeKw = new KeywordIndex("", timeInput, 1, 3, null);
		DoubleVector time = Input.parseDoubles(simModel, timeKw, 0.0d, Double.POSITIVE_INFINITY, TimeUnit.class);

		KeywordIndex valKw = new KeywordIndex("", valInput, 1, 5, null);
		DoubleVector vals = Input.parseDoubles(simModel, valKw, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, unitType);

		Vec3d val = new Vec3d(vals.get(0), vals.get(1), vals.get(2));
		curve.addKey(time.get(0), val);
	}

	@Override
	public Vec3d getValue() {
		return null;
	}

	public Vec3d getValueForTime(double time) {
		return curve.getValAtTime(time);
	}

	public boolean hasKeys() {
		return curve.hasKeys();
	}

	@Override
	public String getDefaultString(JaamSimModel simModel) {
		return "";
	}

}
