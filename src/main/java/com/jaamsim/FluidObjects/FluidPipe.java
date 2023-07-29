/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2023 JaamSim Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.FluidObjects;

import com.jaamsim.DisplayModels.PolylineModel;
import com.jaamsim.Graphics.FillEntity;
import com.jaamsim.Graphics.LineEntity;
import com.jaamsim.Graphics.PolylineEntity;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;

/**
 * FluidPipe is a pipe through which fluid can flow.
 * @author Harry King
 *
 */
public class FluidPipe extends FluidComponent implements LineEntity, FillEntity, PolylineEntity {

	@Keyword(description = "The length of the pipe.",
	         exampleList = {"10.0 m"})
	private final ValueInput lengthInput;

	@Keyword(description = "The height change over the length of the pipe. "
	                     + "Equal to (outlet height - inlet height).",
	         exampleList = {"0.0 m"})
	private final ValueInput heightChangeInput;

	@Keyword(description = "The roughness height of the inside pipe surface. "
	                     + "Used to calculate the Darcy friction factor for the pipe.",
	         exampleList = {"0.01 m"})
	private final ValueInput roughnessInput;

	@Keyword(description = "The pressure loss coefficient or 'K-factor' for the pipe. "
	                     + "The factor multiplies the dynamic pressure and is applied as a loss "
	                     + "at the pipe outlet.",
	         exampleList = {"0.5"})
	private final ValueInput pressureLossCoefficientInput;

	@Keyword(description = "Physical width of the pipe segments with units of distance.",
	         exampleList = { "0.5 m" })
	protected final ValueInput polylineWidth;

	@Keyword(description = "The width of the pipe segments in pixels.",
	         exampleList = {"1"})
	private final IntegerInput widthInput;

	@Keyword(description = "The colour of the pipe.",
	         exampleList = {"red"})
	private final ColourInput colourInput;

	private double darcyFrictionFactor;  // The Darcy Friction Factor for the pipe flow.

	{
		displayModelListInput.clearValidClasses();
		displayModelListInput.addValidClass(PolylineModel.class);

		lengthInput = new ValueInput( "Length", KEY_INPUTS, 1.0d);
		lengthInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		lengthInput.setUnitType( DistanceUnit.class );
		this.addInput( lengthInput);

		heightChangeInput = new ValueInput( "HeightChange", KEY_INPUTS, 0.0d);
		heightChangeInput.setValidRange( Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		heightChangeInput.setUnitType( DistanceUnit.class );
		this.addInput( heightChangeInput);

		roughnessInput = new ValueInput( "Roughness", KEY_INPUTS, 0.0d);
		roughnessInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		roughnessInput.setUnitType( DistanceUnit.class );
		this.addInput( roughnessInput);

		pressureLossCoefficientInput = new ValueInput( "PressureLossCoefficient", KEY_INPUTS, 0.0d);
		pressureLossCoefficientInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		pressureLossCoefficientInput.setUnitType( DimensionlessUnit.class );
		this.addInput( pressureLossCoefficientInput);

		polylineWidth = new ValueInput("PolylineWidth", FORMAT, 0.0d);
		polylineWidth.setUnitType(DistanceUnit.class);
		polylineWidth.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		polylineWidth.setDefaultText("PolylineModel");
		this.addInput(polylineWidth);

		widthInput = new IntegerInput("LineWidth", FORMAT, 1);
		widthInput.setValidRange(1, Integer.MAX_VALUE);
		widthInput.setDefaultText("PolylineModel");
		this.addInput(widthInput);
		this.addSynonym(widthInput, "Width");

		colourInput = new ColourInput("LineColour", FORMAT, ColourInput.BLACK);
		colourInput.setDefaultText("PolylineModel");
		colourInput.setHidden(true);
		this.addInput(colourInput);
		this.addSynonym(colourInput, "Color");
		this.addSynonym(colourInput, "Colour");
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
			error("Darcy Friction Factor iterations did not converge: lastx = %f  x = %f  n = %d",
			      lastx, x, n);
		}

		return 1.0 / ( x * x );
	}

	@Override
	public boolean isOutlined() {
		return (getPolylineWidth() <= 0.0d);
	}

	@Override
	public int getLineWidth() {
		if (widthInput.isDefault()) {
			LineEntity model = getDisplayModel(LineEntity.class);
			if (model != null)
				return model.getLineWidth();
		}
		return widthInput.getValue();
	}

	@Override
	public Color4d getLineColour(double simTime) {
		if (colourInput.isDefault()) {
			LineEntity model = getDisplayModel(LineEntity.class);
			if (model != null)
				return model.getLineColour(simTime);
		}
		return colourInput.getValue();
	}

	@Override
	public boolean isFilled() {
		return false;
	}

	@Override
	public Color4d getFillColour(double simTime) {
		if (getFluid() == null)
			return ColourInput.BLACK;
		return getFluid().getColour();
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public double getPolylineWidth() {
		if (polylineWidth.isDefault()) {
			PolylineEntity model = getDisplayModel(PolylineEntity.class);
			if (model != null)
				return model.getPolylineWidth();
		}
		return polylineWidth.getValue();
	}

	@Output(name = "DarcyFrictionFactor",
	 description = "The Darcy Friction Factor for the pipe.",
	    unitType = DimensionlessUnit.class)
	public double getDarcyFrictionFactor(double simTime) {
		return darcyFrictionFactor;
	}

}
