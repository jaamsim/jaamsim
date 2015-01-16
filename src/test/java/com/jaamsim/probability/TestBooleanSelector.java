/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import com.jaamsim.ProbabilityDistributions.BooleanSelector;
import com.jaamsim.input.InputAgent;

public class TestBooleanSelector {
	@Test
	public void allTrue() {
		BooleanSelector selector = InputAgent.defineEntityWithUniqueName(BooleanSelector.class, "Dist", "-", true);
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
		BooleanSelector selector = InputAgent.defineEntityWithUniqueName(BooleanSelector.class, "Dist", "-", true);
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
		BooleanSelector selector = InputAgent.defineEntityWithUniqueName(BooleanSelector.class, "Dist", "-", true);
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
