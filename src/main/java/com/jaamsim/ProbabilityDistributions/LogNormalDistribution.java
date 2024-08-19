/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2024 JaamSim Software Inc.
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
 * LogNormal Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 454.
 * Polar Method, Marsaglia and Bray (1964) is used to calculate the normal distribution
 */
public class LogNormalDistribution extends Distribution {

	@Keyword(description = "The mean of the dimensionless normal distribution (not the mean of the lognormal).",
	         exampleList = {"5.0", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput normalMeanInput;

	@Keyword(description = "The standard deviation of the dimensionless normal distribution (not the standard deviation of the lognormal).",
	         exampleList = {"2.0", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput normalStandardDeviationInput;

	private final MRG1999a rng1 = new MRG1999a();
	private final MRG1999a rng2 = new MRG1999a();

	{
		minValueInput.setDefaultValue(0.0d);

		locationInput.setHidden(false);
		scaleInput.setHidden(false);

		normalMeanInput = new SampleInput("NormalMean", KEY_INPUTS, 0.0d);
		normalMeanInput.setUnitType(DimensionlessUnit.class);
		normalMeanInput.setOutput(true);
		this.addInput(normalMeanInput);

		normalStandardDeviationInput = new SampleInput("NormalStandardDeviation", KEY_INPUTS, 1.0d);
		normalStandardDeviationInput.setUnitType(DimensionlessUnit.class);
		normalStandardDeviationInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		normalStandardDeviationInput.setOutput(true);
		this.addInput(normalStandardDeviationInput);
	}

	public LogNormalDistribution() {}

	@Override
	public void earlyInit() {
		super.earlyInit();

		rng1.setSeedStream(getStreamNumber()    , getSubstreamNumber());
		rng2.setSeedStream(getStreamNumber() + 1, getSubstreamNumber());
	}

	@Override
	protected double getSample(double simTime) {
		double location = getLocationInput(simTime);
		double scale = getScaleInput(simTime);
		double mean = normalMeanInput.getNextSample(this, simTime);
		double sd = normalStandardDeviationInput.getNextSample(this, simTime);
		return location + scale * getSample(mean, sd, rng1, rng2);
	}

	@Override
	protected double getMean(double simTime) {
		double location = getLocationInput(simTime);
		double scale = getScaleInput(simTime);
		double mean = normalMeanInput.getNextSample(this, simTime);
		double sd = normalStandardDeviationInput.getNextSample(this, simTime);
		return location + scale * getMean(mean, sd);
	}

	@Override
	protected double getStandardDev(double simTime) {
		double scale = getScaleInput(simTime);
		double mean = normalMeanInput.getNextSample(this, simTime);
		double sd = normalStandardDeviationInput.getNextSample(this, simTime);
		return scale * getStandardDev(mean, sd);
	}

	@Override
	protected double getMin(double simTime) {
		double location = getLocationInput(simTime);
		return location;
	}

	@Override
	protected double getMax(double simTime) {
		return Double.POSITIVE_INFINITY;
	}

	public static double getSample(double normalMean, double normalSD, MRG1999a rng1, MRG1999a rng2) {
		double sample = NormalDistribution.getSample(normalMean, normalSD, rng1, rng2);
		return Math.exp(sample);
	}

	public static double getMean(double normalMean, double normalSD) {
		return Math.exp(normalMean + normalSD*normalSD/2.0);
	}

	public static double getStandardDev(double normalMean, double normalSD) {
		return getMean(normalMean, normalSD) * Math.sqrt( Math.exp(normalSD*normalSD) - 1.0 );
	}

}
