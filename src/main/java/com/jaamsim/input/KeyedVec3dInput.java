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

import com.jaamsim.math.Vec3d;
import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringVector;

public class KeyedVec3dInput extends Input<Vec3d> {

	private KeyedVec3dCurve curve = new KeyedVec3dCurve();
	private String timeUnits;

	public KeyedVec3dInput(String key, String cat, String valUnits, String timeUnits) {
		super(key, cat, null);
		this.setUnits(valUnits);
		this.timeUnits = timeUnits;
	}

	@Override
	public void parse(StringVector input) throws InputErrorException {
		ArrayList<String> strings = new ArrayList<String>(input.size());
		for (String s : input) {
			strings.add(s);
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

		Unit timeUnit = Input.parseUnits(timeInput.get(2));
		Unit valUnit = Input.parseUnits(valInput.get(4));

		Unit defTimeUnit = Input.tryParseEntity(timeUnits.replaceAll("[()]", "").trim(), Unit.class);
		Unit defValUnit = Input.tryParseEntity(unitString.replaceAll("[()]", "").trim(), Unit.class);

		double timeConversionFactor = timeUnit.getConversionFactorToUnit(defTimeUnit);
		double valConversionFactor = valUnit.getConversionFactorToUnit(defValUnit);

		double time = Input.parseDouble(timeInput.get(1),
		                                Double.NEGATIVE_INFINITY,
		                                Double.POSITIVE_INFINITY,
		                                timeConversionFactor);

		double x = Input.parseDouble(valInput.get(1),
		                             Double.NEGATIVE_INFINITY,
		                             Double.POSITIVE_INFINITY,
		                             valConversionFactor);

		double y = Input.parseDouble(valInput.get(2),
		                             Double.NEGATIVE_INFINITY,
		                             Double.POSITIVE_INFINITY,
		                             valConversionFactor);

		double z = Input.parseDouble(valInput.get(3),
		                             Double.NEGATIVE_INFINITY,
		                             Double.POSITIVE_INFINITY,
		                             valConversionFactor);

		Vec3d val = new Vec3d(x, y, z);

		curve.addKey(time, val);
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
