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

import com.jaamsim.font.TessFont;
import com.jaamsim.font.TessString;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Transform;

/**
 * A Render proxy for TessStrings
 * @author Matt.Chudleigh
 *
 */
public class StringProxy implements RenderProxy {

	private String _contents;
	private TessFontKey _fontKey;
	private Color4d _fontColour;
	private Mat4d _trans;
	private double _height;
	private long _pickingID;
	private VisibilityInfo _visInfo;

	private TessString cachedString = null;

	public StringProxy(String cont, TessFontKey fontKey, Color4d colour, Transform trans,
	                   double height, VisibilityInfo visInfo, long pickingID) {
		this(cont, fontKey, colour, trans.getMat4dRef(), height, visInfo, pickingID);
	}

	public StringProxy(String cont, TessFontKey fontKey, Color4d colour, Mat4d trans,
	                   double height, VisibilityInfo visInfo, long pickingID) {
		_contents = cont;
		_fontKey = fontKey;
		_fontColour = colour;
		_trans = trans;
		_height = height;
		_pickingID = pickingID;
		_visInfo = visInfo;
	}


	@Override
	public void collectRenderables(Renderer r, ArrayList<Renderable> outList) {
		TessFont tf = r.getTessFont(_fontKey);

		if (cachedString == null) {
			cachedString = new TessString(tf, _contents, _fontColour, _trans, _height, _visInfo, _pickingID);
		}

		outList.add(cachedString);
	}

	@Override
	public void collectOverlayRenderables(Renderer r, ArrayList<OverlayRenderable> outList) {
		// None
	}

}
