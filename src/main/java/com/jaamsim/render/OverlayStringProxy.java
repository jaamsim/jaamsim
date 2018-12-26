/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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

import com.jaamsim.font.OverlayString;
import com.jaamsim.font.TessFont;
import com.jaamsim.math.Color4d;

public class OverlayStringProxy implements RenderProxy {

	private String _contents;
	private TessFontKey _fontKey;
	private Color4d _fontColour;
	private double _x, _y;
	private double _height;
	private boolean _alignRight, _alignBottom;
	private VisibilityInfo _visInfo;
	private final long _pickingID;
	OverlayString cachedString;

	public OverlayStringProxy(String cont, TessFontKey fontKey, Color4d colour,
	                          double height, double x, double y, boolean alignRight, boolean alignBottom,
	                          VisibilityInfo visInfo, long pickingID) {
		_contents = cont;
		_fontKey = fontKey;
		_fontColour = colour;
		_height = height;
		_x = x; _y = y;
		_alignRight = alignRight; _alignBottom = alignBottom;
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
			cachedString = new OverlayString(tf, _contents, _fontColour, _height, _x, _y, _alignRight, _alignBottom, _visInfo, _pickingID);
		}
		outList.add(cachedString);

	}

}
