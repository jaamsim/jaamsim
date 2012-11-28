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
package com.jaamsim.observers;

import java.net.URL;
import java.util.ArrayList;

import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;
import com.jaamsim.render.MeshProtoKey;
import com.jaamsim.render.MeshProxy;
import com.jaamsim.render.RenderProxy;
import com.sandwell.JavaSimulation.Entity;

public class CubeObserver extends RenderObserver {
	public static URL CUBE_URL;
	public static MeshProtoKey CUBE_KEY;

	static {
		try {
			CUBE_URL = new URL("http://testcube.cube"); // meaningless URL for fun!
		} catch (Exception e) {
			assert(false);
		}

		CUBE_KEY = new MeshProtoKey(CUBE_URL);
	}

	public CubeObserver(Entity observee) {
		super(observee);
	}

	/**
	 * Build a render proxy from this entity
	 * @return
	 */
	@Override
	public void collectProxies(ArrayList<RenderProxy> out) {
		if (_dispObservee == null) {
			// We're not looking at anything
			return;
		}
		double simTime = _dispObservee.getCurrentTime();
		Transform trans = _dispObservee.getGlobalTrans(simTime);
		Vector4d scale = _dispObservee.getJaamMathSize();

		if (trans == null || scale == null) {
			return;
		}

		out.add( new MeshProxy(CUBE_KEY, trans, scale, _observee.getEntityNumber()) );
	}

}
