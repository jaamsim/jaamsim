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
package com.jaamsim.Samples;

import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.OutputChain;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.units.Unit;

public class SampleOutput implements SampleProvider {

	private final OutputChain chain;
	private final Class<? extends Unit> unitType;

	public SampleOutput(OutputChain ch, Class<? extends Unit> ut) {
		chain = ch;
		unitType = ut;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType;
	}

	@Override
	public double getNextSample(double simTime) {
		OutputHandle out = chain.getOutputHandle(simTime);

		if (out == null)
			throw new ErrorException("Output is null.");
		if (out.getUnitType() != unitType)
			throw new ErrorException("Unit mismatch. Expected a %s, received a %s", unitType, out.getUnitType());

		return out.getValueAsDouble(simTime, 0.0);
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

	@Override
	public String toString() {
		return chain.toString();
	}

}
