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
import com.jaamsim.math.Gamma;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.DimensionlessUnit;

/**
 * Weibull Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 452.
 */
public class WeibullDistribution extends Distribution {

	@Keyword(description = "The shape parameter for the Weibull distribution.  A decimal value > 0.0.",
	         exampleList = {"1.0", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput shapeInput;

	private final MRG1999a rng = new MRG1999a();

	{
		minValueInput.setDefaultValue(0.0d);

		locationInput.setHidden(false);
		scaleInput.setHidden(false);

		shapeInput = new SampleInput("Shape", KEY_INPUTS, 1.0d);
		shapeInput.setValidRange(1.0e-10d, Double.POSITIVE_INFINITY);
		shapeInput.setUnitType(DimensionlessUnit.class);
		shapeInput.setOutput(true);
		this.addInput(shapeInput);
	}

	public WeibullDistribution() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected double getSample(double simTime) {
		double location = getLocationInput(simTime);
		double scale = getScaleInput(simTime);
		double shape = shapeInput.getNextSample(this, simTime);
		return location + getSample(scale, shape, rng);
	}

	@Override
	protected double getMean(double simTime) {
		double location = getLocationInput(simTime);
		double scale = getScaleInput(simTime);
		double shape = shapeInput.getNextSample(this, simTime);
		return location + getMean(scale, shape);
	}

	@Override
	protected double getStandardDev(double simTime) {
		double scale = getScaleInput(simTime);
		double shape = shapeInput.getNextSample(this, simTime);
		return getStandardDev(scale, shape);
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

	public static double getSample(double scale, double shape, MRG1999a rng) {
		return scale * Math.pow( - Math.log(rng.nextUniform()), 1.0/shape );
	}

	public static double getMean(double scale, double shape) {
		return scale/shape * Gamma.gamma(1.0/shape);
	}

	public static double getStandardDev(double scale, double shape) {
		return scale/shape * Math.sqrt( 2.0*shape*Gamma.gamma(2.0/shape) - Math.pow(Gamma.gamma(1.0/shape), 2.0) );
	}

}
