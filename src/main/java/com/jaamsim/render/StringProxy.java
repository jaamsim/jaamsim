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

import com.jaamsim.font.TessFont;
import com.jaamsim.font.TessString;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Matrix4d;
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
	private Matrix4d _trans;
	private double _height;
	private long _pickingID;

	private TessString cachedString = null;

	public StringProxy(String cont, TessFontKey fontKey, Color4d colour, Transform trans, double height, long pickingID) {
		this(cont, fontKey, colour, trans.getMatrixRef(), height, pickingID);
	}

	public StringProxy(String cont, TessFontKey fontKey, Color4d colour, Matrix4d trans, double height, long pickingID) {
		_contents = cont;
		_fontKey = fontKey;
		_fontColour = colour;
		_trans = trans;
		_height = height;
		_pickingID = pickingID;
	}


	@Override
	public void collectRenderables(Renderer r, ArrayList<Renderable> outList) {
		TessFont tf = r.getTessFont(_fontKey);

		if (cachedString == null) {
			cachedString = new TessString(tf, _contents, _fontColour, _trans, _height, _pickingID);
		}

		outList.add(cachedString);
	}

	@Override
	public void collectOverlayRenderables(Renderer r, ArrayList<OverlayRenderable> outList) {
		// None
	}

}
