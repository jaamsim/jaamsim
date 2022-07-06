/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.BasicObjects;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * EntitlementSelector selects an index to return based on the difference
 * between the expected and actual numbers of samples for each index. The
 * object with the largest difference (expected - actual) is selected.
 * @author Harry King
 *
 */
public class EntitlementSelector extends DisplayEntity implements SampleProvider {

	@Keyword(description = "A list of N numbers equal to the relative proportion for each of the "
	                     + "N indices. Must sum to 1.0.",
	         exampleList = {"0.3  0.7"})
	private final ValueListInput proportionList;

	private int lastSample;  // the index that was selected most recently
	private int totalCount;  // the total number of samples that have been selected
	private int[] sampleCount;  // number of times each index has been selected
	private double[] sampleDifference;  // (actual number of samples) - (expected number)

	{
		proportionList = new ValueListInput("ProportionList", KEY_INPUTS, null);
		proportionList.setUnitType(DimensionlessUnit.class);
		proportionList.setRequired(true);
		this.addInput(proportionList);
	}

	public EntitlementSelector() {
		sampleCount = new int[0];
		sampleDifference = new double[0];
	}

	@Override
	public void validate() {
		super.validate();

		// The entries in the ProportionList must sum to 1.0
		if (Math.abs(proportionList.getValue().sum() - 1.0) > 1.0e-10) {
			throw new InputErrorException("The entries in the ProportionList must sum to 1.0");
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		lastSample = -1;
		totalCount = 0;
		sampleCount = new int[proportionList.getValue().size()];
		sampleDifference = new double[proportionList.getValue().size()];
	}

	/**
	 * Returns the next sample.
	 */
	@Output(name = "Value",
	 description = "The last sampled index (from 1 to N).",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 0)
	@Override
	public final double getNextSample(double simTime) {
		// If we are not in a model context, do not perturb the distribution by sampling,
		// instead simply return the last sampled value
		if (!EventManager.hasCurrent()) {
			return lastSample;
		}

		// Make the next selection
		DoubleVector probList = proportionList.getValue();
		int index = 0;
		double maxDiff = Double.NEGATIVE_INFINITY;
		totalCount++;
		for (int i=0; i<probList.size(); i++) {
			double diff = totalCount * probList.get(i) - sampleCount[i];
			if (diff > maxDiff) {
				maxDiff = diff;
				index = i;
			}
		}
		lastSample = index + 1;

		// Collect statistics on the sampled values
		sampleCount[index]++;
		for(int i=0; i<sampleCount.length; i++) {
			sampleDifference[i] = sampleCount[i] - totalCount*proportionList.getValue().get(i);
		}

		return lastSample;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return DimensionlessUnit.class;
	}

	@Override
	public double getMeanValue(double simTime) {
		return 0;
	}

	@Output(name = "NumberOfSamples",
	 description = "The number of times the distribution has been sampled.",
	    sequence = 1)
	public int getNumberOfSamples(double simTime) {
		return totalCount;
	}

	@Output(name = "SampleCount",
	 description = "The number samples for each entity.",
	    sequence = 2)
	public DoubleVector getSampleCount(double simTime) {
		DoubleVector ret = new DoubleVector(sampleCount.length);
		for (int i=0; i<sampleCount.length; i++) {
			ret.add(sampleCount[i]);
		}
		return ret;
	}

	@Output(name = "SampleDifference",
	 description = "The difference between the actual number samples for each entity and the "
	             + "expected number.",
	    sequence = 3)
	public DoubleVector getSampleDifference(double simTime) {
		DoubleVector ret = new DoubleVector(sampleDifference.length);
		for (int i=0; i<sampleDifference.length; i++) {
			ret.add(sampleDifference[i]);
		}
		return ret;
	}

}
