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
import com.sandwell.JavaSimulation.IntegerInput;

/**
 * Erlang Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 449.
 */
public class ErlangDistribution extends Distribution {

	@Keyword(description = "The scale parameter for the Erlang distribution.",
	         example = "ErlangDist1 Mean { 5.0 }")
	private final ValueInput meanInput;

	@Keyword(description = "The shape parameter for the Erlang distribution.  An integer value >= 1.  " +
			"Shape = 1 gives the Exponential distribution.  " +
			"For Shape > 10 it is better to use the Gamma distribution.",
	         example = "ErlangDist1 Shape { 2 }")
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
