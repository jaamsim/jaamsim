/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class ExpResult {
	public static final ExpResult BAD_RESULT = new ExpResult(Double.NaN, DimensionlessUnit.class);

	public double value;
	public Class<? extends Unit> unitType;

	public ExpResult(double val, Class<? extends Unit> ut) {
		value = val;
		unitType = ut;
	}

	public boolean isBad() {
		return this == BAD_RESULT;
	}
}
