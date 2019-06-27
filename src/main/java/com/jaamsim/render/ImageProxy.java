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

import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;

public class ImageProxy implements RenderProxy {

	private final TexLoader _texLoader;
	private final Transform _trans;
	private final Vec3d _scale;
	private final long _pickingID;
	private final VisibilityInfo _visInfo;
	private final ArrayList<Vec2d> _texCoords;

	private TextureView cached;

	public ImageProxy(TexLoader loader, ArrayList<Vec2d> texCoords, Transform trans, Vec3d scale,
	                  VisibilityInfo visInfo, long pickingID) {
		_texLoader = loader;
		_trans = trans;
		_scale = RenderUtils.fixupScale(scale);
		_pickingID = pickingID;
		_visInfo = visInfo;
		_texCoords = texCoords;
	}

	public ImageProxy(TexLoader loader, Transform trans, Vec3d scale,
            VisibilityInfo visInfo, long pickingID){
		this(loader, null, trans, scale, visInfo, pickingID);
	}

	@Override
	public void collectRenderables(Renderer r, ArrayList<Renderable> outList) {
		if (cached == null) {
			cached = new TextureView(_texLoader, _texCoords, _trans, _scale, _visInfo, _pickingID);
		}
		outList.add(cached);

	}

	@Override
	public void collectOverlayRenderables(Renderer r, ArrayList<OverlayRenderable> outList) {
		// None
	}

}
