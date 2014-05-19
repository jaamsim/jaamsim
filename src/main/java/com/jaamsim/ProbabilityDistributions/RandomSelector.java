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
package com.jaamsim.ProbabilityDistributions;

import java.util.Random;

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.IntegerInput;

/**
 * RandomSelector randomly selects the next entity to return based on the given probabilities.
 * @author Harry King
 *
 */
public class RandomSelector extends ObjectSelector {

	@Keyword(description = "Seed for the random number generator.  Must be an integer > 0.",
			 example = "RandomSelector1 RandomSeed { 547 }")
	private IntegerInput randomSeedInput;

	protected final Random randomGenerator;

	{
		randomSeedInput = new IntegerInput("RandomSeed", "Key Inputs", 1);
		randomSeedInput.setValidRange( 1, Integer.MAX_VALUE);
		this.addInput(randomSeedInput);
	}

	public RandomSelector() {
		randomGenerator = new Random();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		randomGenerator.setSeed( randomSeedInput.getValue() );
	}

	@Override
	protected int getNextIndex() {
		DoubleVector probList = this.getProbabilityList();
		double rand = randomGenerator.nextDouble();
		double cumProb = 0.0;
		for( int i=0; i<probList.size(); i++) {
			cumProb += probList.get(i);
			if( rand <= cumProb ) {
				return i;
			}
		}
		return probList.size()-1;
	}
}
