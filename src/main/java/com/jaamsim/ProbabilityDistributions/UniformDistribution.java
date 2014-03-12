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

import com.jaamsim.rng.MRG1999a;

/**
 * Uniform Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 448.
 */
public class UniformDistribution extends Distribution {
	private final MRG1999a rng = new MRG1999a();

	public UniformDistribution() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected double getNextSample() {

		// Select the sample from a uniform distribution between the min and max values
		return this.getMinValue() + rng.nextUniform() * ( this.getMaxValue() - this.getMinValue() );
	}

	@Override
	protected double getMeanValue() {
		return ( 0.5 * ( this.getMinValue() + this.getMaxValue() ) );
	}

	@Override
	protected double getStandardDeviation() {
		return (  0.5 * ( this.getMaxValue() - this.getMinValue() ) / Math.sqrt(3.0) );
	}
}
