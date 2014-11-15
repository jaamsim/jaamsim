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
package com.jaamsim.Samples;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpParser;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class SampleExpression implements SampleProvider {
	private final ExpParser.Expression exp;
	private final Entity thisEnt;

	public SampleExpression(ExpParser.Expression e, Entity ent) {
		exp = e;
		thisEnt = ent;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return DimensionlessUnit.class;
	}

	@Override
	public double getNextSample(double simTime) {
		double ret = 0.0;
		try {
			ret = ExpEvaluator.evaluateExpression(exp, simTime, thisEnt).value;
		}
		catch(ExpError e) {
			thisEnt.error("%s", e.getMessage());
		}
		return ret;
	}

	@Override
	public double getMeanValue(double simTime) {
		return 0;
	}

	@Override
	public double getMinValue() {
		return Double.NEGATIVE_INFINITY;
	}

	@Override
	public double getMaxValue() {
		return Double.POSITIVE_INFINITY;
	}

}
