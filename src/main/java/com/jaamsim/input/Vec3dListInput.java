/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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
import com.jaamsim.datatypes.DoubleVector;
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
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {

		// Check if number of outer lists violate minCount or maxCount
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		if (subArgs.size() < minCount || subArgs.size() > maxCount) {
			if (maxCount == Integer.MAX_VALUE)
				throw new InputErrorException(INP_ERR_RANGECOUNTMIN, minCount, kw.argString());
			throw new InputErrorException(INP_ERR_RANGECOUNT, minCount, maxCount, kw.argString());
		}

		ArrayList<Vec3d> tempValue = new ArrayList<>(subArgs.size());
		for (KeywordIndex subArg : subArgs) {
			DoubleVector temp = Input.parseDoubles(subArg, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, unitType);
			// pad the vector to have 3 elements
			while (temp.size() < 3) {
				temp.add(0.0d);
			}

			tempValue.add(new Vec3d(temp.get(0), temp.get(1), temp.get(2)));
		}

		value = tempValue;
	}

	@Override
	public String getValidInputDesc() {
		return String.format(Input.VALID_VEC3D_LIST, unitType.getSimpleName());
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	@Override
	public String getDefaultString() {
		if (defValue == null || defValue.isEmpty())
			return "";

		double factor = Unit.getDisplayedUnitFactor(unitType);
		String unitStr = Unit.getDisplayedUnit(unitType);

		StringBuilder tmp = new StringBuilder();
		for (Vec3d each: defValue) {

			// blank space between elements
			if (tmp.length() > 0)
				tmp.append(BRACE_SEPARATOR);

			if (each == null) {
				tmp.append("");
				continue;
			}

			tmp.append("{");
			tmp.append(BRACE_SEPARATOR);
			tmp.append(each.x/factor);
			tmp.append(SEPARATOR);
			tmp.append(each.y/factor);
			tmp.append(SEPARATOR);
			tmp.append(each.z/factor);
			if (!unitStr.isEmpty()) {
				tmp.append(SEPARATOR);
				tmp.append(unitStr);
			}
			tmp.append(BRACE_SEPARATOR);
			tmp.append("}");
		}
		return tmp.toString();
	}

}
