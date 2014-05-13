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

import com.jaamsim.input.Keyword;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.HasScreenPoints;
import com.jaamsim.render.PolygonProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.units.DistanceUnit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Vec3dInput;
import com.sandwell.JavaSimulation3D.Arrow;

public class ArrowModel extends ScreenPointsModel {
	@Keyword(description = "A set of { x, y, z } numbers that define the size of the arrowhead " +
	        "in those directions at the end of the connector.",
	 example = "Arrow1 ArrowSize { 0.165 0.130 0.0 m }")
	private final Vec3dInput arrowHeadSize;

	private static List<Vec4d> arrowHeadVerts;

	static {
		arrowHeadVerts = new ArrayList<Vec4d>(3);
		arrowHeadVerts.add(new Vec4d(0.0,  0.0, 0.0, 1.0d));
		arrowHeadVerts.add(new Vec4d(1.0, -0.5, 0.0, 1.0d));
		arrowHeadVerts.add(new Vec4d(1.0,  0.5, 0.0, 1.0d));
	}

	{
		arrowHeadSize = new Vec3dInput("ArrowSize", "Basic Graphics", new Vec3d(0.1d, 0.1d, 0.0d));
		arrowHeadSize.setUnitType(DistanceUnit.class);
		this.addInput(arrowHeadSize);
	}

	@Override
	public DisplayModelBinding getBinding(Entity ent) {
		return new Binding(ent, this);
	}

	private class Binding extends ScreenPointsModel.Binding {

		private Arrow arrowObservee;

		private ArrayList<Vec4d> headPoints = null;

		private RenderProxy cachedProxy = null;

		private Vec4d startCache;
		private Vec4d fromCache;
		private Color4d colorCache;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			if (observee instanceof Arrow)
				arrowObservee = (Arrow)observee;
		}

		private void updateHead() {

			Vec4d startPoint = selectionPoints.get(selectionPoints.size() - 1);
			Vec4d fromPoint = selectionPoints.get(selectionPoints.size() - 2);
			HasScreenPoints.PointsInfo[] pointInfos = screenPointObservee.getScreenPoints();
			if (pointInfos == null || pointInfos.length == 0)
				return;

			Color4d color = pointInfos[0].color;

			boolean dirty = false;

			dirty = dirty || dirty_vec4d(startCache, startPoint);
			dirty = dirty || dirty_vec4d(fromCache, fromPoint);
			dirty = dirty || dirty_col4d(colorCache, color);

			startCache = startPoint;
			fromCache = fromPoint;
			colorCache = color;

			if (cachedProxy != null && !dirty) {
				// up to date
				return;
			}

			// Draw an arrow head at the last two points
			if (selectionPoints.size() < 2) {
				return;
			}

			// Calculate a z-rotation in the XY-plane
			Vec3d zRot = new Vec3d();
			zRot.sub3(fromPoint, startPoint);
			zRot.set3(0.0d, 0.0d, Math.atan2(zRot.y, zRot.x));

			Mat4d trans = new Mat4d();
			trans.setEuler3(zRot);
			if (arrowObservee != null)
				trans.scaleCols3(arrowObservee.getArrowHeadSize());
			else
				trans.scaleCols3(arrowHeadSize.getValue());
			trans.setTranslate3(startPoint);

			headPoints = new ArrayList<Vec4d>(arrowHeadVerts.size());
			for (Vec4d v : arrowHeadVerts) {
				Vec4d tmp = new Vec4d();
				tmp.mult4(trans, v);
				headPoints.add(tmp);
			}

			cachedProxy = new PolygonProxy(headPoints, Transform.ident, DisplayModel.ONES, color,
			        false, 1, getVisibilityInfo(), observee.getEntityNumber());
		}

		@Override
		public void collectProxies(double simTime, ArrayList<RenderProxy> out) {

			if (displayObservee == null || !displayObservee.getShow()) {
				return;
			}

			updateProxies(simTime);
			updateHead();

			super.collectProxies(simTime, out);

			out.add(cachedProxy);
		}
	}
}
