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
	final double[] timeList;
	final double[] valueList;
	private double maxValue;  // The maximum value that occurs in valueList
	private double minValue;  // The minimum value that occurs in valueList

	public TimeSeriesData( DoubleVector times, DoubleVector values ) {
		timeList = new double[times.size()];
		for (int i = 0; i < times.size(); i++)
			timeList[i] = times.get(i);

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
}
