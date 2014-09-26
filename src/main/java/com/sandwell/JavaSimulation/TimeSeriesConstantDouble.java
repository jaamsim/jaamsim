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
package com.sandwell.JavaSimulation;

import com.jaamsim.units.Unit;

public class TimeSeriesConstantDouble implements TimeSeriesProvider {
	private final Class<? extends Unit> unitType;
	private final double val;

	public TimeSeriesConstantDouble(Class<? extends Unit> unitType, double val) {
		this.unitType = unitType;
		this.val = val;
	}

	public TimeSeriesConstantDouble(double val) {
		this.unitType = Unit.class;
		this.val = val;
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
	public double getNextChangeTimeAfterHours( double simTime ) {
		return Double.POSITIVE_INFINITY;
	}

	@Override
	public double getNextTimeAfter(double simTime) {
		return Double.POSITIVE_INFINITY;
	}

	@Override
	public double getMaxValue() {
		return val;
	}

	@Override
	public double getMinValue() {
		return val;
	}

	@Override
	public double getMaxTimeValue() {
		return 0.0;
	}

	@Override
	public double getMeanValue(double simTime) {
		return val;
	}
}
