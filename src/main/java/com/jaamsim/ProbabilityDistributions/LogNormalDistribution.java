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
 * LogNormal Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 454.
 * Polar Method, Marsaglia and Bray (1964) is used to calculate the normal distribution
 */
public class LogNormalDistribution extends NewProbabilityDistribution {

	@Keyword(desc = "The mean of the normal distribution (not the mean of the lognormal).",
	         example = "LogNormalDist-1 NormalMean { 5.0 }")
	private final DoubleInput normalMeanInput;

	@Keyword(desc = "The standard deviation of the normal distribution (not the standard deviation of the lognormal).",
	         example = "LogNormalDist-1 NormalStandardDeviation { 2.0 }")
	private final DoubleInput normalStandardDeviationInput;

	{
		normalMeanInput = new DoubleInput("NormalMean", "Key Inputs", 0.0d);
		this.addInput(normalMeanInput, true);

		normalStandardDeviationInput = new DoubleInput("NormalStandardDeviation", "Key Inputs", 1.0d);
		normalStandardDeviationInput.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		this.addInput(normalStandardDeviationInput, true);
	}

	@Override
	protected double getNextNonZeroSample() {

		// Loop until we have a random x-y coordinate in the unit circle
		double w, v1, v2, sample;
		do {
			v1 = 2.0 * randomGenerator1.nextDouble() - 1.0;
			v2 = 2.0 * randomGenerator2.nextDouble() - 1.0;
			w = ( v1 * v1 ) + ( v2 * v2 );
		} while( w > 1.0 || w == 0.0 );

		// Calculate the normalised random sample
		// (normally distributed with mode = 0 and standard deviation = 1)
		sample = v1 * Math.sqrt( -2.0 * Math.log( w ) / w );

		// Adjust for the desired mode and standard deviation
		sample = normalMeanInput.getValue() + ( sample * normalStandardDeviationInput.getValue() );

		// Convert to lognormal
		return Math.exp( sample );
	}

	@Override
	protected double getMeanValue() {
		double sd = normalStandardDeviationInput.getValue();
		return Math.exp( normalMeanInput.getValue() + sd*sd/2.0 );
	}

	@Override
	protected double getStandardDeviation() {
		double sd = normalStandardDeviationInput.getValue();
		return this.getMeanValue() * Math.sqrt( Math.exp( sd*sd ) - 1.0 );
	}
}
