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
package com.jaamsim.Graphics;

import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.HasScreenPoints;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;

public class Arrow extends DisplayEntity implements HasScreenPoints {
	@Keyword(description = "If TRUE, then a drop shadow appears for the arrow.",
	         exampleList = {"TRUE"})
	private final BooleanInput dropShadow;

	@Keyword(description = "The colour of the drop shadow, defined using a colour keyword or RGB values.",
	         exampleList = {"blue"})
	private final ColourInput dropShadowColor;

	@Keyword(description = "A set of { x, y, z } offsets in each direction of the drop shadow from the Arrow.",
	         exampleList = {"0.1 0.1 0.0 m"})
	private final Vec3dInput dropShadowOffset;

	@Keyword(description = "The width of the Arrow line segments in pixels.",
	         exampleList = {"1"})
	private final ValueInput width;

	@Keyword(description = "A set of { x, y, z } numbers that define the size of the arrowhead " +
	                "in those directions at the end of the connector.",
	         exampleList = {"0.165 0.130 0.0 m"})
	private final Vec3dInput arrowHeadSize;

	@Keyword(description = "The colour of the arrow, defined using a colour keyword or RGB values.",
	         exampleList = {"red"})
	private final ColourInput color;

	{
		width = new ValueInput("Width", "Graphics", 1.0d);
		width.setUnitType(DimensionlessUnit.class);
		width.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(width);

		arrowHeadSize = new Vec3dInput( "ArrowSize", "Graphics", new Vec3d(0.1d, 0.1d, 0.0d) );
		arrowHeadSize.setUnitType(DistanceUnit.class);
		this.addInput( arrowHeadSize );

		color = new ColourInput("Color", "Graphics", ColourInput.BLACK);
		this.addInput(color);
		this.addSynonym(color, "Colour");

		dropShadow = new BooleanInput( "DropShadow", "Graphics", false );
		this.addInput( dropShadow );

		dropShadowColor = new ColourInput("DropShadowColour", "Graphics", ColourInput.BLACK);
		this.addInput(dropShadowColor);
		this.addSynonym(dropShadowColor, "DropShadowColor");

		dropShadowOffset = new Vec3dInput( "DropShadowOffset", "Graphics", new Vec3d() );
		dropShadowOffset.setUnitType(DistanceUnit.class);
		this.addInput( dropShadowOffset );
	}

	public Arrow() {}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput(in);

		// If Points were input, then use them to set the start and end coordinates
		if( in == pointsInput || in == color || in == width ) {
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
				cachedPointInfo = new PolylineInfo[1];
				PolylineInfo pi = new PolylineInfo();
				cachedPointInfo[0] = pi;

				pi.points = pointsInput.getValue();
				pi.color = color.getValue();
				pi.width = width.getValue().intValue();
				if (pi.width < 1) pi.width = 1;
			}
			return cachedPointInfo;
		}
	}

	/**
	 *  Inform simulation and editBox of new positions.
	 */
	@Override
	public void dragged(Vec3d dist) {
		KeywordIndex kw = InputAgent.formatPointsInputs(pointsInput.getKeyword(), pointsInput.getValue(), dist);
		InputAgent.apply(this, kw);
		super.dragged(dist);
	}

	public Vec3d getArrowHeadSize() {
		return arrowHeadSize.getValue();
	}
}
