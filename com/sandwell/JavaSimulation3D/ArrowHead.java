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

import javax.media.j3d.ColoringAttributes;
import javax.vecmath.Vector3d;

import com.sandwell.JavaSimulation3D.util.Polygon;
import com.sandwell.JavaSimulation3D.util.Shape;

public class ArrowHead extends DisplayEntity {

	private final Polygon arrowFillModel;
	private final Polygon arrowOutlineModel;

	public ArrowHead() {

		// triangle
		double[] verts = { 0.5d, 0.0d, 0.0d,
						   -0.5d, 0.5d, 0.0d,
						   -0.5d, -0.5d, 0.0d };

		arrowFillModel = new Polygon( verts, Polygon.SHAPE_FILLED, "arrowFillModel" );
		arrowFillModel.setColor(Shape.COLOR_LIGHT_GREY);
		addShape( arrowFillModel );

		arrowOutlineModel = new Polygon( verts, Polygon.SHAPE_OUTLINE, "arrowOutlineModel" );
		arrowOutlineModel.setColor(Shape.COLOR_BLACK);
		addShape( arrowOutlineModel );

		this.setAlignment(new Vector3d(0.5d, 0.0d, 0.0d));
	}

	public void setColor( ColoringAttributes rgb ) {
		arrowFillModel.setColor( rgb );
		arrowOutlineModel.setColor( rgb );
	}
}
