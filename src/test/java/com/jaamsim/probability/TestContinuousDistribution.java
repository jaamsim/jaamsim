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

import com.jaamsim.ProbabilityDistributions.ContinuousDistribution;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.events.TestFrameworkHelpers;
import com.jaamsim.input.InputAgent;

public class TestContinuousDistribution {
	static class SampleDistribution extends ProcessTarget {
		final SampleProvider dist;
		final int numSamples;
		double total;

		public SampleDistribution(SampleProvider dist, int numSamples) {
			this.dist = dist;
			this.numSamples = numSamples;
		}
		@Override
		public String getDescription() {
			return "DistibutionUnitTest";
		}

		@Override
		public void process() {
			total = 0.0d;
			for (int i = 0; i < numSamples; i++)
				total += dist.getNextSample(0.0d);
		}
	}

	static double sampleDistribution(SampleProvider dist, int numSamples) {
		SampleDistribution target = new SampleDistribution(dist, numSamples);
		EventManager evt = new EventManager("DistibutionUnitTest");
		evt.clear();

		evt.scheduleProcessExternal(0, 0, false, target, null);
		TestFrameworkHelpers.runEventsToTick(evt, Long.MAX_VALUE, 100000000);
		return target.total;
	}

	@Test
	public void MeanAndStandardDeviation() {
		ObjectType t = InputAgent.defineEntityWithUniqueName(ObjectType.class, "TestType", "-", true);
		InputAgent.processEntity_Keyword_Value( t, "JavaClass", "com.jaamsim.units.DimensionlessUnit");

		ContinuousDistribution dist = InputAgent.defineEntityWithUniqueName(ContinuousDistribution.class, "Dist", "-", true);
		InputAgent.processEntity_Keyword_Value( dist, "UnitType", t.getName());
		InputAgent.processEntity_Keyword_Value( dist, "ValueList", "1.0  3.0  5.0  10.0");
		InputAgent.processEntity_Keyword_Value( dist, "CumulativeProbabilityList", "0.0  0.5  0.8  1.0");
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
}
