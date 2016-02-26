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

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec4d;

public class PointProxy implements RenderProxy {

	private final List<Vec4d> _points;
	private final Color4d _colour;
	private Color4d _hoverColour;
	private final double _pointWidth;
	private final long _pickingID;
	private final VisibilityInfo _visInfo;

	private double _collisionAngle = 0.008727; // 0.5 degrees in radians

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
			cached.setCollisionAngle(_collisionAngle);
		}

		outList.add(cached);
	}

	@Override
	public void collectOverlayRenderables(Renderer r,
			ArrayList<OverlayRenderable> outList) {
		// None
	}

	public void setCollisionAngle(double angleRad) {
		_collisionAngle = angleRad;
	}
}
