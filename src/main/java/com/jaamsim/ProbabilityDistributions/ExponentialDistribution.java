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
 * Exponential Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 448.
 */
public class ExponentialDistribution extends Distribution {

	@Keyword(description = "The mean of the exponential distribution.",
	         example = "ExponentialDist-1 Mean { 5.0 }")
	private final DoubleInput meanInput;

	{
		meanInput = new DoubleInput("Mean", "Key Inputs", 1.0d);
		meanInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		this.addInput(meanInput, true);
	}

	@Override
	protected double getNextNonZeroSample() {

		// Inverse transform method
		return (- meanInput.getValue() * Math.log( randomGenerator1.nextDouble() ) );
	}

	@Override
	protected double getMeanValue() {
		return meanInput.getValue();
	}

	@Override
	protected double getStandardDeviation() {
		return meanInput.getValue();
	}
}
