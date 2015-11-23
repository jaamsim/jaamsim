/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Erlang Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 449.
 */
public class ErlangDistribution extends Distribution {

	@Keyword(description = "The scale parameter for the Erlang distribution.",
	         exampleList = {"5.0"})
	private final ValueInput meanInput;

	@Keyword(description = "The shape parameter for the Erlang distribution.  An integer value >= 1.  " +
			"Shape = 1 gives the Exponential distribution.  " +
			"For Shape > 10 it is better to use the Gamma distribution.",
	         exampleList = {"2"})
	private final IntegerInput shapeInput;

	private final MRG1999a rng = new MRG1999a();

	{
		minValueInput.setDefaultValue(0.0);

		meanInput = new ValueInput("Mean", "Key Inputs", 1.0d);
		meanInput.setUnitType(UserSpecifiedUnit.class);
		meanInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(meanInput);

		shapeInput = new IntegerInput("Shape", "Key Inputs", 1);
		shapeInput.setValidRange( 1, Integer.MAX_VALUE);
		this.addInput(shapeInput);
	}

	public ErlangDistribution() {}

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
	protected double getNextSample() {

		// Calculate the product of k random values
		double u = 1.0;
		int k = shapeInput.getValue();
		for( int i=0; i<k; i++) {
			u *= rng.nextUniform();
		}

		// Inverse transform method
		return (- meanInput.getValue() / shapeInput.getValue() * Math.log( u ));
	}

	@Override
	protected double getMeanValue() {
		return meanInput.getValue();
	}

	@Override
	protected double getStandardDeviation() {
		return meanInput.getValue() / Math.sqrt( shapeInput.getValue() );
	}
}
