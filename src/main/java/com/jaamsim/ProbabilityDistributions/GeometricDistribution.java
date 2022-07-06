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

import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.MathUtils;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.DimensionlessUnit;

/**
 * Geometric Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 5th Edition", page 469.
 */
public class GeometricDistribution extends Distribution {

	@Keyword(description = "Probability of success for each trial.",
	         exampleList = {"0.5", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput probability;

	private final MRG1999a rng = new MRG1999a();

	{
		unitType.setDefaultValue(DimensionlessUnit.class);
		setUnitType(getUnitType());
		unitType.setHidden(true);

		minValueInput.setDefaultValue(0.0d);

		probability = new SampleInput("Probability", KEY_INPUTS, 1.0d);
		probability.setUnitType(DimensionlessUnit.class);
		probability.setValidRange(0.0d, 1.0d);
		this.addInput(probability);
	}

	public GeometricDistribution() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected double getSample(double simTime) {
		double p = probability.getNextSample(simTime);
		return getSample(p, rng);
	}

	@Override
	protected double getMean(double simTime) {
		double p = probability.getNextSample(simTime);
		return getMeanVal(p);
	}

	@Override
	protected double getStandardDev(double simTime) {
		double p = probability.getNextSample(simTime);
		return getStandardDevVal(p);
	}

	@Override
	protected double getMin(double simTime) {
		return 0.0d;
	}

	@Override
	protected double getMax(double simTime) {
		return Double.POSITIVE_INFINITY;
	}

	public static int getSample(double p, MRG1999a rng) {
		if (MathUtils.near(p, 1.0d))
			return 0;
		double rand = rng.nextUniform();
		return (int) (Math.log(rand) / Math.log(1 - p));
	}

	public static double getMeanVal(double p) {
		return (1.0d - p) / p;
	}

	public static double getStandardDevVal(double p) {
		return Math.sqrt(1.0d - p) / p;
	}

}
