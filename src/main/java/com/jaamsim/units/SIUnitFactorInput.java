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
package com.jaamsim.units;

import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;

public class SIUnitFactorInput extends Input<double[]> {
	private static final double[] defFactors = { 1.0d };
	private double siFactor;

	public SIUnitFactorInput(String key, String cat) {
	    super(key, cat, defFactors);
    }

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		DoubleVector temp = Input.parseDoubles(kw, 1e-15d, Double.POSITIVE_INFINITY, DimensionlessUnit.class);
		Input.assertCountRange(temp, 1, 2);
		double[] tmp = new double[temp.size()];
		tmp[0] = temp.get(0);
		if (temp.size() == 2)
			tmp[1] = temp.get(1);

		calculateSI(tmp);
		value = tmp;
	}

	final double getSIFactor() {
		return siFactor;
	}

	private void calculateSI(double[] factors) {
		siFactor = factors[0];
		if (factors.length == 2)
			siFactor /= factors[1];
	}

	@Override
	public void setDefaultValue(double[] val) {
		super.setDefaultValue(val);
		calculateSI(value);
	}

	@Override
	public String getDefaultString() {
		return "1.0";
	}
}
