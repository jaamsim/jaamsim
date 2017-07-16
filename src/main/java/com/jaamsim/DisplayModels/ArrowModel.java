/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.DisplayModels;

import java.util.ArrayList;
import java.util.List;

import com.jaamsim.Graphics.Arrow;
import com.jaamsim.Graphics.PolylineInfo;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.PolygonProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.units.DistanceUnit;

public class ArrowModel extends PolylineModel {
	@Keyword(description = "A set of { x, y, z } numbers that define the size of the arrowhead "
	                     + "in those directions at the end of the connector.",
	         exampleList = "Arrow1 ArrowSize { 0.165 0.130 0.0 m }")
	private final Vec3dInput arrowHeadSize;

	private static List<Vec4d> arrowHeadVerts;

	static {
		arrowHeadVerts = new ArrayList<>(3);
		arrowHeadVerts.add(new Vec4d(0.0,  0.0, 0.0, 1.0d));
		arrowHeadVerts.add(new Vec4d(1.0, -0.5, 0.0, 1.0d));
		arrowHeadVerts.add(new Vec4d(1.0,  0.5, 0.0, 1.0d));
	}

	{
		arrowHeadSize = new Vec3dInput("ArrowSize", "Key Inputs", new Vec3d(0.1d, 0.1d, 0.0d));
		arrowHeadSize.setUnitType(DistanceUnit.class);
		this.addInput(arrowHeadSize);
		arrowHeadSize.setHidden(true);
	}

	@Override
	public DisplayModelBinding getBinding(Entity ent) {
		return new Binding(ent, this);
	}

	private class Binding extends PolylineModel.Binding {

		private Arrow arrowObservee;

		private ArrayList<Vec4d> headPoints = null;

		private RenderProxy cachedProxy = null;

		private Vec3d startCache;
		private Vec3d fromCache;
		private Color4d colorCache;
		private Vec3d arrowSizeCache;
		private Transform globalTransCache;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			if (observee instanceof Arrow)
				arrowObservee = (Arrow)observee;
		}

		private void updateHead(double simTime) {

			PolylineInfo[] pointInfos = displayObservee.getScreenPoints(simTime);

			if (pointInfos == null || pointInfos.length == 0)
				return;

			Transform globalTrans = null;
			if (displayObservee.getCurrentRegion() != null || displayObservee.getRelativeEntity() != null) {
				globalTrans = displayObservee.getGlobalPositionTransform();
			}

			ArrayList<Vec3d> curvePoints = pointInfos[0].getCurvePoints();
			Vec3d startPoint = curvePoints.get(curvePoints.size() - 1);
			Vec3d fromPoint = curvePoints.get(curvePoints.size() - 2);

			Color4d color = pointInfos[0].getColor();

			boolean dirty = false;

			Vec3d arrowSize;
			if (arrowObservee != null)
				arrowSize = arrowObservee.getArrowHeadSize();
			else
				arrowSize = arrowHeadSize.getValue();

			dirty = dirty || dirty_vec3d(startCache, startPoint);
			dirty = dirty || dirty_vec3d(fromCache, fromPoint);
			dirty = dirty || dirty_col4d(colorCache, color);
			dirty = dirty || dirty_vec3d(arrowSizeCache, arrowSize);
			dirty = dirty || !compare(globalTransCache, globalTrans);

			startCache = startPoint;
			fromCache = fromPoint;
			colorCache = color;
			arrowSizeCache = arrowSize;
			globalTransCache = globalTrans;

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
			trans.scaleCols3(arrowSize);
			trans.setTranslate3(startPoint);

			headPoints = new ArrayList<>(arrowHeadVerts.size());
			for (Vec4d v : arrowHeadVerts) {
				Vec4d tmp = new Vec4d();
				tmp.mult4(trans, v);
				headPoints.add(tmp);
			}

			if (globalTrans != null) {
				RenderUtils.transformPointsLocal(globalTrans, headPoints, 0);
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
			updateHead(simTime);

			super.collectProxies(simTime, out);

			out.add(cachedProxy);
		}
	}
}
