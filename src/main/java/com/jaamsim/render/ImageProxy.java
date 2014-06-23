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

import java.net.URI;
import java.util.ArrayList;

import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;

public class ImageProxy implements RenderProxy {

	private URI _imageURI;
	private Transform _trans;
	private Vec3d _scale;
	private long _pickingID;
	private boolean _isTransparent;
	private boolean _isCompressed;
	private VisibilityInfo _visInfo;

	private TextureView cached;

	public ImageProxy(URI url, Transform trans, Vec3d scale, boolean isTransparent, boolean isCompressed,
	                  VisibilityInfo visInfo, long pickingID) {
		_imageURI = url;
		_trans = trans;
		_scale = RenderUtils.fixupScale(scale);
		_isTransparent = isTransparent;
		_pickingID = pickingID;
		_isCompressed = isCompressed;
		_visInfo = visInfo;
	}


	@Override
	public void collectRenderables(Renderer r, ArrayList<Renderable> outList) {
		if (cached == null) {
			cached = new TextureView(_imageURI, _trans, _scale, _isTransparent, _isCompressed, _visInfo, _pickingID);
		}
		outList.add(cached);

	}

	@Override
	public void collectOverlayRenderables(Renderer r, ArrayList<OverlayRenderable> outList) {
		// None
	}

}
