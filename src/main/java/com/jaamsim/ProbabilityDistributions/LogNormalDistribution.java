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
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * LogNormal Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 454.
 * Polar Method, Marsaglia and Bray (1964) is used to calculate the normal distribution
 */
public class LogNormalDistribution extends Distribution {

	@Keyword(description = "The mean of the normal distribution (not the mean of the lognormal).",
	         example = "LogNormalDist1 NormalMean { 5.0 }")
	private final ValueInput normalMeanInput;

	@Keyword(description = "The standard deviation of the normal distribution (not the standard deviation of the lognormal).",
	         example = "LogNormalDist1 NormalStandardDeviation { 2.0 }")
	private final ValueInput normalStandardDeviationInput;

	private final MRG1999a rng1 = new MRG1999a();
	private final MRG1999a rng2 = new MRG1999a();

	{
		minValueInput.setDefaultValue(0.0);

		normalMeanInput = new ValueInput("NormalMean", "Key Inputs", 0.0d);
		normalMeanInput.setUnitType(UserSpecifiedUnit.class);
		this.addInput(normalMeanInput);

		normalStandardDeviationInput = new ValueInput("NormalStandardDeviation", "Key Inputs", 1.0d);
		normalStandardDeviationInput.setUnitType(UserSpecifiedUnit.class);
		normalStandardDeviationInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(normalStandardDeviationInput);
	}

	public LogNormalDistribution() {}

	@Override
	public void earlyInit() {
		super.earlyInit();

		rng1.setSeedStream(getStreamNumber()    , getSubstreamNumber());
		rng2.setSeedStream(getStreamNumber() + 1, getSubstreamNumber());
	}

	@Override
	protected void setUnitType(Class<? extends Unit> specified) {
		super.setUnitType(specified);
		normalMeanInput.setUnitType(specified);
		normalStandardDeviationInput.setUnitType(specified);
	}

	@Override
	protected double getNextSample() {

		// Loop until we have a random x-y coordinate in the unit circle
		double w, v1, v2, sample;
		do {
			v1 = 2.0 * rng1.nextUniform() - 1.0;
			v2 = 2.0 * rng2.nextUniform() - 1.0;
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
