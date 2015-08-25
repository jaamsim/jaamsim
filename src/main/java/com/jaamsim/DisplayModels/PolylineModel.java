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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.PolylineInfo;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.ColourInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.LineProxy;
import com.jaamsim.render.PointProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.VisibilityInfo;

public class PolylineModel extends DisplayModel {

	private static final Color4d MINT = ColourInput.getColorWithName("mint");

	@Override
	public DisplayModelBinding getBinding(Entity ent) {
		return new Binding(ent, this);
	}

	@Override
	public boolean canDisplayEntity(Entity ent) {
		return (ent instanceof DisplayEntity);
	}

	protected class Binding extends DisplayModelBinding {

		//private Segment _segmentObservee;
		protected DisplayEntity displayObservee;

		private PolylineInfo[] pisCache;
		private Transform transCache;
		private VisibilityInfo viCache;

		protected ArrayList<Vec4d> selectionPoints = null;
		private ArrayList<Vec4d> nodePoints = null;
		private LineProxy[] cachedProxies = null;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);

			try {
				displayObservee = (DisplayEntity)ent;
			} catch (ClassCastException e) {
				// The observee is not a display entity
				displayObservee = null;
			}
		}

		/**
		 * Update the cached Points list
		 */
		protected void updateProxies(double simTime) {

			PolylineInfo[] pis = displayObservee.getScreenPoints();
			if (pis == null || pis.length == 0)
				return;

			Transform trans = null;
			if (displayObservee.getCurrentRegion() != null || displayObservee.getRelativeEntity() != null) {
				trans = displayObservee.getGlobalPositionTransform();
			}

			VisibilityInfo vi = getVisibilityInfo();

			boolean dirty = false;

			dirty = dirty || !compareArray(pisCache, pis);
			dirty = dirty || !compare(transCache, trans);
			dirty = dirty || !compare(viCache, vi);

			pisCache = pis;
			transCache = trans;
			viCache = vi;

			if (cachedProxies != null && !dirty) {
				// up to date
				registerCacheHit("Points");
				return;
			}

			registerCacheMiss("Points");

			if (pis.length == 0) {
				cachedProxies = new LineProxy[0];
				return;
			}

			selectionPoints = new ArrayList<>();
			nodePoints = new ArrayList<>();

			// Cache the points in the first series for selection and editing
			ArrayList<Vec3d> basePoints = pis[0].points;
			if (basePoints == null || basePoints.size() < 2) {
				cachedProxies = new LineProxy[0];
				return;
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

			if (trans != null) {
				RenderUtils.transformPointsLocal(trans, selectionPoints, 0);
				RenderUtils.transformPointsLocal(trans, nodePoints, 0);
			}

			// Add the line proxies
			cachedProxies = new LineProxy[pis.length];

			int proxyIndex = 0;
			for (PolylineInfo pi : pis) {
				List<Vec4d> points = new ArrayList<>();

				for (int i = 1; i < pi.points.size(); ++i) { // Skip the first point
					Vec3d start = pi.points.get(i - 1);
					Vec3d end = pi.points.get(i);

					points.add(new Vec4d(start.x, start.y, start.z, 1.0d));
					points.add(new Vec4d(end.x, end.y, end.z, 1.0d));
				}

				if (trans != null) {
					RenderUtils.transformPointsLocal(trans, points, 0);
				}

				cachedProxies[proxyIndex++] = new LineProxy(points, pi.color, pi.width, vi, displayObservee.getEntityNumber());
			}
		}

		@Override
		public void collectProxies(double simTime, ArrayList<RenderProxy> out) {

			if (displayObservee == null ||!displayObservee.getShow()) {
				return;
			}

			updateProxies(simTime);

			for (LineProxy lp : cachedProxies) {
				out.add(lp);
			}
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
		out.add(pp);
	}
}
