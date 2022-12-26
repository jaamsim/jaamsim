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
import com.jaamsim.input.Keyword;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Gamma Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", pages 449-452.
 * Ahrens and Dieter (1974) for shape parameter < 1
 * Cheng (1977) for shape parameter >= 1
 */
public class GammaDistribution extends Distribution {

	@Keyword(description = "The mean of the Gamma distribution.",
	         exampleList = {"5.0", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput meanInput;

	@Keyword(description = "The shape parameter for the Gamma distribution.  A decimal value > 0.0.",
	         exampleList = {"2.0", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput shapeInput;

	private final MRG1999a rng1 = new MRG1999a();
	private final MRG1999a rng2 = new MRG1999a();

	{
		minValueInput.setDefaultValue(0.0d);

		meanInput = new SampleInput("Mean", KEY_INPUTS, 1.0d);
		meanInput.setUnitType(UserSpecifiedUnit.class);
		meanInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(meanInput);

		shapeInput = new SampleInput("Shape", KEY_INPUTS, 1.0d);
		shapeInput.setUnitType(DimensionlessUnit.class);
		shapeInput.setValidRange( 1.0e-10d, Integer.MAX_VALUE);
		this.addInput(shapeInput);
	}

	public GammaDistribution() {}

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
	}

	@Override
	protected double getSample(double simTime) {
		double mean = meanInput.getNextSample(this, simTime);
		double shape = shapeInput.getNextSample(this, simTime);
		return getSample(mean, shape, rng1, rng2);
	}

	@Override
	protected double getMean(double simTime) {
		double mean = meanInput.getNextSample(this, simTime);
		double shape = shapeInput.getNextSample(this, simTime);
		return getMean(mean, shape);
	}

	@Override
	protected double getStandardDev(double simTime) {
		double mean = meanInput.getNextSample(this, simTime);
		double shape = shapeInput.getNextSample(this, simTime);
		return getStandardDev(mean, shape);
	}

	@Override
	protected double getMin(double simTime) {
		return 0.0d;
	}

	@Override
	protected double getMax(double simTime) {
		return Double.POSITIVE_INFINITY;
	}

	public static double getSample(double mean, double shape, MRG1999a rng1, MRG1999a rng2) {
		double u2, b, sample;

		// Case 1 - Shape parameter < 1
		if( shape < 1.0 ) {
			double threshold;
			b = 1.0 + ( shape / Math.E );
			do {
				double p = b * rng2.nextUniform();
				u2 = rng1.nextUniform();

				if( p <= 1.0 ) {
					sample = Math.pow( p, 1.0/shape );
					threshold = Math.exp( - sample );
				}

				else {
					sample = - Math.log( ( b - p ) / shape );
					threshold = Math.pow( sample, shape - 1.0 );
				}
			} while ( u2 > threshold );
		}

		// Case 2 - Shape parameter >= 1
		else {
			double u1, w, z;
			double a = 1.0 / Math.sqrt( ( 2.0 * shape ) - 1.0 );
			b = shape - Math.log( 4.0 );
			double q = shape + ( 1.0 / a );
			double d = 1.0 + Math.log( 4.5 );
			do {
				u1 = rng1.nextUniform();
				u2 = rng2.nextUniform();
				double v = a * Math.log( u1 / ( 1.0 - u1 ) );
				sample = shape * Math.exp( v );
				z = u1 * u1 * u2;
				w = b + q*v - sample;
			} while( ( w + d - 4.5*z < 0.0 ) && ( w < Math.log(z) ) );
		}

		// Scale the sample by the desired mean value
		return sample * mean / shape;
	}

	public static double getMean(double mean, double shape) {
		return mean;
	}

	public static double getStandardDev(double mean, double shape) {
		return mean / Math.sqrt(shape);
	}

}
