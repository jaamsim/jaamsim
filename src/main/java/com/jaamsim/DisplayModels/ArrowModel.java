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
		private ChangeWatcher.Tracker dmTracker;


		private ArrayList<Vec4d> headPoints = null;

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
			dmTracker = dm.getGraphicsChangeTracker();
		}

		private void updateHead() {
			if (headPoints != null && observeeTracker != null && !observeeTracker.checkAndClear()
			    && !dmTracker.checkAndClear()) {
				// up to date
				return;
			}

			// Draw an arrow head at the last two points
			if (points.size() < 2) {
				return;
			}

			Vec4d startPoint = points.get(points.size() - 1);
			Vec4d fromPoint = points.get(points.size() - 2);

			// Calculate a z-rotation in the XY-plane
			Vec3d zRot = new Vec3d();
			zRot.sub3(fromPoint, startPoint);
			zRot.set3(0.0d, 0.0d, Math.atan2(zRot.y, zRot.x));

			Mat4d trans = new Mat4d();
			trans.setEuler3(zRot);
			trans.scaleCols3(arrowObservee.getArrowHeadSize());
			trans.setTranslate3(startPoint);

			headPoints = new ArrayList<Vec4d>(arrowHeadVerts.size());
			for (Vec4d v : arrowHeadVerts) {
				Vec4d tmp = new Vec4d();
				tmp.mult4(trans, v);
				headPoints.add(tmp);
			}
		}

		@Override
		public void collectProxies(double simTime, ArrayList<RenderProxy> out) {

			if (arrowObservee == null || !arrowObservee.getShow()) {
				return;
			}

			updatePoints(simTime);
			updateHead();

			super.collectProxies(simTime, out);

			out.add(new PolygonProxy(headPoints, Transform.ident, Vec4d.ONES,
			        screenPointObservee.getDisplayColour(),
			        false, 1, getVisibilityInfo(), arrowObservee.getEntityNumber()));
		}
	}
}
