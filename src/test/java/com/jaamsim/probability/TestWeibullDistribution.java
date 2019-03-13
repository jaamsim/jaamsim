/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019 JaamSim Software Inc.
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

import org.junit.Before;
import org.junit.Test;

import com.jaamsim.ProbabilityDistributions.WeibullDistribution;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.input.InputAgent;

public class TestWeibullDistribution {

	JaamSimModel simModel;

	@Before
	public void setupTests() {
		simModel = new JaamSimModel();
	}

	@Test
	public void MeanAndStandardDeviation() {
		ObjectType t = InputAgent.defineEntityWithUniqueName(simModel, ObjectType.class, "TestType", "-", true);
		InputAgent.applyArgs(t, "JavaClass", "com.jaamsim.units.DimensionlessUnit");

		WeibullDistribution dist = InputAgent.defineEntityWithUniqueName(simModel, WeibullDistribution.class, "Dist", "-", true);
		InputAgent.applyArgs(dist, "UnitType", t.getName());
		InputAgent.applyArgs(dist, "Scale", "10.0");
		InputAgent.applyArgs(dist, "Shape", "2.0");
		InputAgent.applyArgs(dist, "RandomSeed", "0");
		dist.validate();
		dist.earlyInit();

		int numSamples = 1000000;
		double total = TestContinuousDistribution.sampleDistribution(dist, numSamples);
		double mean = total / numSamples;

		assertTrue( Math.abs( dist.getSampleMean(0.0) - mean ) < 0.001 );
		assertTrue( Math.abs( dist.getSampleMean(0.0) / dist.getMeanValue(0.0) - 1.0 ) < 0.001 );
		assertTrue( Math.abs( dist.getSampleStandardDeviation(0.0) / dist.getStandardDeviation(0.0) - 1.0 ) < 0.001 );

		WeibullDistribution dist2 = InputAgent.defineEntityWithUniqueName(simModel, WeibullDistribution.class, "Dist", "-", true);
		InputAgent.applyArgs(dist2, "UnitType", t.getName());
		InputAgent.applyArgs(dist2, "Scale", "10.0");
		InputAgent.applyArgs(dist2, "Shape", "2.0");
		InputAgent.applyArgs(dist2, "Location", "100.0");
		InputAgent.applyArgs(dist2, "RandomSeed", "0");
		dist2.validate();
		dist2.earlyInit();

		double total2 = TestContinuousDistribution.sampleDistribution(dist2, numSamples);
		double mean2 = total2 / numSamples;
		assertTrue( Math.abs( dist2.getSampleMean(0.0) - mean2 ) < 0.001 );
		assertTrue( Math.abs( dist2.getSampleMean(0.0) / dist2.getMeanValue(0.0) - 1.0 ) < 0.001 );
		assertTrue( Math.abs( dist2.getSampleStandardDeviation(0.0) / dist2.getStandardDeviation(0.0) - 1.0 ) < 0.001 );

		assertTrue(Math.abs(dist.getSampleStandardDeviation(0.0) - dist2.getSampleStandardDeviation(0.0)) < 0.000001);
		assertTrue(Math.abs(dist.getStandardDeviation(0.0) - dist2.getStandardDeviation(0.0)) < 0.000001);
		assertTrue(Math.abs(dist.getSampleMean(0.0) - dist2.getSampleMean(0.0) + 100.0) < 0.000001);
	}
}
