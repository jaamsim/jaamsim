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
import com.jaamsim.math.Vec3d;

public class MeshProxy implements RenderProxy {

	private MeshProtoKey _assetKey;
	private Transform _trans;
	private Vec3d _scale;
	private long _pickingID;
	private VisibilityInfo _visInfo;
	private ArrayList<Action.Queue> _actions;

	private Mesh cached;

	public MeshProxy(MeshProtoKey assetKey, Transform trans, ArrayList<Action.Queue> actions, VisibilityInfo visInfo, long pickingID) {
		this(assetKey, trans, new Vec3d(1, 1, 1), actions, visInfo, pickingID);
	}

	public MeshProxy(MeshProtoKey assetKey, Transform trans, Vec3d scale, ArrayList<Action.Queue> actions, VisibilityInfo visInfo, long pickingID) {
		_assetKey = assetKey;
		_trans = trans;
		_scale = RenderUtils.fixupScale(scale);
		_pickingID = pickingID;
		_visInfo = visInfo;
		_actions = actions;
	}

	@Override
	public void collectRenderables(Renderer r, ArrayList<Renderable> outList) {
		if (cached == null) {
			MeshProto proto = r.getProto(_assetKey);

			cached = new Mesh(proto, _trans, _scale, _actions, _visInfo, _pickingID);
		}
		outList.add(cached);
	}

	@Override
	public void collectOverlayRenderables(Renderer r, ArrayList<OverlayRenderable> outList) {
		// None
	}
}
