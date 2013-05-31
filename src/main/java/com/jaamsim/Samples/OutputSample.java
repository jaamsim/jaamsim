/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.Entity;

public class OutputSample implements SampleProvider {
	private final Entity ent;
	private final String output;
	private final Class<?> retType;

	public OutputSample(Entity ent, String output) {
		this.ent = ent;
		this.output = output;
		retType = ent.getOutputType(output);
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return ent.getOutputUnit(output);
	}

	@Override
	public double getNextSample(double simTime) {
		if (retType == Double.class)
			return ent.getOutputValue(output, simTime, Double.class);
		else
			return ent.getOutputValue(output, simTime, double.class);
	}

	@Override
	public String toString() {
		StringBuilder tmp = new StringBuilder();
		tmp.append(ent.getInputName()).append("  ").append(output);
		return tmp.toString();
	}
}
