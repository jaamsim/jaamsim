/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.Samples;

import com.jaamsim.input.Input;
import com.jaamsim.ui.GUIFrame;
import com.jaamsim.units.Unit;

public class TimeSeriesConstantDouble implements TimeSeriesProvider {
	private Class<? extends Unit> unitType;
	private final double val;

	public TimeSeriesConstantDouble(Class<? extends Unit> unitType, double val) {
		this.unitType = unitType;
		this.val = val;
	}

	public TimeSeriesConstantDouble(double val) {
		this.unitType = Unit.class;
		this.val = val;
	}

	public void setUnitType(Class<? extends Unit> ut) {
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
	public double getValueForTicks(long ticks) {
		return val;
	}

	@Override
	public double getNextTimeAfter(double simTime) {
		return Double.POSITIVE_INFINITY;
	}

	@Override
	public long getLastChangeBeforeTicks(long ticks) {
		return Long.MAX_VALUE;
	}

	@Override
	public long getNextChangeAfterTicks(long ticks) {
		return Long.MAX_VALUE;
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
	public long getMaxTicksValue() {
		return 0;
	}

	@Override
	public double getMeanValue(double simTime) {
		return val;
	}

	@Override
	public String toString() {
		StringBuilder tmp = new StringBuilder();
		tmp.append(val/GUIFrame.getJaamSimModel().getDisplayedUnitFactor(unitType));
		if (unitType != Unit.class)
			tmp.append(Input.SEPARATOR).append(GUIFrame.getJaamSimModel().getDisplayedUnit(unitType));
		return tmp.toString();
	}

	@Override
	public long getInterpolatedTicksForValue(double val) {
		return 0;
	}

	@Override
	public double getInterpolatedCumulativeValueForTicks(long ticks) {
		return val;
	}
}
