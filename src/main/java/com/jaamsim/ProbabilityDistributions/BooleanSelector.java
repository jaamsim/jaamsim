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
package com.jaamsim.ProbabilityDistributions;

import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.DimensionlessUnit;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class BooleanSelector extends DisplayEntity {
	@Keyword(description = "Seed for the random number generator.  Must be an integer > 0.",
	         example = "RandomSelector1 RandomSeed { 547 }")
	private IntegerInput randomSeedInput;

	@Keyword(description = "The probability of the Selector returning true.",
	         example = "RandomSelector1 TrueProbability { 0.5 }")
	private ValueInput trueProbInput;

	private final MRG1999a rng = new MRG1999a();

	{
		randomSeedInput = new IntegerInput("RandomSeed", "Key Inputs", 1);
		randomSeedInput.setValidRange(1, Integer.MAX_VALUE);
		this.addInput(randomSeedInput);

		trueProbInput = new ValueInput("TrueProbability", "Key Inputs", 1.0d);
		trueProbInput.setUnitType(DimensionlessUnit.class);
		trueProbInput.setValidRange(0.0d, 1.0d);
		this.addInput(trueProbInput);
	}

	public BooleanSelector() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(randomSeedInput.getValue(), Distribution.getSubstreamNumber());
	}

	public boolean getNextValue() {
		double samp = rng.nextUniform();
		if (samp < trueProbInput.getValue())
			return true;
		else
			return false;
	}
}
