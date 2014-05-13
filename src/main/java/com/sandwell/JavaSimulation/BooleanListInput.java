/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2011 Ausenco Engineering Canada Inc.
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

import com.jaamsim.input.Input;
import com.jaamsim.input.KeywordIndex;

public class BooleanListInput extends ListInput<BooleanVector> {

	public BooleanListInput(String key, String cat, BooleanVector def) {
		super(key, cat, def);
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		Input.assertCountRange(kw, minCount, maxCount);
		value = Input.parseBooleanVector(kw);
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return NO_VALUE;

		if (defValue.size() == 0)
			return NO_VALUE;

		StringBuilder tmp = new StringBuilder();
		if (defValue.get(0))
			tmp.append("TRUE");
		else
			tmp.append("FALSE");

		for (int i = 1; i < defValue.size(); i++) {
			tmp.append(SEPARATOR);

			if (defValue.get(i))
				tmp.append("TRUE");
			else
				tmp.append("FALSE");
		}
		return tmp.toString();
	}
}
