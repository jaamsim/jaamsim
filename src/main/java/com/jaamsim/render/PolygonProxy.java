/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.render;

import java.util.ArrayList;
import java.util.List;

import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;

/**
 * Wrapper around a Polygon renderable
 * @author matt.chudleigh
 *
 */
public class PolygonProxy implements RenderProxy {

	private List<Vec4d> _points;

	private Color4d _colour;
	private Color4d _hoverColour;
	private boolean _isOutline;
	private double _lineWidth; // only meaningful if (isOutline)
	private long _pickingID;
	private Transform _trans;
	private Vec3d _scale;
	private VisibilityInfo _visInfo;

	private Polygon cachedPoly;

	public PolygonProxy(List<Vec4d> points, Transform trans, Vec3d scale,
	                    Color4d colour, boolean isOutline, double lineWidth, VisibilityInfo visInfo, long pickingID) {
		_colour = colour;
		if (_colour == null)
			throw new ErrorException("Null colour passed to PolygonProxy");
		_hoverColour = colour;
		_points = points;
		_trans = trans;
		_scale = RenderUtils.fixupScale(scale);
		_isOutline = isOutline;
		_lineWidth = lineWidth;
		_pickingID = pickingID;
		_visInfo = visInfo;
	}

	public void setHoverColour(Color4d hoverColour) {
		_hoverColour = hoverColour;
	}

	@Override
	public void collectRenderables(Renderer r, ArrayList<Renderable> outList) {
		if (cachedPoly == null) {

			cachedPoly = new Polygon(_points, _trans, _scale, _colour, _hoverColour, _visInfo, _isOutline, _lineWidth, _pickingID);
		}

		outList.add(cachedPoly);
	}

	@Override
	public void collectOverlayRenderables(Renderer r,
			ArrayList<OverlayRenderable> outList) {
		// None
	}

}
