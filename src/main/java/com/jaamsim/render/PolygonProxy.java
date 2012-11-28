/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.render;

import java.util.ArrayList;
import java.util.List;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;

/**
 * Wrapper around a Polygon renderable
 * @author matt.chudleigh
 *
 */
public class PolygonProxy implements RenderProxy {

	private List<Vector4d> _points;

	private Color4d _colour;
	private Color4d _hoverColour;
	private boolean _isOutline;
	private double _lineWidth; // only meaningful if (isOutline)
	private long _pickingID;
	private Transform _trans;
	private Vector4d _scale;

	private Polygon cachedPoly;

	public PolygonProxy(List<Vector4d> points, Transform trans, Vector4d scale,
	                    Color4d colour, boolean isOutline, double lineWidth, long pickingID) {
		_colour = colour;
		_hoverColour = colour;
		_points = points;
		_trans = trans;
		_scale = scale;
		_isOutline = isOutline;
		_lineWidth = lineWidth;
		_pickingID = pickingID;
	}

	public void setHoverColour(Color4d hoverColour) {
		_hoverColour = hoverColour;
	}

	@Override
	public void collectRenderables(Renderer r, ArrayList<Renderable> outList) {
		if (cachedPoly == null) {

			cachedPoly = new Polygon(_points, _trans, _scale, _colour, _hoverColour, _pickingID);
			cachedPoly.isOutline = _isOutline;
			cachedPoly.lineWidth = _lineWidth;
		}

		outList.add(cachedPoly);
	}

	@Override
	public void collectOverlayRenderables(Renderer r,
			ArrayList<OverlayRenderable> outList) {
		// None
	}

}
