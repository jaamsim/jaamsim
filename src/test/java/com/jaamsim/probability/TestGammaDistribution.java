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
import com.jaamsim.ProbabilityDistributions.GammaDistribution;
import com.jaamsim.input.InputAgent;
import com.sandwell.JavaSimulation.ObjectType;

public class TestGammaDistribution {

	@Test
	/**
	 * Tests the gamma distribution for Shape >= 1
	 */
	public void MeanAndStandardDeviation1() {
		ObjectType t = InputAgent.defineEntityWithUniqueName(ObjectType.class, "TestType", true);
		InputAgent.processEntity_Keyword_Value( t, "JavaClass", "com.jaamsim.units.DimensionlessUnit");

		GammaDistribution dist = InputAgent.defineEntityWithUniqueName(GammaDistribution.class, "Dist", true);
		InputAgent.processEntity_Keyword_Value( dist, "UnitType", t.getInputName());
		InputAgent.processEntity_Keyword_Value( dist, "Mean", "10.0");
		InputAgent.processEntity_Keyword_Value( dist, "Shape", "2.0");
		InputAgent.processEntity_Keyword_Value( dist, "RandomSeed", "1");
		dist.validate();
		dist.earlyInit();

		int numSamples = 1000000;
		double total = TestContinuousDistribution.sampleDistribution(dist, numSamples);
		double mean = total / numSamples;

		assertTrue( Math.abs( dist.getSampleMean(0.0) - mean ) < 0.001 );
		assertTrue( Math.abs( dist.getSampleMean(0.0) / dist.getMeanValue(0.0) - 1.0 ) < 0.001 );
		assertTrue( Math.abs( dist.getSampleStandardDeviation(0.0) / dist.getStandardDeviation(0.0) - 1.0 ) < 0.001 );
	}

	@Test
	/**
	 * Tests the gamma distribution for Shape < 1
	 */
	public void MeanAndStandardDeviation2() {
		ObjectType t = InputAgent.defineEntityWithUniqueName(ObjectType.class, "TestType", true);
		InputAgent.processEntity_Keyword_Value( t, "JavaClass", "com.jaamsim.units.DimensionlessUnit");

		GammaDistribution dist = InputAgent.defineEntityWithUniqueName(GammaDistribution.class, "Dist", true);
		InputAgent.processEntity_Keyword_Value( dist, "UnitType", t.getInputName());
		InputAgent.processEntity_Keyword_Value( dist, "Mean", "10.0");
		InputAgent.processEntity_Keyword_Value( dist, "Shape", "0.5");
		InputAgent.processEntity_Keyword_Value( dist, "RandomSeed", "1");
		dist.earlyInit();

		for(int i = 0; i<1000000; i++) {
			dist.getNextSample(0.0d);
		}
		assertTrue( Math.abs( dist.getSampleMean(0.0) / dist.getMeanValue(0.0) - 1.0 ) < 0.005 );
		assertTrue( Math.abs( dist.getSampleStandardDeviation(0.0) / dist.getStandardDeviation(0.0) - 1.0 ) < 0.005 );
	}
}
