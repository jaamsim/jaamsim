/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018 JaamSim Software Inc.
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
import com.jaamsim.math.Vec2d;

public class OverlayPolygonProxy implements RenderProxy {

	private final List<Vec2d> points;
	private final Color4d colour;
	private final long pickingID;
	private OverlayPolygon cachedPoly;
	private final VisibilityInfo visInfo;
	private final boolean originTop;
	private final boolean originRight;

/***
 *
 * @param points -
 *   The vertices of the polygon. The points should form a convex polygon, otherwise overdrawing might occur
 * @param colour
 *   Color of the polygon
 * @param originTop
 *   Does this use the top of the screen as the origin (default is bottom)
 * @param originRight
 *   Does this use the right of the screen as the origin (default is left)
 * @param visInfo
 *   View visibility info
 * @param pickingID
 *   Currently unused
 */
	public OverlayPolygonProxy(  List<Vec2d> points, Color4d colour,
	                             boolean originTop, boolean originRight,
	                             VisibilityInfo visInfo, long pickingID) {
		this.points = points;
		this.colour = colour;
		this.pickingID = pickingID;
		this.visInfo = visInfo;
		this.originTop = originTop;
		this.originRight = originRight;
	}

	@Override
	public void collectRenderables(Renderer r, ArrayList<Renderable> outList) {
		// None
	}

	@Override
	public void collectOverlayRenderables(Renderer r, ArrayList<OverlayRenderable> outList) {

		if (points == null || points.size() < 2)
			return;

		if (cachedPoly == null) {
			cachedPoly = new OverlayPolygon(points, colour, originTop, originRight, visInfo, pickingID);
		}
		outList.add(cachedPoly);

	}

}
