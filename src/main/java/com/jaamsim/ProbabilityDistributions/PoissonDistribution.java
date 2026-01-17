/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2022-2024 JaamSim Software Inc.
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

import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.DimensionlessUnit;

/**
 * Exponential Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 5th Edition", page 470.
 */
public class PoissonDistribution extends Distribution {

	@Keyword(description = "The mean of the Poisson distribution.",
	         exampleList = {"5.0", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput meanInput;

	private final MRG1999a rng = new MRG1999a();

	{
		unitType.setDefaultValue(DimensionlessUnit.class);
		setUnitType(getUnitType());
		unitType.setHidden(true);

		minValueInput.setDefaultValue(0.0d);

		meanInput = new SampleInput("Mean", KEY_INPUTS, 1.0d);
		meanInput.setUnitType(DimensionlessUnit.class);
		meanInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(meanInput);
	}

	public PoissonDistribution() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected double getSample(double simTime) {
		double mean = meanInput.getNextSample(this, simTime);
		return getSample(mean, rng);
	}

	@Override
	protected double getMean(double simTime) {
		double mean = meanInput.getNextSample(this, simTime);
		return getMeanVal(mean);
	}

	@Override
	protected double getStandardDev(double simTime) {
		double mean = meanInput.getNextSample(this, simTime);
		return getStandardDevVal(mean);
	}

	@Override
	protected double getMin(double simTime) {
		return 0.0d;
	}

	@Override
	protected double getMax(double simTime) {
		return Double.POSITIVE_INFINITY;
	}

	public static int getSample(double mean, MRG1999a rng) {
		double a = Math.exp(-mean);
		double b = 1;
		int i = 0;
		while (true) {
			b *= rng.nextUniform();
			if (b < a)
				return i;
			i++;
		}
	}

	public static double getMeanVal(double mean) {
		return mean;
	}

	public static double getStandardDevVal(double mean) {
		return Math.sqrt(mean);
	}

}
