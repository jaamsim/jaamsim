/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2011 Ausenco Engineering Canada Inc.
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

public class IntegerInput extends Input<Integer> {
	private int minValue = Integer.MIN_VALUE;
	private int maxValue = Integer.MAX_VALUE;

	public IntegerInput(String key, String cat, Integer def) {
		super(key, cat, def);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);
		value = Input.parseInteger(kw.getArg(0), minValue, maxValue);
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_INTEGER;
	}

	public void setValidRange(int min, int max) {
		minValue = min;
		maxValue = max;
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return "";

		if (defValue.intValue() == Integer.MAX_VALUE)
			return POSITIVE_INFINITY;

		if (defValue.intValue() == Integer.MIN_VALUE)
			return NEGATIVE_INFINITY;

		StringBuilder tmp = new StringBuilder(defValue.toString());

		return tmp.toString();
	}
}
