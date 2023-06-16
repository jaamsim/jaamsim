/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017-2023 JaamSim Software Inc.
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
import com.jaamsim.Graphics.PolylineEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.MathUtils;
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

public class PolylineModel extends AbstractShapeModel implements PolylineEntity {

	@Keyword(description = "Determines whether or not to show a line between the last point of "
	                     + "the polyline and its first point. "
	                     + "If TRUE, the closing line is displayed. "
	                     + "If FALSE, the closing line is not displayed.",
	         exampleList = {"TRUE", "FALSE"})
	protected final BooleanInput closed;

	@Keyword(description = "Physical width of the polyline with units of distance.",
	         exampleList = { "0.5 m" })
	protected final ValueInput polylineWidth;

	@Keyword(description = "If TRUE, an arrow head is displayed at the end of the polyline.",
	         exampleList = {"TRUE", "FALSE"})
	protected final BooleanInput showArrowHead;

	@Keyword(description = "A set of (x, y, z) numbers that define the size of the arrowhead.",
	         exampleList = { "0.165 0.130 0.0 m" })
	protected final Vec3dInput arrowHeadSize;

	{
		outlined.setDefaultValue(true);

		this.addSynonym(lineColour, "Color");
		this.addSynonym(lineColour, "Colour");

		this.addSynonym(lineWidth, "Width");

		this.addSynonym(fillColour, "FillColor");

		polylineWidth = new ValueInput("PolylineWidth", KEY_INPUTS, 0.0d);
		polylineWidth.setUnitType(DistanceUnit.class);
		polylineWidth.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(polylineWidth);

		closed = new BooleanInput("Closed", KEY_INPUTS, false);
		this.addInput(closed);

		showArrowHead = new BooleanInput("ShowArrowHead", KEY_INPUTS, false);
		this.addInput(showArrowHead);

		arrowHeadSize = new Vec3dInput("ArrowHeadSize", KEY_INPUTS, new Vec3d(0.1d, 0.1d, 0.0d));
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
	public boolean isClosed() {
		return closed.getValue();
	}

	@Override
	public double getPolylineWidth() {
		return polylineWidth.getValue();
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
		private PolylineEntity polylineEnt;

		private ArrayList<Vec4d> headPoints = null;
		private Vec3d arrowSizeCache;
		private int lineWidthCache;
		private Color4d lineColourCache;
		private boolean outlinedCache;
		private boolean filledCache;
		private Color4d fillColourCache;
		private boolean showArrowHeadCache;
		private boolean closedCache;
		private double widthCache;

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
			if (ent instanceof PolylineEntity)
				polylineEnt = (PolylineEntity) ent;
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

			boolean outln = lineEnt == null ? outlined.getValue() : lineEnt.isOutlined();
			Color4d lineColour = lineEnt == null ? getLineColour() : lineEnt.getLineColour();
			int lineWidth = lineEnt == null ? getLineWidth() : lineEnt.getLineWidth();

			boolean fill = fillEnt == null ? filled.getValue() : fillEnt.isFilled();
			Color4d fc = fillEnt == null ? fillColour.getValue() : fillEnt.getFillColour();

			boolean closed = polylineEnt == null ? isClosed() : polylineEnt.isClosed();
			double width = polylineEnt == null ? getPolylineWidth() : polylineEnt.getPolylineWidth();

			Vec3d arrowSize = getArrowHeadSize();
			if (arrowObservee != null)
				arrowSize = arrowObservee.getArrowHeadSize();

			VisibilityInfo vi = getVisibilityInfo();

			boolean dirty = false;

			dirty = dirty || !compareArray(pisCache, pis);
			dirty = dirty || lineWidthCache != lineWidth;
			dirty = dirty || dirty_col4d(lineColourCache, lineColour);
			dirty = dirty || outlinedCache != outln;
			dirty = dirty || filledCache != fill;
			dirty = dirty || fillColourCache != fc;
			dirty = dirty || closedCache != closed;
			dirty = dirty || widthCache != width;
			dirty = dirty || showArrowHeadCache != getShowArrowHead();
			dirty = dirty || dirty_vec3d(arrowSizeCache, arrowSize);
			dirty = dirty || !compare(globalTransCache, globalTrans);
			dirty = dirty || !compare(viCache, vi);

			pisCache = pis;
			lineWidthCache = lineWidth;
			lineColourCache = lineColour;
			outlinedCache = outln;
			filledCache = fill;
			fillColourCache = fc;
			closedCache = closed;
			widthCache = width;
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

			if (closed) {
				Vec3d start = basePoints.get(basePoints.size() - 1);
				Vec3d end = basePoints.get(0);
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

			cachedProxies = new ArrayList<>();

			// Add wide-polyline proxies
			if (width > 0.0d)
				addWidePolylineProxies();

			// Add the line proxies
			addLineAndFillProxies();

			// Add the arrowhead proxies
			if (showArrowHeadCache)
				addArrowHeadProxies();
		}

		private void addLineAndFillProxies() {
			long id = displayObservee.getEntityNumber();

			for (PolylineInfo pi : pisCache) {
				List<Vec4d> points = new ArrayList<>();

				ArrayList<Vec3d> curvePoints = new ArrayList<>(pi.getCurvePoints());
				if (closedCache)
					curvePoints.add(curvePoints.get(0));

				for (int i = 1; i < curvePoints.size(); ++i) { // Skip the first point
					Vec3d start = curvePoints.get(i - 1);
					Vec3d end = curvePoints.get(i);
					points.add(new Vec4d(start.x, start.y, start.z, 1.0d));
					points.add(new Vec4d(end.x, end.y, end.z, 1.0d));
				}

				if (globalTransCache != null) {
					RenderUtils.transformPointsLocal(globalTransCache, points, 0);
				}

				// Draw the outline
				if (outlinedCache) {
					int wid = pi.getWidth();
					if (wid == -1)
						wid = lineWidthCache;

					Color4d col = pi.getColor();
					if (col == null)
						col = lineColourCache;
					cachedProxies.add(new LineProxy(points, col, wid, viCache, id));
				}

				// Draw the fill
				if (filledCache) {
					cachedProxies.add(new PolygonProxy(points, Transform.ident, DisplayModel.ONES,
							fillColourCache, false, 1, getVisibilityInfo(), id));
				}
			}
		}

		private void addArrowHeadProxies() {
			long id = displayObservee.getEntityNumber();

			ArrayList<Vec3d> curvePts = pisCache[0].getCurvePoints();
			if (curvePts.size() < 2)
				return;
			Vec3d startPoint = curvePts.get(curvePts.size() - 1);
			Vec3d fromPoint = curvePts.get(curvePts.size() - 2);

			Color4d color = pisCache[0].getColor();
			if (color == null)
				color = lineColourCache;

			// Calculate the Euler angles for the arrowhead
			Vec3d zRot = new Vec3d();
			zRot.sub3(fromPoint, startPoint);
			zRot.normalize3();
			zRot.set3(0.0d, Math.asin(-zRot.z), Math.atan2(zRot.y, zRot.x));

			Mat4d trans = new Mat4d();
			trans.setEuler3(zRot);
			trans.scaleCols3(arrowSizeCache);
			trans.setTranslate3(startPoint);

			headPoints = new ArrayList<>(arrowHeadVerts.size());
			for (Vec4d v : arrowHeadVerts) {
				Vec4d tmp = new Vec4d();
				tmp.mult4(trans, v);
				headPoints.add(tmp);
			}

			if (globalTransCache != null) {
				RenderUtils.transformPointsLocal(globalTransCache, headPoints, 0);
			}

			cachedProxies.add(new PolygonProxy(headPoints, Transform.ident, DisplayModel.ONES,
					color, false, 1, viCache, id));
		}

		private void addWidePolylineProxies() {
			double halfWidth = 0.5d * widthCache;
			Vec3d zDir = new Vec3d(0.0d, 0.0d, 1.0d);
			long id = displayObservee.getEntityNumber();
			Vec4d lastPoint1 = null;
			Vec4d lastPoint2 = null;

			for (PolylineInfo pi : pisCache) {
				ArrayList<Vec3d> curvePoints = new ArrayList<>(pi.getCurvePoints());
				if (closedCache)
					curvePoints.add(curvePoints.get(0));

				List<Vec4d> points1 = new ArrayList<>();  // points on side 1
				List<Vec4d> points2 = new ArrayList<>();  // points on side 2
				for (int i = 0; i < curvePoints.size(); ++i) {
					Vec3d pt = curvePoints.get(i);

					// Vector towards the point
					Vec3d vecIn = null;
					if (i > 0) {
						vecIn = new Vec3d(pt);
						vecIn.sub3(curvePoints.get(i - 1));
					}
					else if (i == 0 && closedCache) {
						vecIn = new Vec3d(pt);
						vecIn.sub3(curvePoints.get(curvePoints.size() - 2));
					}
					if (vecIn != null && MathUtils.isSmall(vecIn.mag3()))
						vecIn = null;

					// Vector away from the point
					Vec3d vecOut = null;
					if (i + 1 < curvePoints.size()) {
						vecOut = new Vec3d(curvePoints.get(i + 1));
						vecOut.sub3(pt);
					}
					else if (i == curvePoints.size() - 1 && closedCache) {
						vecOut = new Vec3d(curvePoints.get(1));
						vecOut.sub3(pt);
					}
					if (vecOut != null && MathUtils.isSmall(vecOut.mag3()))
						vecOut = null;
					//System.out.format("vecIn=%s, vecOut=%s%n", vecIn, vecOut);

					if (vecIn == null && vecOut == null)
						continue;

					// Normal vectors
					Vec3d nIn = null;  // normal to vecIn in the common plane
					Vec3d nOut = null;  // normal to vecOut in the common plane
					if (vecIn != null && vecOut != null) {
						Vec3d normal = new Vec3d();  // normal to vecIn and vecOut
						normal.cross3(vecIn, vecOut);
						if (MathUtils.isSmall(normal.mag3()/(vecIn.mag3()*vecOut.mag3())))
							normal = zDir;
						nIn = new Vec3d();
						nIn.cross3(vecIn, normal);
						nOut = new Vec3d();
						nOut.cross3(vecOut, normal);
						//System.out.format("normal=%s%n", normal);
					}
					//System.out.format("nIn=%s, nOut=%s%n", nIn, nOut);

					// Horizontal vectors that are perpendicular to vecIn and vecOut
					Vec3d deltaIn = null;
					if (vecIn != null) {
						deltaIn = new Vec3d();
						deltaIn.cross3(vecIn, zDir);
						deltaIn.normalize3();
						deltaIn.scale3(halfWidth);
					}
					Vec3d deltaOut = null;
					if (vecOut != null) {
						deltaOut = new Vec3d();
						deltaOut.cross3(vecOut, zDir);
						deltaOut.normalize3();
						deltaOut.scale3(halfWidth);
					}
					//System.out.format("deltaIn=%s, deltaOut=%s%n", deltaIn, deltaOut);

					// Side 1
					Vec3d pIn = null;
					if (vecIn != null) {
						pIn = new Vec3d(pt);
						pIn.add3(deltaIn);
					}
					Vec3d pOut = null;
					if (vecOut != null) {
						pOut = new Vec3d(pt);
						pOut.add3(deltaOut);
					}

					if (pIn == null) {
						points1.add(new Vec4d(pOut, 1.0d));
					}
					else if (pOut == null) {
						points1.add(new Vec4d(pIn, 1.0d));
					}

					else {
						// Intersection points
						Vec3d diff = new Vec3d(pOut);
						diff.sub3(pIn);
						Vec3d delta = new Vec3d(vecIn);
						delta.scale3(diff.dot3(nOut)/vecIn.dot3(nOut));
						if (delta.mag3()/halfWidth < 4.0d) {
							// Midpoint between the closest points along vecIn and vecOut
							Vec3d p0 = new Vec3d(pIn);
							p0.add3(delta);

							delta = new Vec3d(vecOut);
							delta.scale3(-diff.dot3(nIn)/vecOut.dot3(nIn));
							p0.add3(pOut);
							p0.add3(delta);
							p0.scale3(0.5d);
							points1.add(new Vec4d(p0, 1.0d));
						}
						else {
							points1.add(new Vec4d(pIn, 1.0d));
							points1.add(new Vec4d(pOut, 1.0d));
							//System.out.format("delta=%s, pIn=%s, pOut=%s%n", delta, pIn, pOut);
						}
					}

					// Side2
					pIn = null;
					if (deltaIn != null) {
						pIn = new Vec3d(pt);
						pIn.sub3(deltaIn);
					}
					pOut = null;
					if (deltaOut != null) {
						pOut = new Vec3d(pt);
						pOut.sub3(deltaOut);
					}

					if (pIn == null) {
						points2.add(0, new Vec4d(pOut, 1.0d));
					}
					else if (pOut == null) {
						points2.add(0, new Vec4d(pIn, 1.0d));
					}
					else {
						// Intersection points
						Vec3d diff = new Vec3d(pOut);
						diff.sub3(pIn);
						Vec3d delta = new Vec3d(vecIn);
						delta.scale3(diff.dot3(nOut)/vecIn.dot3(nOut));
						if (delta.mag3()/halfWidth < 4.0d) {
							// Midpoint between the closest points along vecIn and vecOut
							Vec3d p0 = new Vec3d(pIn);
							p0.add3(delta);

							delta = new Vec3d(vecOut);
							delta.scale3(-diff.dot3(nIn)/vecOut.dot3(nIn));
							p0.add3(pOut);
							p0.add3(delta);
							p0.scale3(0.5d);
							points2.add(0, new Vec4d(p0, 1.0d));
						}
						else {
							points2.add(0, new Vec4d(pIn, 1.0d));
							points2.add(0, new Vec4d(pOut, 1.0d));
						}
					}

					// Show the polygon formed by the present points and the last points
					Vec4d lastPt1 = new Vec4d(points1.get(points1.size() - 1));
					Vec4d lastPt2 = new Vec4d(points2.get(0));
					if (lastPoint1 != null && lastPoint2 != null) {
						ArrayList<Vec4d> points = new ArrayList<>();
						points.add(lastPoint1);
						points.addAll(points1);
						points.addAll(points2);
						points.add(lastPoint2);
						if (globalTransCache != null)
							RenderUtils.transformPointsLocal(globalTransCache, points, 0);
						cachedProxies.add(new PolygonProxy(points, Transform.ident, DisplayModel.ONES,
								fillColourCache, false, 1, viCache, id));
						//System.out.format("lastPoint1=%s, lastPoint2=%s, points1=%s, points2=%s%n%n", lastPoint1, lastPoint2, points1, points2);
						points1.clear();
						points2.clear();
					}
					lastPoint1 = lastPt1;
					lastPoint2 = lastPt2;
				}
			}
		}

		@Override
		public void collectProxies(double simTime, ArrayList<RenderProxy> out) {

			if (displayObservee == null || !displayObservee.getShow(simTime)) {
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
			    !displayObservee.getShow(simTime) ||
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
