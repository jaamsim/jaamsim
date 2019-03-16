/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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

import com.jaamsim.basicsim.Entity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class Vec3dInput extends Input<Vec3d> {
	private Class<? extends Unit> unitType = DimensionlessUnit.class;
	private double minValue = Double.NEGATIVE_INFINITY;
	private double maxValue = Double.POSITIVE_INFINITY;

	public Vec3dInput(String key, String cat, Vec3d def) {
		super(key, cat, def);
	}

	public void setUnitType(Class<? extends Unit> units) {
		unitType = units;
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		DoubleVector temp = Input.parseDoubles(thisEnt.getJaamSimModel(), kw, minValue, maxValue, unitType);
		Input.assertCountRange(temp, 1, 3);

		// pad the vector to have 3 elements
		while (temp.size() < 3) {
			temp.add(0.0d);
		}

		value = new Vec3d(temp.get(0), temp.get(1), temp.get(2));
	}

	@Override
	public String getValidInputDesc() {
		if (unitType == DimensionlessUnit.class) {
			return Input.VALID_VEC3D_DIMLESS;
		}
		return String.format(Input.VALID_VEC3D, unitType.getSimpleName());
	}

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return "";

		double factor = Unit.getDisplayedUnitFactor(unitType);

		StringBuilder tmp = new StringBuilder();
		tmp.append(defValue.x/factor);
		tmp.append(SEPARATOR);
		tmp.append(defValue.y/factor);
		tmp.append(SEPARATOR);
		tmp.append(defValue.z/factor);
		if (unitType != Unit.class) {
			tmp.append(SEPARATOR);
			tmp.append(Unit.getDisplayedUnit(unitType));
		}

		return tmp.toString();
	}
}
