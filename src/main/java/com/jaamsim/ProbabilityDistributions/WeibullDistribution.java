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
import com.jaamsim.math.Gamma;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Weibull Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 452.
 */
public class WeibullDistribution extends Distribution {

	@Keyword(description = "The scale parameter for the Weibull distribution.",
	         example = "WeibullDist1 Scale { 3.0 h }")
	private final ValueInput scaleInput;

	@Keyword(description = "The shape parameter for the Weibull distribution.  A decimal value > 0.0.",
	         example = "WeibullDist-1 Shape { 1.0 }")
	private final ValueInput shapeInput;

	@Keyword(description = "The location parameter for the Weibull distribution.",
	         example = "WeibullDist-1 Location { 5.0 h }")
	private final ValueInput locationInput;

	private final MRG1999a rng = new MRG1999a();

	{
		minValueInput.setDefaultValue(0.0);

		scaleInput = new ValueInput("Scale", "Key Inputs", 1.0d);
		scaleInput.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		scaleInput.setUnitType( UserSpecifiedUnit.class );
		this.addInput(scaleInput);

		locationInput = new ValueInput("Location", "Key Inputs", 0.0d);
		locationInput.setUnitType( UserSpecifiedUnit.class );
		this.addInput(locationInput);

		shapeInput = new ValueInput("Shape", "Key Inputs", 1.0d);
		shapeInput.setValidRange( 1.0e-10d, Double.POSITIVE_INFINITY);
		shapeInput.setUnitType( DimensionlessUnit.class );
		this.addInput(shapeInput);
	}

	public WeibullDistribution() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected void setUnitType(Class<? extends Unit> ut) {
		super.setUnitType(ut);
		scaleInput.setUnitType(ut);
		locationInput.setUnitType(ut);
	}

	@Override
	protected double getNextSample() {

		// Inverse transform method
		return  scaleInput.getValue() * Math.pow( - Math.log( rng.nextUniform() ), 1.0/shapeInput.getValue() ) + locationInput.getValue();
	}

	@Override
	protected double getMeanValue() {
		double shape = shapeInput.getValue();
		double scale = scaleInput.getValue();
		double loc = locationInput.getValue();
		return scale/shape * Gamma.gamma(1.0/shape) + loc;
	}

	@Override
	protected double getStandardDeviation() {
		double shape = shapeInput.getValue();
		double scale = scaleInput.getValue();
		return scale/shape * Math.sqrt( 2.0*shape*Gamma.gamma(2.0/shape) - Math.pow(Gamma.gamma(1.0/shape), 2.0) );
	}
}
