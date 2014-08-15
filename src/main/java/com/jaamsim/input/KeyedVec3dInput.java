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
package com.jaamsim.input;

import java.util.ArrayList;

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
	public void parse(KeywordIndex kw) throws InputErrorException {
		ArrayList<String> strings = new ArrayList<String>(kw.numArgs());
		for (int i = 0; i < kw.numArgs(); i++) {
			strings.add(kw.getArg(i));
		}
		ArrayList<ArrayList<String>> keys = InputAgent.splitForNestedBraces(strings);
		for( ArrayList<String> key : keys) {
			parseKey(key);
		}
	}

	private void parseKey(ArrayList<String> key) throws InputErrorException {
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

		DoubleVector time = Input.parseDoubles(timeInput.subList(1, 3), 0.0d, Double.POSITIVE_INFINITY, TimeUnit.class);
		DoubleVector vals = Input.parseDoubles(valInput.subList(1, 5), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, unitType);
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
	public String getDefaultString() {
		return "{ }";
	}

}
