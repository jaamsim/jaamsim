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

import javax.vecmath.Vector3d;

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;
import com.jaamsim.render.HasScreenPoints;
import com.jaamsim.render.LineProxy;
import com.jaamsim.render.PointProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class ScreenPointsObserver extends RenderObserver {
	private static final Color4d MINT = ColourInput.getColorWithName("mint");

	//private Segment _segmentObservee;
	protected HasScreenPoints _screenPointObservee;
	private ChangeWatcher.Tracker _observeeTracker;

	protected ArrayList<Vector4d> points = null;
	private ArrayList<Vector4d> nodePoints = null;
	private LineProxy cachedProxy = null;

	ScreenPointsObserver(Entity observee) {
		super(observee);

		try {
			_screenPointObservee = (HasScreenPoints)observee;
			_observeeTracker = ((DisplayEntity)observee).getGraphicsChangeTracker();
		} catch (ClassCastException e) {
			// The observee is not a display entity
			_screenPointObservee = null;
			// Debug assert, not actually an error
			assert(false);
		}

	}

	/**
	 * Update the cached Points list
	 */
	protected void updatePoints() {

		if (points != null && !_observeeTracker.checkAndClear()) {
			// up to date
			_cacheHits++;
			return;
		}

		_cacheMisses++;

		// Convert to JaamSim math lib, and convert from a line list to discrete segments
		points = new ArrayList<Vector4d>();
		nodePoints = new ArrayList<Vector4d>();
		ArrayList<Vector3d> screenPoints = _screenPointObservee.getScreenPoints();
		if (screenPoints == null || screenPoints.size() < 2) { return; }

		for (int i = 1; i < screenPoints.size(); ++i) { // Skip the first point
			Vector3d start = screenPoints.get(i - 1);
			Vector3d end = screenPoints.get(i);

			points.add(new Vector4d(start.x, start.y, start.z));
			points.add(new Vector4d(end.x, end.y, end.z));
		}

		for (int i = 0; i < screenPoints.size(); ++i) { // Skip the first point
			// Save the point list as is for control nodes
			Vector3d p = screenPoints.get(i);
			nodePoints.add(new Vector4d(p.x, p.y, p.z));
		}

		double simTime = _dispObservee.getCurrentTime();

		if (_dispObservee.getCurrentRegion() != null) {
			Transform regionTrans = _dispObservee.getCurrentRegion().getRegionTrans(simTime);
			RenderUtils.transformPointsLocal(regionTrans, points);
			RenderUtils.transformPointsLocal(regionTrans, nodePoints);
		}

		cachedProxy = new LineProxy(points, _screenPointObservee.getDisplayColour(),
	                                _screenPointObservee.getWidth(), _dispObservee.getEntityNumber());
	}

	@Override
	public void collectProxies(ArrayList<RenderProxy> out) {

		if (_dispObservee == null || _screenPointObservee == null ||!_dispObservee.getShow()) {
			return;
		}

		updatePoints();

		if (points.size() == 0) {
			return;
		}

		out.add(cachedProxy);

	}

	@Override
	public void collectSelectionProxies(ArrayList<RenderProxy> out) {

		if (_dispObservee == null ||
		    _screenPointObservee == null ||
		    !_dispObservee.getShow() ||
		    !_screenPointObservee.selectable()) {
			return;
		}

		updatePoints();

		if (points.size() == 0) {
			return;
		}

		LineProxy lp = new LineProxy(points, MINT, 2, RenderManager.LINEDRAG_PICK_ID);
		lp.setHoverColour(ColourInput.LIGHT_GREY);
		out.add(lp);

		for (int i = 0; i < nodePoints.size(); ++i) {
			List<Vector4d> pl = new ArrayList<Vector4d>(1);

			pl.add(nodePoints.get(i));
			PointProxy pp = new PointProxy(pl, ColourInput.GREEN, 8, RenderManager.LINENODE_PICK_ID - i);
			pp.setHoverColour(ColourInput.LIGHT_GREY);
			out.add(pp);
		}

	}
}
