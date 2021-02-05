/*
 * JaamSim Discrete Event Simulation
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
package com.jaamsim.Statistics;

import java.util.Arrays;

import com.jaamsim.basicsim.ErrorException;

public class TimeBasedFrequency {

	private double startTime;
	private double lastTime;
	private int lastVal = 0;
	private double[] binTimes;  // total time recorded for each bin
	private int firstVal;  // value for the first bin
	private int minVal;  // minimum value recorded
	private int maxVal;  // maximum value recorded

	/**
	 * Constructs an object that collects frequency statistics on the total time that
	 * individual integer values are observed. A separate bin is maintained for each integer
	 * in the range of values that are observed.
	 * @param val0 - lowest integer in the initial range of values to record
	 * @param val1 - highest integer in the initial range of values to record
	 */
	public TimeBasedFrequency(int val0, int val1) {
		startTime = -1.0d;
		firstVal = val0;
		binTimes = new double[val1 - val0 + 1];
		minVal = val0;
		maxVal = val0;
	}

	public void clear() {
		startTime = -1.0d;
		lastTime = -1.0d;
		Arrays.fill(binTimes, 0.0d);
		maxVal = minVal;
	}

	private void resize(int val0, int val1) {
		if (val0 > firstVal || val1 < firstVal + binTimes.length - 1)
			throw new ErrorException("Invalid resizing of bin array.");

		int offset = firstVal - val0;
		int num = val1 - val0 + 1;
		double[] newBinTimes = new double[num];
		System.arraycopy(binTimes, 0, newBinTimes, offset, binTimes.length);
		binTimes = newBinTimes;
		firstVal = val0;
	}

	private void resizeForValue(int val) {
		int val0 = firstVal;
		int val1 =  firstVal + binTimes.length - 1;
		if (val >= val0 && val <= val1)
			return;

		if (val < val0) {
			val0 = Math.min(val, val0 - binTimes.length);
		}
		else if (val > val1) {
			val1 = Math.max(val, val1 + binTimes.length);
		}
		resize(val0, val1);
	}

	/**
	 * Records the specified integer value at the specified time.
	 * @param t - time at which the value occurs
	 * @param val - integer value to be recorded
	 */
	public void addValue(double t, int val) {
		resizeForValue(val);

		if (startTime < 0.0d) {
			startTime = t;
			lastTime = t;
			minVal = val;
			maxVal = val;
			lastVal = val;
			return;
		}

		int index = lastVal - firstVal;
		binTimes[index] += t - lastTime;
		minVal = Math.min(minVal, val);
		maxVal = Math.max(maxVal, val);
		lastTime = t;
		lastVal = val;
	}

	/**
	 * Returns the total time recorded for the integer values up to the specified time.
	 * @param t - specified time
	 * @return total time
	 */
	public double getTotalTime(double t) {
		return t - startTime;
	}

	/**
	 * Returns the minimum integer value that was recorded.
	 * @return minimum integer value
	 */
	public int getMin() {
		return minVal;
	}

	/**
	 * Returns the maximum integer value that was recorded.
	 * @return maximum integer value
	 */
	public int getMax() {
		return maxVal;
	}

	/**
	 * Returns the total time during which the specified integer value applied up to the
	 * specified time.
	 * @param t - specified time
	 * @param val - specified integer value
	 * @return total time
	 */
	public double getBinTime(double t, int val) {
		int index = val - firstVal;
		double ret = binTimes[index];
		if (val == lastVal) {
			ret += t - lastTime;
		}
		return ret;
	}

	/**
	 * Returns the total time during which the specified integer value applied up to the
	 * specified time as a fraction of the total time.
	 * @param t - specified time
	 * @param val - specified integer value
	 * @return faction of total time
	 */
	public double getBinFraction(double t, int val) {
		return getBinTime(t, val)/(t - startTime);
	}

	/**
	 * Returns an array of the integer values covering the range between the lowest and highest
	 * values that were recorded.
	 * @return array of integer values
	 */
	public int[] getBinValues() {
		int num = maxVal - minVal + 1;
		int[] ret = new int[num];
		for (int i = 0; i < num; i++) {
			ret[i] = minVal + i;
		}
		return ret;
	}

	/**
	 * Returns an array containing the total time that each integer value applied up to the
	 * specified time, covering the range between the lowest and highest values.
	 * @param t - specified time
	 * @return array of total times
	 */
	public double[] getBinTimes(double t) {
		int num = maxVal - minVal + 1;
		int offset = minVal - firstVal;
		double[] ret = new double[num];
		System.arraycopy(binTimes, offset, ret, 0, num);
		int index = lastVal - minVal;
		ret[index] += t - lastTime;
		return ret;
	}

	/**
	 * Returns an array containing the fractional time that each integer value applied,
	 * covering the range between the lowest and highest values.
	 * @param t - specified time
	 * @return array of values between 0 and 1
	 */
	public double[] getBinFractions(double t) {
		double[] ret = getBinTimes(t);
		double total = t - startTime;
		for (int i = 0; i < ret.length; i++) {
			ret[i] = ret[i]/total;
		}
		return ret;
	}

}
