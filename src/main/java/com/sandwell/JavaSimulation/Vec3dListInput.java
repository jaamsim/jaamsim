/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation;

import java.util.ArrayList;

import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class Vec3dListInput extends ListInput<ArrayList<Vec3d>> {
	private Class<? extends Unit> unitType = DimensionlessUnit.class;

	public Vec3dListInput(String key, String cat, ArrayList<Vec3d> def) {
		super(key, cat, def);
	}

	public void setUnitType(Class<? extends Unit> units) {
		unitType = units;
	}

	@Override
	public void parse(StringVector input)
	throws InputErrorException {

		// Check if number of outer lists violate minCount or maxCount
		ArrayList<StringVector> splitData = InputAgent.splitStringVectorByBraces(input);
		if (splitData.size() < minCount || splitData.size() > maxCount)
			throw new InputErrorException(INP_ERR_RANGECOUNT, minCount, maxCount, input.toString());

		ArrayList<Vec3d> tempValue = new ArrayList<Vec3d>();
		for (StringVector innerInput : splitData) {
			DoubleVector temp = Input.parseDoubles(innerInput, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, unitType);
			// pad the vector to have 3 elements
			while (temp.size() < 3) {
				temp.add(0.0d);
			}

			tempValue.add(new Vec3d(temp.get(0), temp.get(1), temp.get(2)));
		}

		value = tempValue;
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return NO_VALUE;

		if (defValue.size() == 0)
			return NO_VALUE;

		StringBuilder tmp = new StringBuilder();
		for (Vec3d each: defValue) {

			// blank space between elements
			if (tmp.length() > 0)
				tmp.append(SEPARATOR);

			if (each == null) {
				tmp.append(NO_VALUE);
				continue;
			}

			tmp.append("{");
			tmp.append(SEPARATOR);
			tmp.append(each.x);
			tmp.append(SEPARATOR);
			tmp.append(each.y);
			tmp.append(SEPARATOR);
			tmp.append(each.z);
			tmp.append(SEPARATOR);
			if (unitType != Unit.class) {
				tmp.append(Unit.getSIUnit(unitType));
				tmp.append(SEPARATOR);
			}
			tmp.append("}");
		}
		return tmp.toString();
	}
}
