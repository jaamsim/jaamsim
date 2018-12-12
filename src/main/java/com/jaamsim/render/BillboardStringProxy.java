/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import com.jaamsim.font.BillboardString;
import com.jaamsim.font.TessFont;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;

public class BillboardStringProxy implements RenderProxy {

	private final String _contents;
	private final TessFontKey _fontKey;
	private final Color4d _fontColour;
	private final double _height;
	private final double _xOffset, _yOffset;
	private final Vec3d _pos;
	private final VisibilityInfo _visInfo;
	private final long _pickingID;
	BillboardString cachedString;

	public BillboardStringProxy(String cont, TessFontKey fontKey, Color4d colour,
	                          double height, Vec3d pos, double xOffset, double yOffset,
	                          VisibilityInfo visInfo, long pickingID) {
		_contents = cont;
		_fontKey = fontKey;
		_fontColour = colour;
		_height = height;
		_xOffset = xOffset;
		_yOffset = yOffset;
		_pos = pos;
		_visInfo = visInfo;
		_pickingID = pickingID;
	}

	@Override
	public void collectRenderables(Renderer r, ArrayList<Renderable> outList) {
		// None
	}

	@Override
	public void collectOverlayRenderables(Renderer r, ArrayList<OverlayRenderable> outList) {

		if (cachedString == null) {
			TessFont tf = r.getTessFont(_fontKey);
			cachedString = new BillboardString(tf, _contents, _fontColour, _height, _pos, _xOffset, _yOffset, _visInfo, _pickingID);
		}
		outList.add(cachedString);

	}

}
