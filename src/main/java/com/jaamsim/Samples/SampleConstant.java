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

public class SampleConstant implements SampleProvider {
	private Class<? extends Unit> unitType;
	private final double val;

	public SampleConstant(Class<? extends Unit> unitType, double val) {
		this.unitType = unitType;
		this.val = val;
	}

	public SampleConstant(double val) {
		this.unitType = Unit.class;
		this.val = val;
	}

	void setUnitType(Class<? extends Unit> ut) {
		unitType = ut;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType;
	}

	@Override
	public double getNextSample(double simTime) {
		return val;
	}

	@Override
	public double getMeanValue(double simTime) {
		return val;
	}

	@Override
	public double getMinValue() {
		return val;
	}

	@Override
	public double getMaxValue() {
		return val;
	}

	@Override
	public String toString() {
		StringBuilder tmp = new StringBuilder();
		tmp.append(val);
		if (unitType != Unit.class)
			tmp.append("  ").append(Unit.getSIUnit(unitType));
		return tmp.toString();
	}
}
