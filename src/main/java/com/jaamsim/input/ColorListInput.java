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
package com.jaamsim.input;

import java.util.ArrayList;

import com.jaamsim.math.Color4d;

public class ColorListInput extends ListInput<ArrayList<Color4d>>  {

	public ColorListInput(String key, String cat, ArrayList<Color4d> def) {
		super(key, cat, def);
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		Input.assertCountRange(kw, minCount, maxCount);
		value = Input.parseColorVector(kw);
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
		if (defValue == null)
			return NO_VALUE;

		if (defValue.size() == 0)
			return NO_VALUE;

		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < defValue.size(); i++) {

			// blank space between elements
			if (tmp.length() > 0)
				tmp.append(SEPARATOR);

			Color4d col = defValue.get(i);
			if (col == null) {
				tmp.append(NO_VALUE);
				continue;
			}

			tmp.append( String.format("{%s%.0f%s%.0f%s%.0f%s}", SEPARATOR, col.r * 255,
			   SEPARATOR, col.g * 255, SEPARATOR, col.b * 255, SEPARATOR ));
		}

		return tmp.toString();
	}
}
