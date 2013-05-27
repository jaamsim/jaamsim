/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation3D;

import java.util.ArrayList;

import com.jaamsim.input.InputAgent;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.HasScreenPoints;
import com.jaamsim.units.DistanceUnit;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.Vec3dInput;
import com.sandwell.JavaSimulation.Vec3dListInput;

public class Arrow extends DisplayEntity implements HasScreenPoints {
	@Keyword(description = "A list of points in { x, y, z } coordinates defining the line segments that" +
                    "make up the arrow.  When two coordinates are given it is assumed that z = 0." ,
	         example = "Arrow1  Points { { 6.7 2.2 m } { 4.9 2.2 m } { 4.9 3.4 m } }")
	private final Vec3dListInput pointsInput;

	@Keyword(description = "If TRUE, then a drop shadow appears for the arrow.",
	         example = "Arrow1  DropShadow { TRUE }")
	private final BooleanInput dropShadow;

	@Keyword(description = "The colour of the drop shadow, defined using a colour keyword or RGB values.",
	         example = "Arrow1  DropShadowColour { blue }")
	private final ColourInput dropShadowColor;

	@Keyword(description = "A set of { x, y, z } offsets in each direction of the drop shadow from the Arrow.",
	         example = "Arrow1  DropShadowOffset { 0.1 0.1 0.0 m }")
	private final Vec3dInput dropShadowOffset;

	@Keyword(description = "The width of the Arrow line segments in pixels.",
	         example = "Arrow1 Width { 1 }")
	private final DoubleInput width;

	@Keyword(description = "A set of { x, y, z } numbers that define the size of the arrowhead " +
	                "in those directions at the end of the connector.",
	         example = "Arrow1 ArrowSize { 0.165 0.130 0.0 m }")
	private final Vec3dInput arrowHeadSize;

	@Keyword(description = "The colour of the arrow, defined using a colour keyword or RGB values.",
	         example = "Arrow1 Color { red }")
	private final ColourInput color;

	{
		ArrayList<Vec3d> defPoints =  new ArrayList<Vec3d>();
		defPoints.add(new Vec3d(0.0d, 0.0d, 0.0d));
		defPoints.add(new Vec3d(1.0d, 0.0d, 0.0d));
		pointsInput = new Vec3dListInput("Points", "Arrow Graphics", defPoints);
		pointsInput.setValidCountRange( 2, Integer.MAX_VALUE );
		pointsInput.setUnitType(DistanceUnit.class);
		this.addInput(pointsInput, true);

		width = new DoubleInput("Width", "Arrow Graphics", 1.0d);
		width.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(width, true);

		arrowHeadSize = new Vec3dInput( "ArrowSize", "Arrow Graphics", new Vec3d(0.1d, 0.1d, 0.0d) );
		arrowHeadSize.setUnitType(DistanceUnit.class);
		this.addInput( arrowHeadSize, true );

		color = new ColourInput("Color", "Arrow Graphics", ColourInput.BLACK);
		this.addInput(color, true, "Colour");

		dropShadow = new BooleanInput( "DropShadow", "Arrow Graphics", false );
		this.addInput( dropShadow, true );

		dropShadowColor = new ColourInput("DropShadowColour", "Arrow Graphics", ColourInput.BLACK);
		this.addInput(dropShadowColor, true, "DropShadowColor");

		dropShadowOffset = new Vec3dInput( "DropShadowOffset", "Arrow Graphics", new Vec3d() );
		dropShadowOffset.setUnitType(DistanceUnit.class);
		this.addInput( dropShadowOffset, true );
	}

	public Arrow() {}

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
			vec.add(new Vec3d(dist.x + v.x, dist.y + v.y, dist.z + v.z));
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
		return color.getValue();
	}

	@Override
	public int getWidth() {
		int ret = width.getValue().intValue();
		if (ret < 1) return 1;
		return ret;
	}

	public Vec3d getArrowHeadSize() {
		return arrowHeadSize.getValue();
	}
}
