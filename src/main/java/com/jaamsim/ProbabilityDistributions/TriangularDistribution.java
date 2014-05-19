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
import com.sandwell.JavaSimulation.InputErrorException;

/**
 * Triangular Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 457.
 */
public class TriangularDistribution extends Distribution {

	@Keyword(description = "The mode of the triangular distribution, i.e. the value with the highest probability.",
	         example = "TriangularDist1 Mode { 5.0 }")
	private final ValueInput modeInput;

	private final MRG1999a rng = new MRG1999a();

	{
		modeInput = new ValueInput("Mode", "Key Inputs", 1.0d);
		modeInput.setUnitType(UserSpecifiedUnit.class);
		this.addInput(modeInput);
	}

	public TriangularDistribution() {}

	@Override
	public void validate() {
		super.validate();

		// The mode must be between the minimum and maximum values
		if( this.getMinValue() > modeInput.getValue() ) {
			throw new InputErrorException( "The input for Mode must be >= than that for MinValue.");
		}
		if( this.getMaxValue() < modeInput.getValue() ) {
			throw new InputErrorException( "The input for Mode must be <= than that for MaxValue.");
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected void setUnitType(Class<? extends Unit> specified) {
		super.setUnitType(specified);
		modeInput.setUnitType(specified);
	}

	@Override
	protected double getNextSample() {

		double sample;
		double min = this.getMinValue();
		double max = this.getMaxValue();

		// Select the random value
		double rand = rng.nextUniform();

		// Calculate the normalised mode
		double m = ( modeInput.getValue() - min )/ ( max - min );

		// Use the inverse transform method to calculate the normalised random sample
		// (triangular distribution with min = 0, max = 1, and mode = m)
		if( rand <= m ) {
			sample = Math.sqrt( m * rand );
		}
		else {
			sample = 1.0 - Math.sqrt( ( 1.0 - m )*( 1.0 - rand ) );
		}

		// Adjust for the desired min and max values
		return  min + sample * ( max - min );
	}

	@Override
	protected double getMeanValue() {
		return ( ( this.getMinValue() + modeInput.getValue() + this.getMaxValue() ) / 3.0 );
	}

	@Override
	protected double getStandardDeviation() {
		double a = this.getMinValue();
		double b = this.getMaxValue();
		double m = modeInput.getValue();
		return  Math.sqrt( ( a*a + b*b + m*m - a*b - a*m - b*m ) / 18.0 );
	}
}
