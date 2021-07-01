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
package com.jaamsim.probability;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jaamsim.ProbabilityDistributions.StudentsTDistribution;

public class TestStudentsTDistribution {

	@Test
	public void ConfidenceIntervalFactor() {
		assertTrue( StudentsTDistribution.getConfidenceIntervalFactor95(1) == 12.710d );
		assertTrue( StudentsTDistribution.getConfidenceIntervalFactor95(10) == 2.228d );
		assertTrue( StudentsTDistribution.getConfidenceIntervalFactor95(31) == 2.021d );
		assertTrue( StudentsTDistribution.getConfidenceIntervalFactor95(121) == 1.960d );
	}

}
