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

public class SampleFrequency {

	private long[] binCounts;  // number of values recorded for each bin
	private int firstVal;  // value for the first bin
	private int minVal;  // minimum value recorded
	private int maxVal;  // maximum value recorded

	/**
	 * Constructs an object that collects frequency statistics on the number of times that
	 * individual integer values are observed. A separate bin is maintained for each integer
	 * in the range of values that are observed.
	 * @param val0 - lowest integer in the initial range of values to record
	 * @param val1 - highest integer in the initial range of values to record
	 */
	public SampleFrequency(int val0, int val1) {
		firstVal = val0;
		binCounts = new long[val1 - val0 + 1];
		minVal = val0;
		maxVal = val0;
	}

	public void clear() {
		Arrays.fill(binCounts, 0L);
		maxVal = minVal;
	}

	private void resize(int val0, int val1) {
		if (val0 > firstVal || val1 < firstVal + binCounts.length - 1)
			throw new ErrorException("Invalid resizing of bin array.");

		int offset = firstVal - val0;
		int num = val1 - val0 + 1;
		long[] newBinCounts = new long[num];
		System.arraycopy(binCounts, 0, newBinCounts, offset, binCounts.length);
		binCounts = newBinCounts;
		firstVal = val0;
	}

	private void resizeForValue(int val) {
		int val0 = firstVal;
		int val1 =  firstVal + binCounts.length - 1;
		if (val >= val0 && val <= val1)
			return;

		if (val < val0) {
			val0 = Math.min(val, val0 - binCounts.length);
		}
		else if (val > val1) {
			val1 = Math.max(val, val1 + binCounts.length);
		}
		resize(val0, val1);
	}

	/**
	 * Records the specified integer value.
	 * @param val - integer value to be recorded
	 */
	public void addValue(int val) {
		resizeForValue(val);
		int index = val - firstVal;
		binCounts[index]++;
		minVal = Math.min(minVal, val);
		maxVal = Math.max(maxVal, val);
	}

	/**
	 * Returns the total number of times an integer value was recorded.
	 * @return total number of times
	 */
	public long getCount() {
		long sum = 0L;
		int start = minVal - firstVal;
		int end = maxVal - firstVal;
		for (int i = start; i <= end; i++) {
			sum += binCounts[i];
		}
		return sum;
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
	 * Returns the total number of times the specified integer value was recorded.
	 * @param val - specified integer value
	 * @return total number of times
	 */
	public long getBinCount(int val) {
		int index = val - firstVal;
		return binCounts[index];
	}

	/**
	 * Returns the total number of times the specified integer value was recorded as a fraction of
	 * the total number of times that any value was recorded.
	 * @param val - specified integer value
	 * @return faction of values recorded
	 */
	public double getBinFraction(int val) {
		return (double)(getBinCount(val))/getCount();
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
	 * Returns an array containing the number of times each integer value was recorded, covering
	 * the range between the lowest and highest values.
	 * @return array of bin counts
	 */
	public long[] getBinCounts() {
		int num = maxVal - minVal + 1;
		int offset = minVal - firstVal;
		long[] ret = new long[num];
		System.arraycopy(binCounts, offset, ret, 0, num);
		return ret;
	}

	/**
	 * Returns an array containing the fractional number of times each integer value was recorded,
	 * covering the range between the lowest and highest values.
	 * @return array of values between 0 and 1
	 */
	public double[] getBinFractions() {
		int num = maxVal - minVal + 1;
		int offset = minVal - firstVal;
		double total = getCount();
		double[] ret = new double[num];
		for (int i = 0; i < num; i++) {
			ret[i] = binCounts[i + offset]/total;
		}
		return ret;
	}

	/**
	 * Returns an array containing the fractional number of times each integer value or less was
	 * recorded, covering the range between the lowest and highest values.
	 * @return array of values between 0 and 1
	 */
	public double[] getBinCumulativeFractions() {
		int num = maxVal - minVal + 1;
		int offset = minVal - firstVal;
		double total = getCount();
		double[] ret = new double[num];
		ret[0] = binCounts[offset]/total;
		for (int i = 1; i < num; i++) {
			ret[i] = ret[i - 1] + binCounts[i + offset]/total;
		}
		return ret;
	}

}
