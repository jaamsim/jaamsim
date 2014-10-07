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
import com.jaamsim.ProbabilityDistributions.EntitlementSelector;
import com.jaamsim.input.InputAgent;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class TestEntitlementSelector {

	@Test
	public void EntityCounts() {
		DisplayEntity entA = new DisplayEntity();
		DisplayEntity entB = new DisplayEntity();
		DisplayEntity entC = new DisplayEntity();
		entA.setInputName( "A" );
		entB.setInputName( "B" );
		entC.setInputName( "C" );

		EntitlementSelector selector = InputAgent.defineEntityWithUniqueName(EntitlementSelector.class, "Dist", "-", true);
		InputAgent.processEntity_Keyword_Value( selector, "EntityList", "A  B  C");
		InputAgent.processEntity_Keyword_Value( selector, "ProbabilityList", "0.5  0.3  0.2");
		selector.validate();
		selector.earlyInit();

		for(int i = 0; i<1000000; i++) {
			selector.nextValue();
		}

		double maxDiff = 0.0;
		double[] diff = selector.getSampleDifference(0.0);
		for( int i=0; i<diff.length; i++ ) {
			maxDiff = Math.max( Math.abs( diff[i] ), maxDiff );
		}

		assertTrue( maxDiff <= 1.0 );
	}
}
