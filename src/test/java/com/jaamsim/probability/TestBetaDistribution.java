package com.jaamsim.probability;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jaamsim.ProbabilityDistributions.BetaDistribution;
import com.jaamsim.input.InputAgent;
import com.sandwell.JavaSimulation.ObjectType;

public class TestBetaDistribution {

	@Test
	public void MeanAndStandardDeviation() {
		ObjectType t = InputAgent.defineEntityWithUniqueName(ObjectType.class, "TestType", "-", true);
		InputAgent.processEntity_Keyword_Value( t, "JavaClass", "com.jaamsim.units.DimensionlessUnit");

		BetaDistribution dist = InputAgent.defineEntityWithUniqueName(BetaDistribution.class, "Dist2", "-", true);
		InputAgent.processEntity_Keyword_Value( dist, "UnitType", t.getInputName());
		InputAgent.processEntity_Keyword_Value( dist, "AlphaParam", "2.0");
		InputAgent.processEntity_Keyword_Value( dist, "BetaParam", "2.0");
		dist.validate();
		dist.earlyInit();

		int numSamples = 100000;
		double total = TestContinuousDistribution.sampleDistribution(dist, numSamples);
		double mean = total / numSamples;

		assertTrue( Math.abs( dist.getSampleMean(0.0) - mean ) < 0.001 );
		assertTrue( Math.abs( dist.getSampleMean(0.0) / dist.getMeanValue(0.0) - 1.0 ) < 0.001 );
		assertTrue( Math.abs( dist.getSampleStandardDeviation(0.0) / dist.getStandardDeviation(0.0) - 1.0 ) < 0.005 );
	}


}
