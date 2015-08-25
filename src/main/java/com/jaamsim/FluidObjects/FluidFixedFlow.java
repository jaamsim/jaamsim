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

import com.jaamsim.Graphics.PolylineInfo;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.VolumeFlowUnit;

/**
 * FluidFixedFlow models a specified flow rate between a source and destination.
 * A null source is taken to be an infinite reservoir to supply fluid.
 * A null destination is taken to be an infinite reservoir to receive fluid.
 * @author Harry King
 *
 */
public class FluidFixedFlow extends FluidFlowCalculation {

	@Keyword(description = "Volumetric flow rate.",
	         example = "FluidFixedFlow1 FlowRate { 1.0 m3/s }")
	private final ValueInput flowRateInput;

	@Keyword(description = "The width of the pipe segments in pixels.",
	         example = "Pipe1 Width { 1 }")
	private final ValueInput widthInput;

	@Keyword(description = "The colour of the pipe, defined using a colour keyword or RGB values.",
	         example = "Pipe1 Colour { red }")
	private final ColourInput colourInput;

	{
		flowRateInput = new ValueInput( "FlowRate", "Key Inputs", 0.0d);
		flowRateInput.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		flowRateInput.setUnitType( VolumeFlowUnit.class );
		this.addInput( flowRateInput);

		widthInput = new ValueInput("Width", "Key Inputs", 1.0d);
		widthInput.setValidRange(1.0d, Double.POSITIVE_INFINITY);
		widthInput.setUnitType( DimensionlessUnit.class );
		this.addInput(widthInput);

		colourInput = new ColourInput("Colour", "Key Inputs", ColourInput.BLACK);
		this.addInput(colourInput);
		this.addSynonym(colourInput, "Color");
	}

	@Override
	protected void calcFlowRate(FluidComponent source, FluidComponent destination, double dt) {

		// Update the flow rate
		this.setFlowRate( flowRateInput.getValue() );
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
	public PolylineInfo[] getScreenPoints() {
		synchronized(screenPointLock) {
			if (cachedPointInfo == null) {
				int w = Math.max(1, widthInput.getValue().intValue());
				cachedPointInfo = new PolylineInfo[1];
				cachedPointInfo[0] = new PolylineInfo(pointsInput.getValue(), colourInput.getValue(), w);
			}
			return cachedPointInfo;
		}
	}

}
