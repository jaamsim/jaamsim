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

public class TimeBasedStatistics {

	private double startTime;
	private double lastTime;
	private double lastVal = Double.NaN;
	private double minVal = Double.NaN;
	private double maxVal = Double.NaN;
	private double weightedSum;
	private double weightedSumSquared;

	public TimeBasedStatistics() {}

	public void clear() {
		startTime = 0.0d;
		lastTime = 0.0d;
		lastVal = Double.NaN;
		minVal = Double.NaN;
		maxVal = Double.NaN;
		weightedSum = 0.0d;
		weightedSumSquared = 0.0d;
	}

	public void addValue(double t, double val) {
		if (Double.isNaN(lastVal)) {
			startTime = t;
		}
		else {
			double dt = t - lastTime;
			weightedSum += dt * lastVal;
			weightedSumSquared += dt * lastVal * lastVal;
		}
		if (Double.isNaN(minVal) || val < minVal) {
			minVal = val;
		}
		if (Double.isNaN(maxVal) || val > maxVal) {
			maxVal = val;
		}
		lastTime = t;
		lastVal = val;
	}

	public double getMin() {
		return minVal;
	}

	public double getMax() {
		return maxVal;
	}

	public double getSum(double t) {
		double dt = t - lastTime;
		return weightedSum + dt*lastVal;
	}

	public double getSumSquared(double t) {
		double dt = t - lastTime;
		return weightedSumSquared + dt*lastVal*lastVal;
	}

	public double getMean(double t) {
		return getSum(t) / (t - startTime);
	}

	public double getMeanSquared(double t) {
		return getSumSquared(t) / (t - startTime);
	}

	public double getVariance(double t) {
		double mean = getMean(t);
		return getMeanSquared(t) - mean*mean;
	}

	public double getStandardDeviation(double t) {
		return Math.sqrt(getVariance(t));
	}

}
