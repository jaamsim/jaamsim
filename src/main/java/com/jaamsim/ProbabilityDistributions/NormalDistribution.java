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
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Normal Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 453.
 * Polar Method, Marsaglia and Bray (1964)
 */
public class NormalDistribution extends Distribution {

	@Keyword(description = "The mean of the normal distribution (ignoring the MinValue and "
	                     + "MaxValue keywords).",
	         exampleList = {"5.0", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput meanInput;

	@Keyword(description = "The standard deviation of the normal distribution (ignoring the "
	                     + "MinValue and MaxValue keywords).",
	         exampleList = {"2.0", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput standardDeviationInput;

	private final MRG1999a rng1 = new MRG1999a();
	private final MRG1999a rng2 = new MRG1999a();

	{
		meanInput = new SampleInput("Mean", KEY_INPUTS, 0.0d);
		meanInput.setUnitType(UserSpecifiedUnit.class);
		meanInput.setOutput(true);
		this.addInput(meanInput);

		standardDeviationInput = new SampleInput("StandardDeviation", KEY_INPUTS, 1.0d);
		standardDeviationInput.setUnitType(UserSpecifiedUnit.class);
		standardDeviationInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		standardDeviationInput.setOutput(true);
		this.addInput(standardDeviationInput);
	}

	public NormalDistribution() {}

	@Override
	public void earlyInit() {
		super.earlyInit();

		rng1.setSeedStream(getStreamNumber()    , getSubstreamNumber());
		rng2.setSeedStream(getStreamNumber() + 1, getSubstreamNumber());
	}

	@Override
	protected void setUnitType(Class<? extends Unit> specified) {
		super.setUnitType(specified);
		meanInput.setUnitType(specified);
		standardDeviationInput.setUnitType(specified);
	}

	@Override
	protected double getSample(double simTime) {
		double mean = meanInput.getNextSample(this, simTime);
		double sdev = standardDeviationInput.getNextSample(this, simTime);
		return getSample(mean, sdev, rng1, rng2);
	}

	@Override
	protected double getMean(double simTime) {
		double mean = meanInput.getNextSample(this, simTime);
		double sdev = standardDeviationInput.getNextSample(this, simTime);
		return getMean(mean, sdev);
	}

	@Override
	protected double getStandardDev(double simTime) {
		double mean = meanInput.getNextSample(this, simTime);
		double sdev = standardDeviationInput.getNextSample(this, simTime);
		return getStandardDev(mean, sdev);
	}

	@Override
	protected double getMin(double simTime) {
		return Double.NEGATIVE_INFINITY;
	}

	@Override
	protected double getMax(double simTime) {
		return Double.POSITIVE_INFINITY;
	}

	public static double getSample(double mean, double sdev, MRG1999a rng1, MRG1999a rng2) {

		// Loop until we have a random x-y coordinate in the unit circle
		double w, v1, v2, sample;
		do {
			v1 = 2.0 * rng1.nextUniform() - 1.0;
			v2 = 2.0 * rng2.nextUniform() - 1.0;
			w = ( v1 * v1 ) + ( v2 * v2 );
		} while( w > 1.0 || w == 0.0 );

		// Calculate the normalised random sample
		// (normally distributed with mode = 0 and standard deviation = 1)
		sample = v1 * Math.sqrt( -2.0 * Math.log( w ) / w );

		// Adjust for the desired mode and standard deviation
		return mean + sample*sdev;
	}

	public static double getMean(double mean, double sdev) {
		return mean;
	}

	public static double getStandardDev(double mean, double sdev) {
		return sdev;
	}
}
