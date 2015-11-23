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
