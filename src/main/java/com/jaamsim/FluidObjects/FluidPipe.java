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
import java.util.Locale;

import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.HasScreenPoints;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.sandwell.JavaSimulation.ColourInput;
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
	private final ValueInput lengthInput;

	@Keyword(description = "The height change over the length of the pipe.  " +
			"Equal to (outlet height - inlet height).",
	         example = "Pipe1 HeightChange { 0.0 }")
	private final ValueInput heightChangeInput;

	@Keyword(description = "The roughness height of the inside pipe surface.  " +
			"Used to calculate the Darcy friction factor for the pipe.",
	         example = "Pipe1 Roughness { 0.01 m }")
	private final ValueInput roughnessInput;

	@Keyword(description = "The pressure loss coefficient or 'K-factor' for the pipe.  " +
			"The factor multiplies the dynamic pressure and is applied as a loss at the pipe outlet.",
	         example = "Pipe1 PressureLossCoefficient { 0.5 }")
	private final ValueInput pressureLossCoefficientInput;

    @Keyword(description = "A list of points in { x, y, z } coordinates defining the line segments that" +
            "make up the pipe.  When two coordinates are given it is assumed that z = 0." ,
             example = "Pipe1  Points { { 6.7 2.2 m } { 4.9 2.2 m } { 4.9 3.4 m } }")
	private final Vec3dListInput pointsInput;

	@Keyword(description = "The width of the pipe segments in pixels.",
	         example = "Pipe1 Width { 1 }")
	private final ValueInput widthInput;

	@Keyword(description = "The colour of the pipe, defined using a colour keyword or RGB values.",
	         example = "Pipe1 Colour { red }")
	private final ColourInput colourInput;

	private double darcyFrictionFactor;  // The Darcy Friction Factor for the pipe flow.

	private Object screenPointLock = new Object();
	private HasScreenPoints.PointsInfo[] cachedPointInfo;

	{
		lengthInput = new ValueInput( "Length", "Key Inputs", 1.0d);
		lengthInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		lengthInput.setUnitType( DistanceUnit.class );
		this.addInput( lengthInput);

		heightChangeInput = new ValueInput( "HeightChange", "Key Inputs", 0.0d);
		heightChangeInput.setValidRange( Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		heightChangeInput.setUnitType( DistanceUnit.class );
		this.addInput( heightChangeInput);

		roughnessInput = new ValueInput( "Roughness", "Key Inputs", 0.0d);
		roughnessInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		roughnessInput.setUnitType( DistanceUnit.class );
		this.addInput( roughnessInput);

		pressureLossCoefficientInput = new ValueInput( "PressureLossCoefficient", "Key Inputs", 0.0d);
		pressureLossCoefficientInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		pressureLossCoefficientInput.setUnitType( DimensionlessUnit.class );
		this.addInput( pressureLossCoefficientInput);

		ArrayList<Vec3d> defPoints =  new ArrayList<Vec3d>();
		defPoints.add(new Vec3d(0.0d, 0.0d, 0.0d));
		defPoints.add(new Vec3d(1.0d, 0.0d, 0.0d));
		pointsInput = new Vec3dListInput("Points", "Key Inputs", defPoints);
		pointsInput.setValidCountRange( 2, Integer.MAX_VALUE );
		pointsInput.setUnitType(DistanceUnit.class);
		this.addInput(pointsInput);

		widthInput = new ValueInput("Width", "Key Inputs", 1.0d);
		widthInput.setValidRange(1.0d, Double.POSITIVE_INFINITY);
		widthInput.setUnitType( DimensionlessUnit.class );
		this.addInput(widthInput);

		colourInput = new ColourInput("Colour", "Key Inputs", ColourInput.BLACK);
		this.addInput(colourInput);
		this.addSynonym(colourInput, "Color");
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
	public void updateForInput( Input<?> in ) {
		super.updateForInput(in);

		// If Points were input, then use them to set the start and end coordinates
		if( in == pointsInput || in == colourInput || in == widthInput ) {
			synchronized(screenPointLock) {
				cachedPointInfo = null;
			}
			return;
		}
	}

	@Override
	public HasScreenPoints.PointsInfo[] getScreenPoints() {
		synchronized(screenPointLock) {
			if (cachedPointInfo == null) {
				cachedPointInfo = new HasScreenPoints.PointsInfo[1];
				HasScreenPoints.PointsInfo pi = new HasScreenPoints.PointsInfo();
				cachedPointInfo[0] = pi;

				pi.points = pointsInput.getValue();
				pi.color = colourInput.getValue();
				pi.width = widthInput.getValue().intValue();
				if (pi.width < 1) pi.width = 1;
			}
			return cachedPointInfo;
		}
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
			tmp.append(String.format((Locale)null, " { %.3f %.3f %.3f m }", v.x, v.y, v.z));
		}
		InputAgent.processEntity_Keyword_Value(this, pointsInput, tmp.toString());

		super.dragged(dist);
	}

	@Output(name = "DarcyFrictionFactor",
	 description = "The Darcy Friction Factor for the pipe.",
	    unitType = DimensionlessUnit.class)
	public double getDarcyFrictionFactor(double simTime) {
		return darcyFrictionFactor;
	}
}
