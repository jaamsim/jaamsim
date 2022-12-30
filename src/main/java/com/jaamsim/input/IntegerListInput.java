/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2021-2022 JaamSim Software Inc.
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
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.datatypes.IntegerVector;

public class IntegerListInput extends ListInput<IntegerVector> {
	private int minValue = Integer.MIN_VALUE;
	private int maxValue = Integer.MAX_VALUE;
	protected int[] validCounts; // valid list sizes not including units

	public IntegerListInput(String key, String cat, IntegerVector def) {
		super(key, cat, def);
		validCounts = new int[] { };
	}

	public void setDefaultValue(int... args) {
		IntegerVector def = new IntegerVector(args.length);
		for (int each : args) {
			def.add(each);
		}
		setDefaultValue(def);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		Input.assertCountRange(kw, minCount, maxCount);
		Input.assertCount(kw, validCounts);
		value = Input.parseIntegerVector(kw, minValue, maxValue);
	}

	@Override
	public int getListSize() {
		IntegerVector val = getValue();
		if (val == null)
			return 0;
		else
			return val.size();
	}

	public void setValidRange(int min, int max) {
		minValue = min;
		maxValue = max;
	}

	public void setValidCounts(int... list) {
		validCounts = list;
	}

	@Override
	public String getDefaultString(JaamSimModel simModel) {
		if (defValue == null || defValue.size() == 0)
			return "";

		StringBuilder tmp = new StringBuilder();
		tmp.append(defValue.get(0));
		for (int i = 1; i < defValue.size(); i++) {
			tmp.append(SEPARATOR);
			tmp.append(defValue.get(i));
		}

		return tmp.toString();
	}
}
