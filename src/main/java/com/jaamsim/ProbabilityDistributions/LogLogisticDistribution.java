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
 * Log-Logistic Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 456.
 */
public class LogLogisticDistribution extends Distribution {

	@Keyword(description = "The scale parameter for the Log-Logistic distribution.",
	         example = "LogLogisticDist1 Scale { 3.0 }")
	private final ValueInput scaleInput;

	@Keyword(description = "The shape parameter for the Log-Logistic distribution.  A decimal value > 0.0.",
	         example = "LogLogisticDist1 Shape { 1.0 }")
	private final ValueInput shapeInput;

	private final MRG1999a rng = new MRG1999a();

	{
		minValueInput.setDefaultValue(0.0);

		scaleInput = new ValueInput("Scale", "Key Inputs", 1.0d);
		scaleInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		scaleInput.setUnitType( UserSpecifiedUnit.class );
		this.addInput(scaleInput);

		shapeInput = new ValueInput("Shape", "Key Inputs", 1.0d);
		shapeInput.setValidRange( 2.000001d, Double.POSITIVE_INFINITY);
		shapeInput.setUnitType( DimensionlessUnit.class );
		this.addInput(shapeInput);
	}

	public LogLogisticDistribution() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected void setUnitType(Class<? extends Unit> ut) {
		super.setUnitType(ut);
		scaleInput.setUnitType(ut);
	}

	@Override
	protected double getNextSample() {

		// Inverse transform method
		double u = rng.nextUniform();
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
