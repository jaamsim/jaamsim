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

import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Exponential Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 448.
 */
public class ExponentialDistribution extends Distribution {

	@Keyword(description = "The mean of the exponential distribution.",
	         exampleList = {"5.0"})
	private final ValueInput meanInput;

	private final MRG1999a rng = new MRG1999a();

	{
		minValueInput.setDefaultValue(0.0);

		meanInput = new ValueInput("Mean", "Key Inputs", 1.0d);
		meanInput.setUnitType(UserSpecifiedUnit.class);
		meanInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(meanInput);
	}

	public ExponentialDistribution() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected void setUnitType(Class<? extends Unit> specified) {
		super.setUnitType(specified);
		meanInput.setUnitType(specified);
	}

	@Override
	protected double getSample(double simTime) {

		// Inverse transform method
		return (-meanInput.getValue() * Math.log(rng.nextUniform()));
	}

	@Override
	protected double getMean(double simTime) {
		return meanInput.getValue();
	}

	@Override
	protected double getStandardDev(double simTime) {
		return meanInput.getValue();
	}
}
