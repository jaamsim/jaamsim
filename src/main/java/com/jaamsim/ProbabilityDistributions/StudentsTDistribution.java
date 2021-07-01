/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2021 JaamSim Software Inc.
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
package com.jaamsim.ProbabilityDistributions;

import java.util.Arrays;

public class StudentsTDistribution {

	private static int[] degreesOfFreedom = {
			     1,     2,     3,     4,     5,     6,     7,     8,     9,    10,
			    11,    12,    13,    14,    15,    16,    17,    18,    19,    20,
			    21,    22,    23,    24,    25,    26,    27,    28,    29,    30,
			    40,    50,    60,    80,   100,   120};

	private static double[] confidenceIntervalFactor95 = {
			12.710, 4.303, 3.182, 2.776, 2.571, 2.447, 2.365, 2.306, 2.262, 2.228,
			 2.201, 2.179, 2.160, 2.145, 2.131, 2.120, 2.110, 2.101, 2.093, 2.086,
			 2.080, 2.074, 2.069, 2.064, 2.060, 2.056, 2.052, 2.048, 2.045, 2.042,
			 2.021, 2.009, 2.000, 1.990, 1.984, 1.980, 1.960};

	/**
	 * Return the Student's T factor corresponding to a 95% confidence interval for the specified
	 * number of degrees of freedom.
	 * Adapted from the Wikipedia article "Student's t-distribution", downloaded July 1, 2021.
	 * @param n - degrees of freedom
	 * @return factor for a 95% confidence interval
	 */
	public static double getConfidenceIntervalFactor95(int n) {
		int k = Arrays.binarySearch(degreesOfFreedom, n);
		int index = (k >= 0) ? k : -k - 1;
		return confidenceIntervalFactor95[index];
	}

}
