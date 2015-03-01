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

import com.jaamsim.input.OutputHandle;
import com.jaamsim.units.Unit;

public class SampleOutput implements SampleProvider {
	private OutputHandle out;

	public SampleOutput(OutputHandle o) {
		out = o;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return out.getUnitType();
	}

	@Override
	public double getNextSample(double simTime) {
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

}
