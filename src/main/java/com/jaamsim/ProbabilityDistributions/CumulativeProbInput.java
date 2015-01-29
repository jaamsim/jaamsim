/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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
package com.jaamsim.ProbabilityDistributions;

import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.units.DimensionlessUnit;

public class CumulativeProbInput extends Input<DoubleVector>{
	public CumulativeProbInput(String key, String cat, DoubleVector def) {
		super(key, cat, def);
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		DoubleVector temp = Input.parseDoubles(kw, 0.0d, 1.0d, DimensionlessUnit.class);
		if (temp.get(0) != 0.0d)
			throw new InputErrorException("The first value of a cumulative probability list must be 0.0, got %f", temp.get(0));

		if (temp.get(temp.size() - 1) != 1.0d)
			throw new InputErrorException("The last value of a cumulative probability list must be 1.0, got %f", temp.get(temp.size() - 1));

		for (int i = 1; i < temp.size(); i++) {
			if (temp.get(i - 1) > temp.get(i))
				throw new InputErrorException("The values of a cumulative probability list must be strictly increasing");
		}

		value = temp;
	}
}
