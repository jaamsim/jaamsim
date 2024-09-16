/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
 * Copyright (C) 2024 JaamSim Software Inc.
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

public class LineProxy implements RenderProxy {

	private final List<Vec4d> _lineSegments;
	private final Color4d _colour;
	private Color4d _hoverColour;
	private final double _lineWidth;
	private final long _pickingID;
	private DebugLine _cachedLine;
	private final VisibilityInfo _visInfo;

	public LineProxy(List<Vec4d> lineSegments, Color4d colour, double lineWidth, VisibilityInfo visInfo, long pickingID) {
		_lineSegments = lineSegments;
		_colour = colour;
		_hoverColour = colour;
		_lineWidth = lineWidth;
		_pickingID = pickingID;
		_visInfo = visInfo;
	}

	public void setHoverColour(Color4d hoverColour) {
		_hoverColour = hoverColour;
	}

	@Override
	public void collectRenderables(Renderer r, ArrayList<Renderable> outList) {

		if (_lineSegments == null || _lineSegments.size() < 2 || _lineWidth < 1.0d)
			return;

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
