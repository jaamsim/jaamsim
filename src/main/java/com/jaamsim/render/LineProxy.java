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

public class LineProxy implements RenderProxy {

	private List<Vec4d> _lineSegments;
	private Color4d _colour;
	private Color4d _hoverColour;
	private double _lineWidth;
	private long _pickingID;
	private DebugLine _cachedLine;
	private VisibilityInfo _visInfo;

	public LineProxy(List<Vec4d> lineSegments, Color4d colour, double lineWidth, VisibilityInfo visInfo, long pickingID) {
		_lineSegments = lineSegments;
		_colour = colour;
		_hoverColour = colour;
		_lineWidth = lineWidth;
		_pickingID = pickingID;
		_visInfo = visInfo;

		assert(lineSegments.size() >= 2);
	}

	public void setHoverColour(Color4d hoverColour) {
		_hoverColour = hoverColour;
	}

	@Override
	public void collectRenderables(Renderer r, ArrayList<Renderable> outList) {

		if (_cachedLine == null) {
			_cachedLine = new DebugLine(_lineSegments, _colour, _hoverColour, _lineWidth, _visInfo, _pickingID);
		}
		outList.add(_cachedLine);
	}
	@Override
	public void collectOverlayRenderables(Renderer r,
			ArrayList<OverlayRenderable> outList) {
		// None
	}

}
