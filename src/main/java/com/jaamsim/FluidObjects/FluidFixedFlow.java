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
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.HasScreenPoints;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Keyword;
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
	         example = "FluidFixedFlow-1 FlowRate { 1.0 m3/s }")
	private final DoubleInput flowRateInput;

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

	{
		flowRateInput = new DoubleInput( "FlowRate", "Key Inputs", 0.0d);
		flowRateInput.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		flowRateInput.setUnits( "m3/s");
		this.addInput( flowRateInput, true);

		ArrayList<Vec3d> defPoints =  new ArrayList<Vec3d>();
		defPoints.add(new Vec3d(0.0d, 0.0d, 0.0d));
		defPoints.add(new Vec3d(1.0d, 0.0d, 0.0d));
		pointsInput = new Vec3dListInput("Points", "Key Inputs", defPoints);
		pointsInput.setValidCountRange( 2, Integer.MAX_VALUE );
		pointsInput.setUnits("m");
		this.addInput(pointsInput, true);

		widthInput = new DoubleInput("Width", "Key Inputs", 1.0d);
		widthInput.setValidRange(1.0d, Double.POSITIVE_INFINITY);
		this.addInput(widthInput, true);

		colourInput = new ColourInput("Colour", "Key Inputs", ColourInput.BLACK);
		this.addInput(colourInput, true, "Color");
	}

	@Override
	protected void calcFlowRate(FluidComponent source, FluidComponent destination, double dt) {

		// Update the flow rate
		this.setFlowRate( flowRateInput.getValue() );
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
			tmp.append(String.format(" { %.3f %.3f %.3f %s }", v.x, v.y, v.z, pointsInput.getUnits()));
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
}
