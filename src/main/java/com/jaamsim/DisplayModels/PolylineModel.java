/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017-2019 JaamSim Software Inc.
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
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.FillEntity;
import com.jaamsim.Graphics.LineEntity;
import com.jaamsim.Graphics.PolylineInfo;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.LineProxy;
import com.jaamsim.render.PointProxy;
import com.jaamsim.render.PolygonProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.VisibilityInfo;
import com.jaamsim.units.DistanceUnit;

public class PolylineModel extends DisplayModel implements LineEntity, FillEntity {

	@Keyword(description = "The colour of the polyline.",
	         exampleList = {"red"})
	private final ColourInput colour;

	@Keyword(description = "The width of the polyline in pixels.",
	         exampleList = {"1"})
	private final IntegerInput width;

	@Keyword(description = "Determines whether the polyline is filled or hollow. "
	                     + "If TRUE, then the polyline has a solid fill.",
	         exampleList = { "FALSE" })
	private final BooleanInput filled;

	@Keyword(description = "The colour for the filled part of the polyline.",
	         exampleList = { "red" })
	private final ColourInput fillColour;

	@Keyword(description = "If TRUE, an arrow head is displayed at the end of the polyline.",
	         exampleList = {"TRUE", "FALSE"})
	protected final BooleanInput showArrowHead;

	@Keyword(description = "A set of (x, y, z) numbers that define the size of the arrowhead.",
	         exampleList = { "0.165 0.130 0.0 m" })
	protected final Vec3dInput arrowHeadSize;

	{
		colour = new ColourInput("LineColour", FORMAT, ColourInput.BLACK);
		this.addInput(colour);
		this.addSynonym(colour, "Color");
		this.addSynonym(colour, "Colour");

		width = new IntegerInput("LineWidth", FORMAT, 1);
		width.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(width);
		this.addSynonym(width, "Width");

		filled = new BooleanInput("Filled", FORMAT, false);
		this.addInput(filled);

		fillColour = new ColourInput("FillColour", FORMAT, ColourInput.MED_GREY);
		this.addInput(fillColour);
		this.addSynonym(fillColour, "FillColor");

		showArrowHead = new BooleanInput("ShowArrowHead", FORMAT, false);
		this.addInput(showArrowHead);

		arrowHeadSize = new Vec3dInput("ArrowHeadSize", FORMAT, new Vec3d(0.1d, 0.1d, 0.0d));
		arrowHeadSize.setUnitType(DistanceUnit.class);
		this.addInput(arrowHeadSize);
		this.addSynonym(arrowHeadSize, "ArrowSize");
	}

	public PolylineModel() {}

	private static final Color4d MINT = ColourInput.getColorWithName("mint");
	protected static List<Vec4d> arrowHeadVerts;

	static {
		arrowHeadVerts = new ArrayList<>(3);
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
		return (ent instanceof DisplayEntity);
	}

	@Override
	public boolean isOutlined() {
		return true;
	}

	@Override
	public Color4d getLineColour() {
		return colour.getValue();
	}

	@Override
	public int getLineWidth() {
		return Math.max(1, width.getValue());
	}

	@Override
	public boolean isFilled() {
		return filled.getValue();
	}

	@Override
	public Color4d getFillColour() {
		return fillColour.getValue();
	}

	public boolean getShowArrowHead() {
		return showArrowHead.getValue();
	}

	public Vec3d getArrowHeadSize() {
		return arrowHeadSize.getValue();
	}

	protected class Binding extends DisplayModelBinding {

		private Arrow arrowObservee;
		private LineEntity lineEnt;
		private FillEntity fillEnt;

		private ArrayList<Vec4d> headPoints = null;
		private Vec3d arrowSizeCache;
		private int lineWidthCache;
		private Color4d lineColourCache;
		private boolean filledCache;
		private Color4d fillColourCache;
		private boolean showArrowHeadCache;

		private Transform globalTransCache;

		private DisplayEntity displayObservee;

		private PolylineInfo[] pisCache;
		private VisibilityInfo viCache;

		private ArrayList<Vec4d> selectionPoints = null;
		private ArrayList<Vec4d> nodePoints = null;
		private ArrayList<RenderProxy> cachedProxies = null;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);

