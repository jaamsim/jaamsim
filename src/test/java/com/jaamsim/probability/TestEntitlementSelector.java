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

import com.jaamsim.BasicObjects.EntitlementSelector;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.InputAgent;

public class TestEntitlementSelector {

	@Test
	public void EntityCounts() {

		EntitlementSelector selector = InputAgent.defineEntityWithUniqueName(EntitlementSelector.class, "Dist", "-", true);
		InputAgent.applyArgs(selector, "ProportionList", "0.5", "0.3", "0.2");
		selector.validate();
		selector.earlyInit();

		int numSamples = 1000000;
		TestContinuousDistribution.sampleDistribution(selector, numSamples);

		double maxDiff = 0.0;
		DoubleVector diff = selector.getSampleDifference(0.0);
		for( int i=0; i<diff.size(); i++ ) {
			maxDiff = Math.max( Math.abs( diff.get(i) ), maxDiff );
		}

		assertTrue( maxDiff <= 1.0 );
	}
}
