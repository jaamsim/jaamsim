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
 * Log-Logistic Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 456.
 */
public class LogLogisticDistribution extends NewProbabilityDistribution {

	@Keyword(desc = "The scale parameter for the Log-Logistic distribution.",
	         example = "LogLogisticDist-1 Scale { 3.0 }")
	private final DoubleInput scaleInput;

	@Keyword(desc = "The shape parameter for the Log-Logistic distribution.  A decimal value > 0.0.",
	         example = "LogLogisticDist-1 Shape { 1.0 }")
	private final DoubleInput shapeInput;

	{
		scaleInput = new DoubleInput("Scale", "Key Inputs", 1.0d);
		scaleInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		this.addInput(scaleInput, true);

		shapeInput = new DoubleInput("Shape", "Key Inputs", 1.0d);
		shapeInput.setValidRange( 2.000001d, Double.POSITIVE_INFINITY);
		this.addInput(shapeInput, true);
	}

	@Override
	protected double getNextNonZeroSample() {

		// Inverse transform method
		double u = randomGenerator1.nextDouble();
		return scaleInput.getValue() * Math.pow( u / (1 - u), 1.0 / shapeInput.getValue() );
	}

	@Override
	protected double getMeanValue() {
		double theta = Math.PI / shapeInput.getValue();
		return scaleInput.getValue() * theta / Math.sin( theta );
	}

	@Override
	protected double getStandardDeviation() {
		double theta = Math.PI / shapeInput.getValue();
		return scaleInput.getValue() * Math.sqrt( theta * ( 2.0/Math.sin(2.0*theta) - theta/Math.pow( Math.sin(theta), 2.0) ) );
	}
}
