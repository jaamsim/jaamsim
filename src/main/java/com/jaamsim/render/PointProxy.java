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
import com.jaamsim.math.Vec4d;

public class PointProxy implements RenderProxy {

	private List<Vec4d> _points;
	private Color4d _colour;
	private Color4d _hoverColour;
	private double _pointWidth;
	private long _pickingID;
	private VisibilityInfo _visInfo;

	private DebugPoints cached;

	public PointProxy(List<Vec4d> points, Color4d colour, double pointWidth, VisibilityInfo visInfo, long pickingID) {
		_points = points;
		_colour = colour;
		_hoverColour = colour;
		_pointWidth = pointWidth;
		_pickingID = pickingID;
		_visInfo = visInfo;
	}

	public void setHoverColour(Color4d hoverColour) {
		_hoverColour = hoverColour;
	}

	@Override
	public void collectRenderables(Renderer r, ArrayList<Renderable> outList) {
		if (cached == null) {
			cached = new DebugPoints(_points, _colour, _hoverColour, _pointWidth, _visInfo, _pickingID);
		}

		outList.add(cached);
	}

	@Override
	public void collectOverlayRenderables(Renderer r,
			ArrayList<OverlayRenderable> outList) {
		// None
	}

}
