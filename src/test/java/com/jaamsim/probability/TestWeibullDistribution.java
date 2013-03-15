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

public class TestWeibullDistribution {

	@Test
	public void MeanAndStandardDeviation() {
		WeibullDistribution dist = new WeibullDistribution();
		InputAgent.processEntity_Keyword_Value( dist, "Scale", "10.0");
		InputAgent.processEntity_Keyword_Value( dist, "Shape", "2.0");
		dist.validate();
		dist.earlyInit();

		for(int i = 0; i<1000000; i++) {
			dist.nextValue();
		}
		assertTrue( Math.abs( dist.getSampleMean(0.0) / dist.getMeanValue(0.0) - 1.0 ) < 0.001 );
		assertTrue( Math.abs( dist.getSampleStandardDeviation(0.0) / dist.getStandardDeviation(0.0) - 1.0 ) < 0.001 );
	}
}
