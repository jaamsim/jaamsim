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

import java.util.ArrayList;

import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Output;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.HasScreenPoints;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.Vec3dListInput;

/**
 * FluidPipe is a pipe through which fluid can flow.
 * @author Harry King
 *
 */
public class FluidPipe extends FluidComponent implements HasScreenPoints {

	@Keyword(description = "The length of the pipe.",
	         example = "Pipe1 Length { 10.0 m }")
	private final DoubleInput lengthInput;

	@Keyword(description = "The height change over the length of the pipe.  " +
			"Equal to (outlet height - inlet height).",
	         example = "Pipe1 HeightChange { 0.0 }")
	private final DoubleInput heightChangeInput;

	@Keyword(description = "The roughness height of the inside pipe surface.  " +
			"Used to calculate the Darcy friction factor for the pipe.",
	         example = "Pipe1 Roughness { 0.01 m }")
	private final DoubleInput roughnessInput;

	@Keyword(description = "The pressure loss coefficient or 'K-factor' for the pipe.  " +
			"The factor multiplies the dynamic pressure and is applied as a loss at the pipe outlet.",
	         example = "Pipe1 PressureLossCoefficient { 0.5 }")
	private final DoubleInput pressureLossCoefficientInput;

    @Keyword(description = "A list of points in { x, y, z } coordinates defining the line segments that" +
            "make up the pipe.  When two coordinates are given it is assumed that z = 0." ,
             example = "Pipe1  Points { { 6.7 2.2 m } { 4.9 2.2 m } { 4.9 3.4 m } }")
	private final Vec3dListInput pointsInput;

	@Keyword(description = "The width of the pipe segments in pixels.",
	         example = "Pipe1 Width { 1 }")
	private final DoubleInput widthInput;

	@Keyword(description = "The colour of the pipe, defined using a colour keyword or RGB values.",
	         example = "Pipe1 Colour { red }")
	private final ColourInput colourInput;

	private double darcyFrictionFactor;  // The Darcy Friction Factor for the pipe flow.

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
		pressureLossCoefficientInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		this.addInput( pressureLossCoefficientInput, true);

		ArrayList<Vec3d> defPoints =  new ArrayList<Vec3d>();
		defPoints.add(new Vec3d(0.0d, 0.0d, 0.0d));
		defPoints.add(new Vec3d(1.0d, 0.0d, 0.0d));
		pointsInput = new Vec3dListInput("Points", "Key Inputs", defPoints);
		pointsInput.setValidCountRange( 2, Integer.MAX_VALUE );
		pointsInput.setUnitType(DistanceUnit.class);
		this.addInput(pointsInput, true);

		widthInput = new DoubleInput("Width", "Key Inputs", 1.0d);
		widthInput.setValidRange(1.0d, Double.POSITIVE_INFINITY);
		this.addInput(widthInput, true);

		colourInput = new ColourInput("Colour", "Key Inputs", ColourInput.BLACK);
		this.addInput(colourInput, true, "Color");
	}

	@Override
	public double calcOutletPressure( double inletPres, double flowAccel ) {

		double dyn = this.getDynamicPressure();  // Note that dynamic pressure is negative for negative velocities
		double pres = inletPres;
		pres -= this.getFluid().getDensityxGravity() * heightChangeInput.getValue();
		if( Math.abs(dyn) > 0.0 && this.getFluid().getViscosity() > 0.0 ) {
			this.setDarcyFrictionFactor();
			pres -= darcyFrictionFactor * dyn * this.getLength() / this.getDiameter();
		}
		else {
			darcyFrictionFactor = 0.0;
		}
		pres -= pressureLossCoefficientInput.getValue() * dyn;
		pres -= flowAccel * this.getFluid().getDensity() * lengthInput.getValue() / this.getFlowArea();
		return pres;
	}

	@Override
	public double getLength() {
		return lengthInput.getValue();
	}

	private void setDarcyFrictionFactor() {

		double reynoldsNumber = this.getReynoldsNumber();

		// Laminar Flow
		if( reynoldsNumber < 2300.0 ) {
			darcyFrictionFactor = this.getLaminarFrictionFactor( reynoldsNumber );
		}
		// Turbulent Flow
		else if( reynoldsNumber > 4000.0 ) {
			darcyFrictionFactor = this.getTurbulentFrictionFactor( reynoldsNumber );
		}
		// Transitional Flow
		else {
			darcyFrictionFactor = 0.5 * ( this.getLaminarFrictionFactor(reynoldsNumber) + this.getTurbulentFrictionFactor(reynoldsNumber) );
		}
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

	@Override
	public ArrayList<Vec3d> getScreenPoints() {
		return pointsInput.getValue();
	}

	@Override
	public boolean selectable() {
		return true;
	}

	/**
	 *  Inform simulation and editBox of new positions.
	 */
	@Override
	public void dragged(Vec3d dist) {
		ArrayList<Vec3d> vec = new ArrayList<Vec3d>(pointsInput.getValue().size());
		for (Vec3d v : pointsInput.getValue()) {
			vec.add(new Vec3d(v.x + dist.x, v.y + dist.y, v.z + dist.z));
		}

		StringBuilder tmp = new StringBuilder();
		for (Vec3d v : vec) {
			tmp.append(String.format(" { %.3f %.3f %.3f m }", v.x, v.y, v.z));
		}
		InputAgent.processEntity_Keyword_Value(this, pointsInput, tmp.toString());

		super.dragged(dist);
		setGraphicsDataDirty();
	}

	@Override
	public Color4d getDisplayColour() {
		return colourInput.getValue();
	}

	@Override
	public int getWidth() {
		int ret = widthInput.getValue().intValue();
		if (ret < 1) return 1;
		return ret;
	}

	@Output(name = "DarcyFrictionFactor",
	 description = "The Darcy Friction Factor for the pipe.",
	    unitType = DimensionlessUnit.class)
	public double getDarcyFrictionFactor(double simTime) {
		return darcyFrictionFactor;
	}
}
