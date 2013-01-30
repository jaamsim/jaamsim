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
	OverlayString cachedString;

	public OverlayStringProxy(String cont, TessFontKey fontKey, Color4d colour,
	                          double height, double x, double y, boolean alignRight, boolean alignBottom,
	                          VisibilityInfo visInfo) {
		_contents = cont;
		_fontKey = fontKey;
		_fontColour = colour;
		_height = height;
		_x = x; _y = y;
		_alignRight = alignRight; _alignBottom = alignBottom;
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
			cachedString = new OverlayString(tf, _contents, _fontColour, _height, _x, _y, _alignRight, _alignBottom, _visInfo);
		}
		outList.add(cachedString);

	}

}
