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
import com.jaamsim.basicsim.Entity;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.ui.EditBox;
import com.jaamsim.units.DimensionlessUnit;

public class BooleanSelector extends DisplayEntity {
	@Keyword(description = "Seed for the random number generator.  Must be an integer >= 0.",
	         exampleList = {"547"})
	private IntegerInput randomSeedInput;

	@Keyword(description = "The probability of the Selector returning true.",
	         exampleList = {"0.5"})
	private ValueInput trueProbInput;

	private final MRG1999a rng = new MRG1999a();
	private boolean lastValue;

	{
		randomSeedInput = new IntegerInput("RandomSeed", "Key Inputs", -1);
		randomSeedInput.setValidRange(0, Integer.MAX_VALUE);
		randomSeedInput.setRequired(true);
		randomSeedInput.setDefaultText(EditBox.NONE);
		this.addInput(randomSeedInput);

		trueProbInput = new ValueInput("TrueProbability", "Key Inputs", 1.0d);
		trueProbInput.setUnitType(DimensionlessUnit.class);
		trueProbInput.setValidRange(0.0d, 1.0d);
		this.addInput(trueProbInput);
	}

	public BooleanSelector() {}

	@Override
	public void setInputsForDragAndDrop() {
		super.setInputsForDragAndDrop();

		// Find the largest seed used so far
		int seed = 0;
		for (Distribution dist : Entity.getClonesOfIterator(Distribution.class)) {
			seed = Math.max(seed, dist.getStreamNumber());
		}
		for (BooleanSelector bs : Entity.getClonesOfIterator(BooleanSelector.class)) {
			seed = Math.max(seed, bs.getStreamNumber());
		}

		// Set the random number seed next unused value
		InputAgent.applyArgs(this, "RandomSeed", String.format("%s", seed+1));
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), Distribution.getSubstreamNumber());
		lastValue = false;
	}

	protected int getStreamNumber() {
		return randomSeedInput.getValue();
	}

	public boolean getNextValue() {
		double samp = rng.nextUniform();
		lastValue = samp < trueProbInput.getValue();
		return lastValue;
	}

	@Output(name = "Value",
	 description = "The last value sampled from the distribution. When used in an "
	             + "expression, this output returns a new sample every time the expression "
	             + "is evaluated.")
	public boolean getNextValue(double simTime) {

		// If we are not in a model context, do not perturb the distribution by sampling,
		// instead simply return the last sampled value
		if (!EventManager.hasCurrent()) {
			return lastValue;
		}

		// Select the next sample
		return this.getNextValue();
	}
}
