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

	@Keyword(description = "The list of probabilities on which to base the selection.  Must sum to 1.0.",
	         example = "EntitlementSelector1 ProportionList { 0.3  0.7 }")
	private final ValueListInput proportionList;

	private int lastSample;  // the index that was selected most recently
	private int totalCount;  // the total number of samples that have been selected
	private int[] sampleCount;  // number of times each index has been selected
	private double[] sampleDifference;  // (actual number of samples) - (expected number)

	{
		proportionList = new ValueListInput("ProportionList", "Key Inputs", null);
		proportionList.setUnitType(DimensionlessUnit.class);
		proportionList.setRequired(true);
		this.addInput(proportionList);
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
	        unitType = UserSpecifiedUnit.class)
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

	@Override
	public double getMinValue() {
		return 1;
	}

	@Override
	public double getMaxValue() {
		return proportionList.getValue().size();
	}

	@Output( name="NumberOfSamples",
			 description="The number of times the distribution has been sampled.")
	public int getNumberOfSamples(double simTime) {
		return totalCount;
	}

	@Output( name="SampleCount",
			 description="The number samples for each entity.")
	public DoubleVector getSampleCount(double simTime) {
		DoubleVector ret = new DoubleVector(sampleCount.length);
		for (int i=0; i<sampleCount.length; i++) {
			ret.add(sampleCount[i]);
		}
		return ret;
	}

	@Output( name="SampleDifference",
			 description="The difference between the actual number samples for each entity and the expected number.")
	public DoubleVector getSampleDifference(double simTime) {
		DoubleVector ret = new DoubleVector(sampleDifference.length);
		for (int i=0; i<sampleDifference.length; i++) {
			ret.add(sampleDifference[i]);
		}
		return ret;
	}

}
