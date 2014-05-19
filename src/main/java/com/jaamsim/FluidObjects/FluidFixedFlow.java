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
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.HasScreenPoints;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.VolumeFlowUnit;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.Vec3dListInput;

/**
 * FluidFixedFlow models a specified flow rate between a source and destination.
 * A null source is taken to be an infinite reservoir to supply fluid.
 * A null destination is taken to be an infinite reservoir to receive fluid.
 * @author Harry King
 *
 */
public class FluidFixedFlow extends FluidFlowCalculation implements HasScreenPoints {

	@Keyword(description = "Volumetric flow rate.",
	         example = "FluidFixedFlow1 FlowRate { 1.0 m3/s }")
	private final ValueInput flowRateInput;

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

	private Object screenPointLock = new Object();
	private HasScreenPoints.PointsInfo[] cachedPointInfo;

	{
		flowRateInput = new ValueInput( "FlowRate", "Key Inputs", 0.0d);
		flowRateInput.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		flowRateInput.setUnitType( VolumeFlowUnit.class );
		this.addInput( flowRateInput);

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

}
