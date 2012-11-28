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

import java.util.ArrayList;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;
import com.jaamsim.render.PolygonProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.sandwell.JavaSimulation.Entity;

public class BoxObserver extends RenderObserver {

	private Color4d _colour;


	BoxObserver(Entity observee, Color4d colour) {
		super(observee);
		_colour = colour;
	}


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

		out.add(new PolygonProxy(RenderUtils.RECT_POINTS, trans, scale,
		                         _colour, true, 1, _observee.getEntityNumber()));

		//out.add(new BoxProxy(trans, scale, _colour, _observee.getEntityNumber()));

	}

}
