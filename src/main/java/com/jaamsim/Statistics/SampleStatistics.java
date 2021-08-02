/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2021 JaamSim Software Inc.
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

import com.jaamsim.ProbabilityDistributions.StudentsTDistribution;

public class SampleStatistics {

	private long count;
	private double sum;
	private double sumSquared;
	private double minVal = Double.NaN;
	private double maxVal = Double.NaN;

	public SampleStatistics() {}

	public void clear() {
		count = 0L;
		sum = 0.0d;
		sumSquared = 0.0d;
		minVal = Double.NaN;
		maxVal = Double.NaN;
	}

	public void addValue(double val) {
		count++;
		sum += val;
		sumSquared += val*val;
		if (Double.isNaN(minVal) || val < minVal) {
			minVal = val;
		}
		if (Double.isNaN(maxVal) || val > maxVal) {
			maxVal = val;
		}
	}

	public long getCount() {
		return count;
	}

	public double getMin() {
		return minVal;
	}

	public double getMax() {
		return maxVal;
	}

	public double getSum() {
		return sum;
	}

	public double getSumSquared() {
		return sumSquared;
	}

	public double getMean() {
		return sum/count;
	}

	public double getMeanSquared() {
		return sumSquared/count;
	}

	public double getVariance() {
		return (sumSquared - sum*sum/count)/(count - 1L);
	}

	public double getStandardDeviation() {
		return Math.sqrt(getVariance());
	}

	public double getConfidenceInterval95() {
		if (count == 0L)
			return Double.NaN;
		int n = (int) count - 1;
		double factor = StudentsTDistribution.getConfidenceIntervalFactor95(n);
		return Math.sqrt(getVariance()/count) * factor;
	}

}
