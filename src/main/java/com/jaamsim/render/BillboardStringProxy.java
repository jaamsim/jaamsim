/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
	BillboardString cachedString;

	public BillboardStringProxy(String cont, TessFontKey fontKey, Color4d colour,
	                          double height, Vec3d pos, double xOffset, double yOffset,
	                          VisibilityInfo visInfo) {
		_contents = cont;
		_fontKey = fontKey;
		_fontColour = colour;
		_height = height;
		_xOffset = xOffset;
		_yOffset = yOffset;
		_pos = pos;
		_visInfo = visInfo;
	}

	@Override
	public void collectRenderables(Renderer r, ArrayList<Renderable> outList) {
		// None
	}

	@Override
	public void collectOverlayRenderables(Renderer r, ArrayList<OverlayRenderable> outList) {

		if (cachedString == null) {
			TessFont tf = r.getTessFont(_fontKey);
			cachedString = new BillboardString(tf, _contents, _fontColour, _height, _pos, _xOffset, _yOffset, _visInfo);
		}
		outList.add(cachedString);

	}

}
