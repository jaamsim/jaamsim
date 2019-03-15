/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import com.jaamsim.ProbabilityDistributions.BooleanSelector;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.input.InputAgent;

public class TestBooleanSelector {

	JaamSimModel simModel;
	Simulation simulation;

	@Before
	public void setupTests() {
		simModel = new JaamSimModel();
		simulation = simModel.createInstance(Simulation.class);
	}

	@Test
	public void allTrue() {
		BooleanSelector selector = InputAgent.defineEntityWithUniqueName(simModel, BooleanSelector.class, "Dist", "-", true);
		InputAgent.applyArgs(selector, "RandomSeed", "1");
		selector.validate();
		selector.earlyInit();

		int numTrue = 0;
		int numFalse = 0;
		for (int i = 0; i < 1000000; i++) {
			if (selector.getNextValue())
				numTrue++;
			else
				numFalse++;
		}

		assertTrue(numTrue == 1000000);
		assertTrue(numFalse == 0);
	}

	@Test
	public void allFalse() {
		BooleanSelector selector = InputAgent.defineEntityWithUniqueName(simModel, BooleanSelector.class, "Dist", "-", true);
		InputAgent.applyArgs(selector, "RandomSeed", "1");
		InputAgent.applyArgs(selector, "TrueProbability", "0.0");
		selector.validate();
		selector.earlyInit();

		int numTrue = 0;
		int numFalse = 0;
		for (int i = 0; i < 1000000; i++) {
			if (selector.getNextValue())
				numTrue++;
			else
				numFalse++;
		}

		assertTrue(numTrue == 0);
		assertTrue(numFalse == 1000000);
	}

	@Test
	public void mixProb() {
		testMix(0.01, 1000000);
		testMix(0.25, 1000000);
		testMix(0.50, 1000000);
		testMix(0.75001, 1000000);
		testMix(0.99, 1000000);
	}

	private void testMix(double trueProb, int numSamples) {
		BooleanSelector selector = InputAgent.defineEntityWithUniqueName(simModel, BooleanSelector.class, "Dist", "-", true);
		InputAgent.applyArgs(selector, "RandomSeed", "1");
		InputAgent.applyArgs(selector, "TrueProbability", Double.toString(trueProb));
		selector.validate();
		selector.earlyInit();

		int numTrue = 0;
		int numFalse = 0;
		for (int i = 0; i < numSamples; i++) {
			if (selector.getNextValue())
				numTrue++;
			else
				numFalse++;
		}

		double mix = (double)numTrue / (numTrue + numFalse);
		double diff = Math.abs(trueProb - mix);
		//System.out.println("T:" + numTrue + " F:" + numFalse + " Diff:" + diff);
		assertTrue(diff < 0.001);
	}
}
