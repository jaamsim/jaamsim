/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2022 JaamSim Software Inc.
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

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.DimensionlessUnit;

/**
 * Binomial Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 5th Edition", page 469.
 */
public class BinomialDistribution extends Distribution {

	@Keyword(description = "Number of independent trials to perform.",
	         exampleList = {"5.0", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput numberOfTrials;

	@Keyword(description = "Probability of success for each trial.",
	         exampleList = {"0.5", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput probability;

	private final MRG1999a rng = new MRG1999a();

	{
		minValueInput.setDefaultValue(new SampleConstant(0.0d));

		numberOfTrials = new SampleInput("NumberOfTrials", KEY_INPUTS, new SampleConstant(1.0d));
		numberOfTrials.setUnitType(DimensionlessUnit.class);
		numberOfTrials.setValidRange(1.0d, Double.POSITIVE_INFINITY);
		this.addInput(numberOfTrials);

		probability = new SampleInput("Probability", KEY_INPUTS, new SampleConstant(1.0d));
		probability.setUnitType(DimensionlessUnit.class);
		probability.setValidRange(0.0d, 1.0d);
		this.addInput(probability);
	}

	public BinomialDistribution() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected double getSample(double simTime) {
		int n = (int) numberOfTrials.getNextSample(simTime);
		double p = probability.getNextSample(simTime);
		return getSample(n, p, rng);
	}

	@Override
	protected double getMean(double simTime) {
		int n = (int) numberOfTrials.getNextSample(simTime);
		double p = probability.getNextSample(simTime);
		return getMean(n, p);
	}

	@Override
	protected double getStandardDev(double simTime) {
		int n = (int) numberOfTrials.getNextSample(simTime);
		double p = probability.getNextSample(simTime);
		return getStandardDev(n, p);
	}

	public static int getSample(int n, double p, MRG1999a rng) {
		int ret = 0;
		for (int i = 0; i < n; i++) {
			if (rng.nextUniform() <= p) {
				ret++;
			}
		}
		return ret;
	}

	public static double getMean(int n, double p) {
		return n * p;
	}

	public static double getStandardDev(int n, double p) {
		return Math.sqrt(n * p * (1.0d - p));
	}

}
