/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.ProbabilityDistributions;

import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
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
	         example = "GammaDist1 Mean { 5.0 }")
	private final ValueInput meanInput;

	@Keyword(description = "The shape parameter for the Gamma distribution.  A decimal value > 0.0.",
	         example = "GammaDist1 Shape { 2.0 }")
	private final ValueInput shapeInput;

	private final MRG1999a rng1 = new MRG1999a();
	private final MRG1999a rng2 = new MRG1999a();

	{
		minValueInput.setDefaultValue(0.0);

		meanInput = new ValueInput("Mean", "Key Inputs", 1.0d);
		meanInput.setUnitType(UserSpecifiedUnit.class);
		meanInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(meanInput);

		shapeInput = new ValueInput("Shape", "Key Inputs", 1.0);
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
	protected double getNextSample() {
		double u2, b, sample;

		// Case 1 - Shape parameter < 1
		if( shapeInput.getValue() < 1.0 ) {
			double threshold;
			b = 1.0 + ( shapeInput.getValue() / Math.E );
			do {
				double p = b * rng2.nextUniform();
				u2 = rng1.nextUniform();

				if( p <= 1.0 ) {
					sample = Math.pow( p, 1.0/shapeInput.getValue() );
					threshold = Math.exp( - sample );
				}

				else {
					sample = - Math.log( ( b - p ) / shapeInput.getValue() );
					threshold = Math.pow( sample, shapeInput.getValue() - 1.0 );
				}
			} while ( u2 > threshold );
		}

		// Case 2 - Shape parameter >= 1
		else {
			double u1, w, z;
			double a = 1.0 / Math.sqrt( ( 2.0 * shapeInput.getValue() ) - 1.0 );
			b = shapeInput.getValue() - Math.log( 4.0 );
			double q = shapeInput.getValue() + ( 1.0 / a );
			double d = 1.0 + Math.log( 4.5 );
			do {
				u1 = rng1.nextUniform();
				u2 = rng2.nextUniform();
				double v = a * Math.log( u1 / ( 1.0 - u1 ) );
				sample = shapeInput.getValue() * Math.exp( v );
				z = u1 * u1 * u2;
				w = b + q*v - sample;
			} while( ( w + d - 4.5*z < 0.0 ) && ( w < Math.log(z) ) );
		}

		// Scale the sample by the desired mean value
		return sample * meanInput.getValue() / shapeInput.getValue();
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
