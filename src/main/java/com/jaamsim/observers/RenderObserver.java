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

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Matrix4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;
import com.jaamsim.render.LineProxy;
import com.jaamsim.render.PointProxy;
import com.jaamsim.render.PolygonProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public abstract class RenderObserver extends Observer {
	private static final Color4d MINT = ColourInput.getColorWithName("mint");

	protected DisplayEntity _dispObservee;

	private ChangeWatcher.Tracker _selectionTracker;

	private List<Vector4d> handlePoints = null;

	private List<Vector4d> rotateHandlePoints = null;

	private final static ArrayList<Vector4d> HANDLE_POINTS;
	private final static ArrayList<Vector4d> ROTATE_POINTS;

	protected static int _cacheHits = 0;
	protected static int _cacheMisses = 0;


	static {
		// NOTE: the order of the points corresponds to the list of static picking IDs in RenderManager,
		// both need to be changed together
		HANDLE_POINTS = new ArrayList<Vector4d>(8);
		// Sides
		HANDLE_POINTS.add(new Vector4d( 0.5,    0, 0));
		HANDLE_POINTS.add(new Vector4d(-0.5,    0, 0));
		HANDLE_POINTS.add(new Vector4d(   0,  0.5, 0));
		HANDLE_POINTS.add(new Vector4d(   0, -0.5, 0));

		// Corners
		HANDLE_POINTS.add(new Vector4d( 0.5,  0.5, 0));
		HANDLE_POINTS.add(new Vector4d( 0.5, -0.5, 0));
		HANDLE_POINTS.add(new Vector4d(-0.5,  0.5, 0));
		HANDLE_POINTS.add(new Vector4d(-0.5, -0.5, 0));

		ROTATE_POINTS = new ArrayList<Vector4d>(2);
		// Sides
		ROTATE_POINTS.add(new Vector4d(1.0, 0, 0));
		ROTATE_POINTS.add(new Vector4d(0.5, 0, 0));
}

	RenderObserver(Entity observee) {
		super(observee);
		try {
			_dispObservee = (DisplayEntity)observee;
			_selectionTracker = _dispObservee.getGraphicsChangeTracker();
		} catch (ClassCastException e) {
			// The observee is not a display entity
			_dispObservee = null;
			// Debug assert, not actually an error
			assert(false);
		}

		RenderManager.inst().registerObserver(this);

	}

	public abstract void collectProxies(ArrayList<RenderProxy> out);

	private void updatePoints() {

		// Convert the points to world space
		double simTime = _dispObservee.getCurrentTime();

		Transform trans = _dispObservee.getGlobalTrans(simTime);
		Vector4d scale = _dispObservee.getJaamMathSize();

		Matrix4d mat = new Matrix4d(trans.getMatrixRef());
		mat.mult(Matrix4d.ScaleMatrix(scale), mat);

		handlePoints = RenderUtils.transformPoints(mat, HANDLE_POINTS);

		rotateHandlePoints = RenderUtils.transformPoints(mat, ROTATE_POINTS);

	}

	// Collect the proxies for the selection box
	public void collectSelectionProxies(ArrayList<RenderProxy> out) {
		collectSelectionBox(out);
	}

	// This is exposed differently than above, because of the weird type heirarchy around
	// ScreenPointsObservers. This can't just be overloaded, because sometime we want it back....
	protected void collectSelectionBox(ArrayList<RenderProxy> out) {

		if (_dispObservee == null) {
			// We're not looking at anything
			return;
		}
		double simTime = _dispObservee.getCurrentTime();
		Transform trans = _dispObservee.getGlobalTrans(simTime);
		Vector4d scale = _dispObservee.getJaamMathSize();

		PolygonProxy outline = new PolygonProxy(RenderUtils.RECT_POINTS, trans, scale,
		                                        MINT, true, 1,
		                                        RenderManager.MOVE_PICK_ID);
		outline.setHoverColour(ColourInput.LIGHT_GREY);
		out.add(outline);

		if (handlePoints == null || _selectionTracker.checkAndClear()) {
			updatePoints();
		}

		for (int i = 0; i < 8; ++i) {

			List<Vector4d> pl = new ArrayList<Vector4d>(1);

			pl.add(handlePoints.get(i));
			PointProxy point = new PointProxy(pl, ColourInput.GREEN, 8, RenderManager.RESIZE_POSX_PICK_ID - i);
			point.setHoverColour(ColourInput.LIGHT_GREY);
			out.add(point);
		}

		// Add the rotate handle
		List<Vector4d> pl = new ArrayList<Vector4d>(1);
		pl.add(rotateHandlePoints.get(0));
		PointProxy point = new PointProxy(pl, ColourInput.GREEN, 8, RenderManager.ROTATE_PICK_ID);
		point.setHoverColour(ColourInput.LIGHT_GREY);
		out.add(point);

		LineProxy rotateLine = new LineProxy(rotateHandlePoints, MINT, 1, RenderManager.ROTATE_PICK_ID);
		rotateLine.setHoverColour(ColourInput.LIGHT_GREY);
		out.add(rotateLine);
	}

	public static int getCacheHits() {
		return _cacheHits;
	}

	public static int getCacheMisses() {
		return _cacheMisses;
	}
	public static void clearCacheCounters() {
		_cacheHits = 0;
		_cacheMisses = 0;
	}


}
