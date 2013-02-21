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

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.HasScreenPoints;
import com.jaamsim.render.LineProxy;
import com.jaamsim.render.PointProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class ScreenPointsModel extends DisplayModel {

	private static final Color4d MINT = ColourInput.getColorWithName("mint");

	@Override
	public DisplayModelBinding getBinding(Entity ent) {
		return new Binding(ent, this);
	}

	@Override
	public boolean canDisplayEntity(Entity ent) {
		return (ent instanceof HasScreenPoints) && (ent instanceof DisplayEntity);
	}

	protected class Binding extends DisplayModelBinding {

		//private Segment _segmentObservee;
		protected HasScreenPoints screenPointObservee;
		protected DisplayEntity displayObservee;
		private ChangeWatcher.Tracker observeeTracker;
		private ChangeWatcher.Tracker dmTracker;

		protected ArrayList<Vec4d> points = null;
		private ArrayList<Vec4d> nodePoints = null;
		private LineProxy cachedProxy = null;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);

			try {
				screenPointObservee = (HasScreenPoints)ent;
				displayObservee = (DisplayEntity)ent;
				if (displayObservee != null) {
					observeeTracker = displayObservee.getGraphicsChangeTracker();
				}
			} catch (ClassCastException e) {
				// The observee is not a display entity
				screenPointObservee = null;
				displayObservee = null;
			}
			dmTracker = dm.getGraphicsChangeTracker();

		}

		/**
		 * Update the cached Points list
		 */
		protected void updatePoints(double simTime) {

			if (points != null && observeeTracker != null && !observeeTracker.checkAndClear()
			    && !dmTracker.checkAndClear()) {
				// up to date
				_cacheHits++;
				return;
			}

			_cacheMisses++;
			registerCacheMiss("Points");

			// Convert to JaamSim math lib, and convert from a line list to discrete segments
			points = new ArrayList<Vec4d>();
			nodePoints = new ArrayList<Vec4d>();
			ArrayList<Vec3d> screenPoints = screenPointObservee.getScreenPoints();
			if (screenPoints == null || screenPoints.size() < 2) { return; }

			for (int i = 1; i < screenPoints.size(); ++i) { // Skip the first point
				Vec3d start = screenPoints.get(i - 1);
				Vec3d end = screenPoints.get(i);

				points.add(new Vec4d(start.x, start.y, start.z, 1.0d));
				points.add(new Vec4d(end.x, end.y, end.z, 1.0d));
			}

			for (int i = 0; i < screenPoints.size(); ++i) { // Skip the first point
				// Save the point list as is for control nodes
				Vec3d p = screenPoints.get(i);
				nodePoints.add(new Vec4d(p.x, p.y, p.z, 1.0d));
			}

			if (displayObservee.getCurrentRegion() != null) {
				Transform regionTrans = displayObservee.getCurrentRegion().getRegionTrans(simTime);
				RenderUtils.transformPointsLocal(regionTrans, points, 0);
				RenderUtils.transformPointsLocal(regionTrans, nodePoints, 0);
			}

			cachedProxy = new LineProxy(points, screenPointObservee.getDisplayColour(),
		                                screenPointObservee.getWidth(), getVisibilityInfo(), displayObservee.getEntityNumber());
		}

		@Override
		public void collectProxies(double simTime, ArrayList<RenderProxy> out) {

			if (displayObservee == null || screenPointObservee == null ||!displayObservee.getShow()) {
				return;
			}

			updatePoints(simTime);

			if (points.size() == 0) {
				return;
			}

			out.add(cachedProxy);

		}

		@Override
		public void collectSelectionProxies(double simTime, ArrayList<RenderProxy> out) {

			if (displayObservee == null ||
			    screenPointObservee == null ||
			    !displayObservee.getShow() ||
			    !screenPointObservee.selectable()) {
				return;
			}

			updatePoints(simTime);

			if (points.size() == 0) {
				return;
			}

			LineProxy lp = new LineProxy(points, MINT, 2, getVisibilityInfo(), RenderManager.LINEDRAG_PICK_ID);
			lp.setHoverColour(ColourInput.LIGHT_GREY);
			out.add(lp);

			for (int i = 0; i < nodePoints.size(); ++i) {
				List<Vec4d> pl = new ArrayList<Vec4d>(1);

				pl.add(new Vec4d(nodePoints.get(i)));
				PointProxy pp = new PointProxy(pl, ColourInput.GREEN, 8, getVisibilityInfo(), RenderManager.LINENODE_PICK_ID - i);
				pp.setHoverColour(ColourInput.LIGHT_GREY);
				out.add(pp);
			}

		}

	}
}
