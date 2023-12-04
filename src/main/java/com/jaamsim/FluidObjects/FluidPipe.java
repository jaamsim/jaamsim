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

import com.jaamsim.ColourProviders.ColourProvInput;
import com.jaamsim.DisplayModels.PolylineModel;
import com.jaamsim.Graphics.FillEntity;
import com.jaamsim.Graphics.LineEntity;
import com.jaamsim.Graphics.PolylineEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
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
	private final SampleInput lengthInput;

	@Keyword(description = "The height change over the length of the pipe. "
	                     + "Equal to (outlet height - inlet height).",
	         exampleList = {"0.0 m"})
	private final SampleInput heightChangeInput;

	@Keyword(description = "The roughness height of the inside pipe surface. "
	                     + "Used to calculate the Darcy friction factor for the pipe.",
	         exampleList = {"0.01 m"})
	private final SampleInput roughnessInput;

	@Keyword(description = "The pressure loss coefficient or 'K-factor' for the pipe. "
	                     + "The factor multiplies the dynamic pressure and is applied as a loss "
	                     + "at the pipe outlet.",
	         exampleList = {"0.5"})
	private final SampleInput pressureLossCoefficientInput;

	@Keyword(description = "Physical width of the pipe segments with units of distance.",
	         exampleList = { "0.5 m" })
	protected final SampleInput polylineWidth;

	@Keyword(description = "The width of the pipe segments in pixels.",
	         exampleList = {"1"})
	private final SampleInput widthInput;

	@Keyword(description = "The colour of the pipe.")
	private final ColourProvInput colourInput;

	private double darcyFrictionFactor;  // The Darcy Friction Factor for the pipe flow.

	{
		displayModelListInput.clearValidClasses();
		displayModelListInput.addValidClass(PolylineModel.class);

		lengthInput = new SampleInput("Length", KEY_INPUTS, 1.0d);
		lengthInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		lengthInput.setUnitType( DistanceUnit.class );
		this.addInput( lengthInput);

		heightChangeInput = new SampleInput("HeightChange", KEY_INPUTS, 0.0d);
		heightChangeInput.setValidRange( Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		heightChangeInput.setUnitType( DistanceUnit.class );
		this.addInput( heightChangeInput);

		roughnessInput = new SampleInput("Roughness", KEY_INPUTS, 0.0d);
		roughnessInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		roughnessInput.setUnitType( DistanceUnit.class );
		this.addInput( roughnessInput);

		pressureLossCoefficientInput = new SampleInput("PressureLossCoefficient", KEY_INPUTS, 0.0d);
		pressureLossCoefficientInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		pressureLossCoefficientInput.setUnitType( DimensionlessUnit.class );
		this.addInput( pressureLossCoefficientInput);

		polylineWidth = new SampleInput("PolylineWidth", FORMAT, 0.0d);
		polylineWidth.setUnitType(DistanceUnit.class);
		polylineWidth.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		polylineWidth.setDefaultText("PolylineModel");
		this.addInput(polylineWidth);

		widthInput = new SampleInput("LineWidth", FORMAT, 1);
		widthInput.setValidRange(1, Double.POSITIVE_INFINITY);
		widthInput.setIntegerValue(true);
		widthInput.setDefaultText("PolylineModel");
		this.addInput(widthInput);
		this.addSynonym(widthInput, "Width");

		colourInput = new ColourProvInput("LineColour", FORMAT, ColourInput.BLACK);
		colourInput.setDefaultText("PolylineModel");
		colourInput.setHidden(true);
		this.addInput(colourInput);
		this.addSynonym(colourInput, "Color");
		this.addSynonym(colourInput, "Colour");
	}

	@Override
	public double calcOutletPressure( double inletPres, double flowAccel ) {
		double simTime = EventManager.simSeconds();

		double dyn = getDynamicPressure(simTime);  // Note that dynamic pressure is negative for negative velocities
		double pres = inletPres;
		pres -= getFluid().getDensityxGravity(simTime) * heightChangeInput.getNextSample(this, simTime);
		if (Math.abs(dyn) > 0.0 && getFluid().getViscosity(simTime) > 0.0 ) {
			setDarcyFrictionFactor(simTime);
			pres -= darcyFrictionFactor * dyn * getLength(simTime) / getDiameter(simTime);
		}
		else {
			darcyFrictionFactor = 0.0;
		}
		pres -= pressureLossCoefficientInput.getNextSample(this, simTime) * dyn;
		pres -= flowAccel * getFluid().getDensity(simTime) * getLength(simTime) / getFlowArea();
		return pres;
	}

	@Override
	public double getLength(double simTime) {
		return lengthInput.getNextSample(this, simTime);
	}

	private void setDarcyFrictionFactor(double simTime) {

		double reynoldsNumber = this.getReynoldsNumber(simTime);

		// Laminar Flow
		if( reynoldsNumber < 2300.0 ) {
			darcyFrictionFactor = getLaminarFrictionFactor(simTime, reynoldsNumber);
		}
		// Turbulent Flow
		else if( reynoldsNumber > 4000.0 ) {
			darcyFrictionFactor = getTurbulentFrictionFactor(simTime, reynoldsNumber);
		}
		// Transitional Flow
		else {
			darcyFrictionFactor = 0.5 * (getLaminarFrictionFactor(simTime, reynoldsNumber) + getTurbulentFrictionFactor(simTime, reynoldsNumber));
		}
	}

	/*
	 * Return the Darcy Friction Factor for a laminar flow.
	 */
	private double getLaminarFrictionFactor(double simTime, double reynoldsNumber) {
		return 64.0 / reynoldsNumber;
	}

	/*
	 * Return the Darcy Friction Factor for a turbulent flow.
	 */
	private double getTurbulentFrictionFactor(double simTime, double reynoldsNumber) {
		double x = 1.0;  // The present value for x = 1 / sqrt( frictionfactor ).
		double lastx = 0.0;

		double a = (roughnessInput.getNextSample(this, simTime) / getDiameter(simTime)) / 3.7;
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
	public boolean isOutlined(double simTime) {
		return (getPolylineWidth(simTime) <= 0.0d);
	}

	@Override
	public int getLineWidth(double simTime) {
		if (widthInput.isDefault()) {
			LineEntity model = getDisplayModel(LineEntity.class);
			if (model != null)
				return model.getLineWidth(simTime);
		}
		return (int) widthInput.getNextSample(this, simTime);
	}

	@Override
	public Color4d getLineColour(double simTime) {
		if (colourInput.isDefault()) {
			LineEntity model = getDisplayModel(LineEntity.class);
			if (model != null)
				return model.getLineColour(simTime);
		}
		return colourInput.getNextColour(this, simTime);
	}

	@Override
	public boolean isFilled(double simTime) {
		return false;
	}

	@Override
	public Color4d getFillColour(double simTime) {
		if (getFluid() == null)
			return ColourInput.BLACK;
		return getFluid().getColour(simTime);
	}

	@Override
	public boolean isClosed(double simTime) {
		return false;
	}

	@Override
	public double getPolylineWidth(double simTime) {
		if (polylineWidth.isDefault()) {
			PolylineEntity model = getDisplayModel(PolylineEntity.class);
			if (model != null)
				return model.getPolylineWidth(simTime);
		}
		return polylineWidth.getNextSample(this, simTime);
	}

	@Output(name = "DarcyFrictionFactor",
	 description = "The Darcy Friction Factor for the pipe.",
	    unitType = DimensionlessUnit.class)
	public double getDarcyFrictionFactor(double simTime) {
		return darcyFrictionFactor;
	}

}
