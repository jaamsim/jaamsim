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
import java.util.List;

import com.jaamsim.math.Matrix4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;
import com.jaamsim.render.PolygonProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.Arrow;

public class ArrowObserver extends ScreenPointsObserver {

	private static List<Vector4d> arrowHeadVerts;

	static {
		arrowHeadVerts = new ArrayList<Vector4d>(3);
		arrowHeadVerts.add(new Vector4d(0.0,  0.0, 0.0));
		arrowHeadVerts.add(new Vector4d(1.0, -0.5, 0.0));
		arrowHeadVerts.add(new Vector4d(1.0,  0.5, 0.0));
	}

	private Arrow _arrowObservee;
	private ChangeWatcher.Tracker _observeeTracker;

	private List<Vector4d> headPoints = null;

	ArrowObserver(Entity observee) {
		super(observee);

		try {
			_arrowObservee = (Arrow)observee;
			_observeeTracker = _arrowObservee.getGraphicsChangeTracker();
		} catch (ClassCastException e) {
			// The observee is not a display entity
			_arrowObservee = null;
			// Debug assert, not actually an error
			assert(false);
		}
	}

	private void updateHead() {
		if (headPoints != null && !_observeeTracker.checkAndClear()) {
			// up to date
			return;
		}

		// Draw an arrow head at the last two points
		if (points.size() < 2) {
			return;
		}

		Vector4d startPoint = points.get(points.size() - 1);

		// dir is the direction of the arrow
		Vector4d fromPoint = points.get(points.size() - 2);

		Vector4d dir = new Vector4d();
		fromPoint.sub3(startPoint, dir);
		dir.normalizeLocal3();

		double theta = Math.atan2(dir.y(), dir.x());

		Vector4d scale = _arrowObservee.getArrowHeadSize();

		Matrix4d trans = Matrix4d.ScaleMatrix(scale);
		Matrix4d.RotationMatrix(theta, Vector4d.Z_AXIS).mult(trans, trans);

		Matrix4d.TranslationMatrix(startPoint).mult(trans, trans);

		headPoints = RenderUtils.transformPoints(trans, arrowHeadVerts);

	}

	@Override
	public void collectProxies(ArrayList<RenderProxy> out) {


		updatePoints();
		updateHead();

		super.collectProxies(out);

		out.add(new PolygonProxy(headPoints, Transform.ident, Vector4d.ONES,
		        _screenPointObservee.getDisplayColour(),
		        false, 1, _arrowObservee.getEntityNumber()));


	}


}
