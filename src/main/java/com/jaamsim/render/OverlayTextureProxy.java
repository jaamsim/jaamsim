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

import java.net.URI;
import java.util.ArrayList;

public class OverlayTextureProxy implements RenderProxy {

	private int _x, _y;
	private int _width, _height;

	private URI _imageURI;

	private boolean _isTransparent;
	private boolean _isCompressed;
	private boolean _alignRight, _alignBottom;
	private VisibilityInfo _visInfo;
	private long _pickingID;

	private OverlayTexture cached;

	public OverlayTextureProxy(int x, int y, int width, int height, URI imageURI, boolean transparent, boolean compressed,
	                           boolean alignRight, boolean alignBottom, VisibilityInfo visInfo, long pickingID) {
		_x = x; _y = y;
		_width = width; _height = height;
		_imageURI = imageURI;
		_isTransparent = transparent; _isCompressed = compressed;
		_alignRight = alignRight; _alignBottom = alignBottom;
		_visInfo = visInfo;
		_pickingID = pickingID;
	}

	@Override
	public void collectRenderables(Renderer r, ArrayList<Renderable> outList) {
		return;
	}

	@Override
	public void collectOverlayRenderables(Renderer r,
			ArrayList<OverlayRenderable> outList) {
		if (cached == null) {
			cached = new OverlayTexture(_x, _y, _width, _height, _imageURI, _isTransparent, _isCompressed,
			                            _alignRight, _alignBottom, _visInfo, _pickingID);
		}
		outList.add(cached);
	}

}
