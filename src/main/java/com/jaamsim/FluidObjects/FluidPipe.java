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
package com.jaamsim.FluidObjects;

import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.ErrorException;

/**
 * FluidPipe is a pipe through which fluid can flow.
 * @author Harry King
 *
 */
public class FluidPipe extends FluidComponent {

	@Keyword(desc = "The length of the pipe.",
	         example = "Tank1 Length { 10.0 m }")
	private final DoubleInput lengthInput;

	@Keyword(desc = "The height change over the length of the pipe.  " +
			"Equal to (outlet height - inlet height).",
	         example = "Tank1 HeightChange { 0.0 }")
	private final DoubleInput heightChangeInput;

	@Keyword(desc = "The roughness height of the inside pipe surface.  " +
			"Used to calculate the Darcy friction factor for the pipe.",
	         example = "Tank1 Roughness { 0.01 m }")
	private final DoubleInput roughnessInput;

	@Keyword(desc = "The pressure loss coefficient or 'K-factor' for the pipe.  " +
			"The factor multiplies the dynamic pressure and is applied as a loss at the pipe outlet.",
	         example = "Tank1 PressureLossCoefficient { 0.5 }")
	private final DoubleInput pressureLossCoefficientInput;

	{
		lengthInput = new DoubleInput( "Length", "Key Inputs", 1.0d);
		lengthInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		lengthInput.setUnits( "m");
		this.addInput( lengthInput, true);

		heightChangeInput = new DoubleInput( "HeightChange", "Key Inputs", 0.0d);
		heightChangeInput.setValidRange( Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		heightChangeInput.setUnits( "m");
		this.addInput( heightChangeInput, true);

		roughnessInput = new DoubleInput( "Roughness", "Key Inputs", 0.0d);
		roughnessInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		roughnessInput.setUnits( "m");
		this.addInput( roughnessInput, true);

		pressureLossCoefficientInput = new DoubleInput( "PressureLossCoefficient", "Key Inputs", 0.0d);
		pressureLossCoefficientInput.setValidRange( 0.0, 1.0);
		this.addInput( pressureLossCoefficientInput, true);
	}

	@Override
	public double calcOutletPressure( double inletPres, double flowAccel ) {

		double dyn = this.getDynamicPressure();  // Note that dynamic pressure is negative for negative velocities
		double pres = inletPres;
		pres -= this.getFluid().getDensityxGravity() * heightChangeInput.getValue();
		if( dyn > 0.0 && this.getFluid().getViscosity() > 0.0 ) {
			pres -= this.getDarcyFrictionFactor() * dyn * this.getLength() / this.getDiameter();
		}
		pres -= pressureLossCoefficientInput.getValue() * dyn;
		pres -= flowAccel * this.getFluid().getDensity() * lengthInput.getValue() / this.getFlowArea();
		return pres;
	}

	@Override
	public double getLength() {
		return lengthInput.getValue();
	}

	private double getDarcyFrictionFactor() {

		double factor;
		double reynoldsNumber = this.getReynoldsNumber();

		System.out.println("Rn = " + reynoldsNumber);

		// Laminar Flow
		if( reynoldsNumber < 2300.0 ) {
			factor = this.getLaminarFrictionFactor( reynoldsNumber );
		}
		// Turbulent Flow
		else if( reynoldsNumber > 4000.0 ) {
			factor = this.getTurbulentFrictionFactor( reynoldsNumber );
		}
		// Transitional Flow
		else {
			factor = 0.5 * ( this.getLaminarFrictionFactor(reynoldsNumber) + this.getTurbulentFrictionFactor(reynoldsNumber) );
		}

		System.out.println("fd = " + factor);

		return factor;
	}

	/*
	 * Return the Darcy Friction Factor for a laminar flow.
	 */
	private double getLaminarFrictionFactor( double reynoldsNumber ) {
		return 64.0 / reynoldsNumber;
	}

	/*
	 * Return the Darcy Friction Factor for a turbulent flow.
	 */
	private double getTurbulentFrictionFactor( double reynoldsNumber ) {
		double x = 1.0;  // The present value for x = 1 / sqrt( frictionfactor ).
		double lastx = 0.0;

		double a = ( roughnessInput.getValue() / this.getDiameter() ) / 3.7;
		double b = 2.51 / reynoldsNumber;

		int n = 0;
		while( Math.abs(x-lastx)/lastx > 1.0e-10 && n < 20 ) {
			lastx = x;
			x = -2.0 * Math.log10( a + b*lastx );
			n++;
		}

		if( n >= 20 ) {
			throw new ErrorException( "Darcy Friction Factor iterations did not converge: " +
					"lastx = " + lastx + "  x = " + x + "  n = " + n);
		}

		return 1.0 / ( x * x );
	}
}
