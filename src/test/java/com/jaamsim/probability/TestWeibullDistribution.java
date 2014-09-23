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
package com.jaamsim.probability;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import com.jaamsim.ProbabilityDistributions.WeibullDistribution;
import com.jaamsim.input.InputAgent;
import com.sandwell.JavaSimulation.ObjectType;

public class TestWeibullDistribution {

	@Test
	public void MeanAndStandardDeviation() {
		ObjectType t = InputAgent.defineEntityWithUniqueName(ObjectType.class, "TestType", true);
		InputAgent.processEntity_Keyword_Value( t, "JavaClass", "com.jaamsim.units.DimensionlessUnit");

		WeibullDistribution dist = InputAgent.defineEntityWithUniqueName(WeibullDistribution.class, "Dist", true);
		InputAgent.processEntity_Keyword_Value( dist, "UnitType", t.getInputName());
		InputAgent.processEntity_Keyword_Value( dist, "Scale", "10.0");
		InputAgent.processEntity_Keyword_Value( dist, "Shape", "2.0");
		dist.validate();
		dist.earlyInit();

		int numSamples = 1000000;
		double total = TestContinuousDistribution.sampleDistribution(dist, numSamples);
		double mean = total / numSamples;

		assertTrue( Math.abs( dist.getSampleMean(0.0) - mean ) < 0.001 );
		assertTrue( Math.abs( dist.getSampleMean(0.0) / dist.getMeanValue(0.0) - 1.0 ) < 0.001 );
		assertTrue( Math.abs( dist.getSampleStandardDeviation(0.0) / dist.getStandardDeviation(0.0) - 1.0 ) < 0.001 );

		WeibullDistribution dist2 = InputAgent.defineEntityWithUniqueName(WeibullDistribution.class, "Dist", true);
		InputAgent.processEntity_Keyword_Value( dist2, "UnitType", t.getInputName());
		InputAgent.processEntity_Keyword_Value( dist2, "Scale", "10.0");
		InputAgent.processEntity_Keyword_Value( dist2, "Shape", "2.0");
		InputAgent.processEntity_Keyword_Value( dist2, "Location", "100.0");
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
