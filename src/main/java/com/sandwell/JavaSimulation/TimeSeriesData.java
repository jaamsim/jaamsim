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

public class TimeSeriesData {

	private DoubleVector timeList;
	private DoubleVector valueList;
	private double maxValue;  // The maximum value that occurs in valueList
	private double minValue;  // The minimum value that occurs in valueList

	public TimeSeriesData( DoubleVector times, DoubleVector values ) {
		timeList = times;
		valueList = values;
		maxValue = values.getMax();
		minValue = values.getMin();
	}

	public DoubleVector getTimeList() {
		return timeList;
	}

	public DoubleVector getValueList() {
		return valueList;
	}

	public double getMaxValue() {
		return maxValue;
	}

	public double getMinValue() {
		return minValue;
	}
}
