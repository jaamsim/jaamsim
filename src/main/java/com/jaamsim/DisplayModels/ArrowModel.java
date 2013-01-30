/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.DisplayModels;

import java.util.ArrayList;
import java.util.List;

import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.PolygonProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.Arrow;

public class ArrowModel extends ScreenPointsModel {
	private static List<Vec4d> arrowHeadVerts;

	static {
		arrowHeadVerts = new ArrayList<Vec4d>(3);
		arrowHeadVerts.add(new Vec4d(0.0,  0.0, 0.0, 1.0d));
		arrowHeadVerts.add(new Vec4d(1.0, -0.5, 0.0, 1.0d));
		arrowHeadVerts.add(new Vec4d(1.0,  0.5, 0.0, 1.0d));
	}

	@Override
	public DisplayModelBinding getBinding(Entity ent) {
		return new Binding(ent, this);
	}

	@Override
	public boolean canDisplayEntity(Entity ent) {
		return (ent instanceof Arrow);
	}

	private class Binding extends ScreenPointsModel.Binding {

		private Arrow arrowObservee;
		private ChangeWatcher.Tracker observeeTracker;

		private List<Vec4d> headPoints = null;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			try {
				arrowObservee = (Arrow)observee;
				if (arrowObservee != null) {
					observeeTracker = arrowObservee.getGraphicsChangeTracker();
				}
			} catch (ClassCastException e) {
				// The observee is not a display entity
				arrowObservee = null;
			}
		}

		private void updateHead() {
			if (headPoints != null && observeeTracker != null && !observeeTracker.checkAndClear()) {
				// up to date
				return;
			}

			// Draw an arrow head at the last two points
			if (points.size() < 2) {
				return;
			}

			Vec4d startPoint = points.get(points.size() - 1);

			// dir is the direction of the arrow
			Vec4d fromPoint = points.get(points.size() - 2);

			Vec4d dir = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
			dir.sub3(fromPoint, startPoint);
			dir.normalize3();

			double theta = Math.atan2(dir.y, dir.x);

			Vec3d scale = arrowObservee.getArrowHeadSize();

			Mat4d trans = new Mat4d();
			trans.setEuler3(new Vec3d(0, 0, theta));
			trans.scaleCols3(scale);
			trans.setTranslate3(startPoint);

			headPoints = RenderUtils.transformPoints(trans, arrowHeadVerts, 0);
		}

		@Override
		public void collectProxies(ArrayList<RenderProxy> out) {

			if (arrowObservee == null || !arrowObservee.getShow()) {
				return;
			}

			updatePoints();
			updateHead();

			super.collectProxies(out);

			out.add(new PolygonProxy(headPoints, Transform.ident, Vec4d.ONES,
			        screenPointObservee.getDisplayColour(),
			        false, 1, getVisibilityInfo(), arrowObservee.getEntityNumber()));
		}
	}
}
