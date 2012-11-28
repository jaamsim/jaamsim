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
import com.jaamsim.math.Vector4d;

public class MeshProxy implements RenderProxy {

	private MeshProtoKey _assetKey;
	private Transform _trans;
	private Vector4d _scale;
	private long _pickingID;

	public MeshProxy(MeshProtoKey assetKey, Transform trans, long pickingID) {
		this(assetKey, trans, new Vector4d(1, 1, 1), pickingID);
	}

	public MeshProxy(MeshProtoKey assetKey, Transform trans, Vector4d scale, long pickingID) {
		_assetKey = assetKey;
		_trans = trans;
		_scale = scale;
		_pickingID = pickingID;
	}

	@Override
	public void collectRenderables(Renderer r, ArrayList<Renderable> outList) {
		MeshProto proto = r.getProto(_assetKey);

		outList.add(new Mesh(_assetKey, proto, _trans, _scale, _pickingID));
	}

	@Override
	public void collectOverlayRenderables(Renderer r, ArrayList<OverlayRenderable> outList) {
		// None
	}
}
