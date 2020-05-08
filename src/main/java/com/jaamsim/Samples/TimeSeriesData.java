/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018 JaamSim Software Inc.
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

import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.events.EventManager;

public class TimeSeriesData {
	final EventManager evt;
	final long[] ticksList;   // time in clock ticks corresponding to each value
	final double[] valueList;
	private double maxValue;  // The maximum value that occurs in valueList
	private double minValue;  // The minimum value that occurs in valueList

	public TimeSeriesData(DoubleVector times, DoubleVector values, EventManager evt) {
		this.evt = evt;
		ticksList = new long[times.size()];
		for (int i = 0; i < times.size(); i++) {
			ticksList[i] = Math.round(times.get(i));
		}

		valueList = new double[values.size()];
		maxValue = Double.NEGATIVE_INFINITY;
		minValue = Double.POSITIVE_INFINITY;
		for (int i = 0; i < values.size(); i++) {
			valueList[i] = values.get(i);
			maxValue = Math.max(maxValue, valueList[i]);
			minValue = Math.min(minValue, valueList[i]);
		}
	}

	public double getMaxValue() {
		return maxValue;
	}

	public double getMinValue() {
		return minValue;
	}

	/**
	 * Tests whether the time series values are monotonically increasing or decreasing.
	 * @param dir - direction (positive = increasing, negative = decreasing)
	 * @return true if monotonic
	 */
	public boolean isMonotonic(int dir) {
		for (int i = 1; i < valueList.length; i++) {
			int comp = Double.compare(valueList[i], valueList[i - 1]);
			if (dir * comp < 0)
				return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{");
		for (int i = 0; i < ticksList.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			String str = String.format(" {%s[s], %s}", evt.ticksToSeconds(ticksList[i]), valueList[i]);
			sb.append(str);
		}
		sb.append(" }");
		return sb.toString();
	}

}
