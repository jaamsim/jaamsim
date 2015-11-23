/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.ProbabilityDistributions;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.DimensionlessUnit;

public class BooleanSelector extends DisplayEntity {
	@Keyword(description = "Seed for the random number generator.  Must be an integer > 0.",
	         exampleList = {"547"})
	private IntegerInput randomSeedInput;

	@Keyword(description = "The probability of the Selector returning true.",
	         exampleList = {"0.5"})
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
