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

import com.jaamsim.BasicObjects.EntitlementSelector;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.InputAgent;

public class TestEntitlementSelector {

	JaamSimModel simModel;

	@Before
	public void setupTests() {
		simModel = new JaamSimModel();
	}

	@Test
	public void EntityCounts() {

		EntitlementSelector selector = InputAgent.defineEntityWithUniqueName(simModel, EntitlementSelector.class, "Dist", "-", true);
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
