/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
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
import com.jaamsim.math.Gamma;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Weibull Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 452.
 */
public class WeibullDistribution extends Distribution {

	@Keyword(description = "The scale parameter for the Weibull distribution.",
	         exampleList = {"3.0 h", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput scaleInput;

	@Keyword(description = "The shape parameter for the Weibull distribution.  A decimal value > 0.0.",
	         exampleList = {"1.0", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput shapeInput;

	@Keyword(description = "The location parameter for the Weibull distribution.",
	         exampleList = {"5.0 h", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput locationInput;

	private final MRG1999a rng = new MRG1999a();

	{
		minValueInput.setDefaultValue(new SampleConstant(0.0d));

		scaleInput = new SampleInput("Scale", KEY_INPUTS, new SampleConstant(1.0d));
		scaleInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		scaleInput.setUnitType(UserSpecifiedUnit.class);
		this.addInput(scaleInput);

		locationInput = new SampleInput("Location", KEY_INPUTS, new SampleConstant(0.0d));
		locationInput.setUnitType(UserSpecifiedUnit.class);
		this.addInput(locationInput);

		shapeInput = new SampleInput("Shape", KEY_INPUTS, new SampleConstant(1.0d));
		shapeInput.setValidRange(1.0e-10d, Double.POSITIVE_INFINITY);
		shapeInput.setUnitType(DimensionlessUnit.class);
		this.addInput(shapeInput);
	}

	public WeibullDistribution() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected void setUnitType(Class<? extends Unit> ut) {
		super.setUnitType(ut);
		scaleInput.setUnitType(ut);
		locationInput.setUnitType(ut);
	}

	@Override
	protected double getSample(double simTime) {

		double scale = scaleInput.getValue().getNextSample(simTime);
		double shape = shapeInput.getValue().getNextSample(simTime);
		double loc = locationInput.getValue().getNextSample(simTime);

		// Inverse transform method
		return  scale * Math.pow( - Math.log(rng.nextUniform()), 1.0/shape ) + loc;
	}

	@Override
	protected double getMean(double simTime) {
		double scale = scaleInput.getValue().getNextSample(simTime);
		double shape = shapeInput.getValue().getNextSample(simTime);
		double loc = locationInput.getValue().getNextSample(simTime);
		return scale/shape * Gamma.gamma(1.0/shape) + loc;
	}

	@Override
	protected double getStandardDev(double simTime) {
		double scale = scaleInput.getValue().getNextSample(simTime);
		double shape = shapeInput.getValue().getNextSample(simTime);
		return scale/shape * Math.sqrt( 2.0*shape*Gamma.gamma(2.0/shape) - Math.pow(Gamma.gamma(1.0/shape), 2.0) );
	}

}
