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

import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Keyword;

/**
 * Gamma Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", pages 449-452.
 * Ahrens and Dieter (1974) for shape parameter < 1
 * Cheng (1977) for shape parameter >= 1
 */
public class GammaDistribution extends NewProbabilityDistribution {

	@Keyword(desc = "The mean of the Gamma distribution.",
	         example = "GammaDist-1 Mean { 5.0 }")
	private final DoubleInput meanInput;

	@Keyword(desc = "The shape parameter for the Gamma distribution.  A decimal value > 0.0.",
	         example = "GammaDist-1 Shape { 2.0 }")
	private final DoubleInput shapeInput;

	{
		meanInput = new DoubleInput("Mean", "Key Inputs", 1.0d);
		meanInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		this.addInput(meanInput, true);

		shapeInput = new DoubleInput("Shape", "Key Inputs", 1.0);
		shapeInput.setValidRange( 1.0e-10d, Integer.MAX_VALUE);
		this.addInput(shapeInput, true);
	}

	@Override
	protected double getNextNonZeroSample() {
		double u2, b, sample;

		// Case 1 - Shape parameter < 1
		if( shapeInput.getValue() < 1.0 ) {
			double threshold;
			b = 1.0 + ( shapeInput.getValue() / Math.E );
			do {
				double p = b * randomGenerator2.nextDouble();
				u2 = randomGenerator1.nextDouble();

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
				u1 = randomGenerator1.nextDouble();
				u2 = randomGenerator2.nextDouble();
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
