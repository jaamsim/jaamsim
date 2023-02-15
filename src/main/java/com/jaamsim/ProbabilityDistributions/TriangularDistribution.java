/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2022 JaamSim Software Inc.
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
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Triangular Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 457.
 */
public class TriangularDistribution extends Distribution {

	@Keyword(description = "The mode of the triangular distribution, i.e. the value with the highest probability.",
	         exampleList = {"5.0", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput modeInput;

	private final MRG1999a rng = new MRG1999a();

	{
		minValueInput.setDefaultValue(0.0d);
		maxValueInput.setDefaultValue(2.0d);

		modeInput = new SampleInput("Mode", KEY_INPUTS, 1.0d);
		modeInput.setUnitType(UserSpecifiedUnit.class);
		this.addInput(modeInput);
	}

	public TriangularDistribution() {}

	@Override
	public void validate() {
		super.validate();

		// The mode must be between the minimum and maximum values
		if (getMinValueInput(0.0d) > modeInput.getNextSample(this, 0.0d)) {
			throw new InputErrorException("The input for Mode must be >= than that for MinValue.");
		}
		if (getMaxValueInput(0.0d) < modeInput.getNextSample(this, 0.0d)) {
			throw new InputErrorException("The input for Mode must be <= than that for MaxValue.");
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected void setUnitType(Class<? extends Unit> specified) {
		super.setUnitType(specified);
		modeInput.setUnitType(specified);
	}

	@Override
	protected double getSample(double simTime) {
		double minVal = minValueInput.getNextSample(this, simTime);
		double maxVal = maxValueInput.getNextSample(this, simTime);
		double mode = modeInput.getNextSample(this, simTime);
		return getSample(minVal, mode, maxVal, rng);
	}

	@Override
	protected double getMean(double simTime) {
		double minVal = minValueInput.getNextSample(this, simTime);
		double maxVal = maxValueInput.getNextSample(this, simTime);
		double mode = modeInput.getNextSample(this, simTime);
		return getMean(minVal, mode, maxVal);
	}

	@Override
	protected double getStandardDev(double simTime) {
		double a = minValueInput.getNextSample(this, simTime);
		double b = maxValueInput.getNextSample(this, simTime);
		double m = modeInput.getNextSample(this, simTime);
		return  getStandardDev(a, m, b);
	}

	@Override
	protected double getMin(double simTime) {
		return getMinValueInput(simTime);
	}

	@Override
	protected double getMax(double simTime) {
		return getMaxValueInput(simTime);
	}

	public static double getSample(double minVal, double mode, double maxVal, MRG1999a rng) {

		// Select the random value
		double rand = rng.nextUniform();

		// Calculate the normalised mode
		double m = (mode - minVal)/(maxVal - minVal);

		// Use the inverse transform method to calculate the normalised random sample
		// (triangular distribution with min = 0, max = 1, and mode = m)
		double sample;
		if (rand <= m) {
			sample = Math.sqrt( m * rand );
		}
		else {
			sample = 1.0 - Math.sqrt( ( 1.0 - m )*( 1.0 - rand ) );
		}

		// Adjust for the desired min and max values
		return  minVal + sample*(maxVal - minVal);
	}

	public static double getMean(double minVal, double mode, double maxVal) {
		return (minVal + mode + maxVal)/3.0;
	}

	public static double getStandardDev(double minVal, double mode, double maxVal) {
		return  Math.sqrt( ( minVal*minVal + maxVal*maxVal + mode*mode - minVal*maxVal - minVal*mode - maxVal*mode ) / 18.0 );
	}

}