			try {
				displayObservee = (DisplayEntity)ent;
			} catch (ClassCastException e) {
				// The observee is not a display entity
				displayObservee = null;
			}
			if (observee instanceof Arrow)
				arrowObservee = (Arrow)observee;
			if (observee instanceof LineEntity)
				lineEnt = (LineEntity) observee;
			if (ent instanceof FillEntity)
				fillEnt = (FillEntity) ent;
		}

		/**
		 * Update the cached Points list
		 */
		protected void updateProxies(double simTime) {

			PolylineInfo[] pis = displayObservee.getScreenPoints(simTime);
			if (pis == null || pis.length == 0)
				return;

			Transform globalTrans = null;
			if (displayObservee.getCurrentRegion() != null || displayObservee.getRelativeEntity() != null) {
				globalTrans = displayObservee.getGlobalPositionTransform();
			}

			Color4d lineColour = lineEnt == null ? getLineColour() : lineEnt.getLineColour();
			int lineWidth = lineEnt == null ? getLineWidth() : lineEnt.getLineWidth();

			boolean fill = fillEnt == null ? filled.getValue() : fillEnt.isFilled();
			Color4d fc = fillEnt == null ? fillColour.getValue() : fillEnt.getFillColour();

			Vec3d arrowSize = getArrowHeadSize();
			if (arrowObservee != null)
				arrowSize = arrowObservee.getArrowHeadSize();

			VisibilityInfo vi = getVisibilityInfo();

			boolean dirty = false;

			dirty = dirty || !compareArray(pisCache, pis);
			dirty = dirty || lineWidthCache != lineWidth;
			dirty = dirty || dirty_col4d(lineColourCache, lineColour);
			dirty = dirty || filledCache != fill;
			dirty = dirty || fillColourCache != fc;
			dirty = dirty || showArrowHeadCache != getShowArrowHead();
			dirty = dirty || dirty_vec3d(arrowSizeCache, arrowSize);
			dirty = dirty || !compare(globalTransCache, globalTrans);
			dirty = dirty || !compare(viCache, vi);

			pisCache = pis;
			lineWidthCache = lineWidth;
			lineColourCache = lineColour;
			filledCache = fill;
			fillColourCache = fc;
			showArrowHeadCache = getShowArrowHead();
			arrowSizeCache = arrowSize;
			globalTransCache = globalTrans;
			viCache = vi;

			if (cachedProxies != null && !dirty) {
				// up to date
				registerCacheHit("Points");
				return;
			}

			registerCacheMiss("Points");

			selectionPoints = new ArrayList<>();
			nodePoints = new ArrayList<>();

			// Cache the points in the first series for selection and editing
			ArrayList<Vec3d> basePoints = displayObservee.getPoints();
			if (basePoints == null) {
				basePoints = new ArrayList<>();
			}

			for (int i = 1; i < basePoints.size(); ++i) { // Skip the first point
				Vec3d start = basePoints.get(i - 1);
				Vec3d end = basePoints.get(i);

				selectionPoints.add(new Vec4d(start.x, start.y, start.z, 1.0d));
				selectionPoints.add(new Vec4d(end.x, end.y, end.z, 1.0d));
			}

			for (int i = 0; i < basePoints.size(); ++i) {
				// Save the point list as is for control nodes
				Vec3d p = basePoints.get(i);
				nodePoints.add(new Vec4d(p.x, p.y, p.z, 1.0d));
			}

			if (globalTrans != null) {
				RenderUtils.transformPointsLocal(globalTrans, selectionPoints, 0);
				RenderUtils.transformPointsLocal(globalTrans, nodePoints, 0);
			}

			// Add the line proxies
			cachedProxies = new ArrayList<>(pis.length + 1);

			for (PolylineInfo pi : pis) {
				List<Vec4d> points = new ArrayList<>();

				ArrayList<Vec3d> curvePoints = pi.getCurvePoints();
				for (int i = 1; i < curvePoints.size(); ++i) { // Skip the first point
					Vec3d start = curvePoints.get(i - 1);
					Vec3d end = curvePoints.get(i);

					points.add(new Vec4d(start.x, start.y, start.z, 1.0d));
					points.add(new Vec4d(end.x, end.y, end.z, 1.0d));
				}

				if (globalTrans != null) {
					RenderUtils.transformPointsLocal(globalTrans, points, 0);
				}

				int wid = pi.getWidth();
				if (wid == -1)
					wid = lineWidth;

				Color4d col = pi.getColor();
				if (col == null)
					col = lineColour;

				cachedProxies.add(new LineProxy(points, col, wid, vi, displayObservee.getEntityNumber()));

				if (fill) {
					Vec3d scale = new Vec3d(1.0d, 1.0d, 1.0d);
					Transform trans = new Transform();
					cachedProxies.add(new PolygonProxy(points, trans, scale, fc, false, 1, getVisibilityInfo(), displayObservee.getEntityNumber()));
				}
			}

			// Add the arrowhead
			if (!getShowArrowHead())
				return;

			ArrayList<Vec3d> curvePts = pis[0].getCurvePoints();
			Vec3d startPoint = curvePts.get(curvePts.size() - 1);
			Vec3d fromPoint = curvePts.get(curvePts.size() - 2);

			Color4d color = pis[0].getColor();
			if (color == null)
				color = lineColour;

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

			cachedProxies.add(new PolygonProxy(headPoints, Transform.ident, DisplayModel.ONES, color,
			        false, 1, getVisibilityInfo(), observee.getEntityNumber()));
		}

		@Override
		public void collectProxies(double simTime, ArrayList<RenderProxy> out) {

			if (displayObservee == null || !displayObservee.getShow()) {
				return;
			}

			updateProxies(simTime);

			if (cachedProxies == null)
				return;

			out.addAll(cachedProxies);
		}

		@Override
		public void collectSelectionProxies(double simTime, ArrayList<RenderProxy> out) {

			if (displayObservee == null ||
			    !displayObservee.getShow() ||
			    !displayObservee.selectable()) {
				return;
			}

			updateProxies(simTime);

			if (selectionPoints.size() == 0) {
				return;
			}

			LineProxy lp = new LineProxy(selectionPoints, MINT, 2, getVisibilityInfo(), RenderManager.LINEDRAG_PICK_ID);
			lp.setHoverColour(ColourInput.LIGHT_GREY);
			out.add(lp);

			for (int i = 0; i < nodePoints.size(); ++i) {

				Color4d col = ColourInput.GREEN;
				if (i == 0)
					col = ColourInput.BLUE;
				if (i == nodePoints.size() -1)
					col = ColourInput.YELLOW;

				addPoint(nodePoints.get(i), col, ColourInput.LIGHT_GREY, RenderManager.LINENODE_PICK_ID - i, out);
			}
		}
	}

	private void addPoint(Vec4d p, Color4d col, Color4d hovCol, long pickID, ArrayList<RenderProxy> out) {
		List<Vec4d> pl = new ArrayList<>(1);

		pl.add(new Vec4d(p));
		PointProxy pp = new PointProxy(pl, col, 8, getVisibilityInfo(), pickID);
		pp.setHoverColour(hovCol);
		pp.setCollisionAngle(0.004363); // 0.25 degrees in radians

		out.add(pp);
	}

}
