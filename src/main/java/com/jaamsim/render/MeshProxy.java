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
