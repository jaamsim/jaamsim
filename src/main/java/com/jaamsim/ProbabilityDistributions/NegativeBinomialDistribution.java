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
 * Negative Binomial Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 5th Edition", page 469.
 */
public class NegativeBinomialDistribution extends Distribution {

	@Keyword(description = "Required number of successful trials.",
	         exampleList = {"5.0", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput successfulTrials;

	@Keyword(description = "Probability of success for each trial.",
	         exampleList = {"0.5", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput probability;

	private final MRG1999a rng = new MRG1999a();

	{
		unitType.setDefaultValue(DimensionlessUnit.class);
		setUnitType(getUnitType());
		unitType.setHidden(true);

		minValueInput.setDefaultValue(new SampleConstant(0.0d));

		successfulTrials = new SampleInput("SuccessfulTrials", KEY_INPUTS, new SampleConstant(1.0d));
		successfulTrials.setUnitType(DimensionlessUnit.class);
		successfulTrials.setValidRange(1.0d, Double.POSITIVE_INFINITY);
		this.addInput(successfulTrials);

		probability = new SampleInput("Probability", KEY_INPUTS, new SampleConstant(1.0d));
		probability.setUnitType(DimensionlessUnit.class);
		probability.setValidRange(0.0d, 1.0d);
		this.addInput(probability);
	}

	public NegativeBinomialDistribution() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected double getSample(double simTime) {
		int s = (int) successfulTrials.getNextSample(simTime);
		double p = probability.getNextSample(simTime);
		return getSample(s, p, rng);
	}

	@Override
	protected double getMean(double simTime) {
		int s = (int) successfulTrials.getNextSample(simTime);
		double p = probability.getNextSample(simTime);
		return getMean(s, p);
	}

	@Override
	protected double getStandardDev(double simTime) {
		int s = (int) successfulTrials.getNextSample(simTime);
		double p = probability.getNextSample(simTime);
		return getStandardDev(s, p);
	}

	public static int getSample(int s, double p, MRG1999a rng) {
		int ret = 0;
		for (int i = 0; i < s; i++) {
			ret += GeometricDistribution.getSample(p, rng);
		}
		return ret;
	}

	public static double getMean(int s, double p) {
		return s * GeometricDistribution.getMeanVal(p);
	}

	public static double getStandardDev(int s, double p) {
		return Math.sqrt(s) * GeometricDistribution.getStandardDevVal(p);
	}

}
