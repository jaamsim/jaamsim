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
package com.jaamsim.render;

import java.util.List;
import java.util.Map;

import com.jaamsim.math.AABB;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Vector4d;

public class DebugPoints implements Renderable {

	private List<Vector4d> _points;
	private final float[] _colour;
	private final float[] _hoverColour;
	private double _pointWidth;
	private long _pickingID;

	private double _collisionAngle = 0.008727; // 0.5 degrees in radians

	private AABB _bounds;

	public DebugPoints(List<Vector4d> points, Color4d colour, Color4d hoverColour, double pointWidth, long pickingID) {
		_points = points;
		_colour = colour.toFloats();
		_hoverColour = hoverColour.toFloats();
		_pointWidth = pointWidth;
		_pickingID = pickingID;

		_bounds = new AABB(_points, 100000); // TODO, tune this fudge factor by something more real
	}

	@Override
	public void render(Map<Integer, Integer> vaoMap, Renderer renderer,
			Camera cam, Ray pickRay) {

		float[] renderColour = _colour;
		if (pickRay != null && getCollisionDist(pickRay) > 0)
			renderColour = _hoverColour;

		DebugUtils.renderPoints(vaoMap, renderer, _points, renderColour, _pointWidth, cam);
	}

	@Override
	public long getPickingID() {
		return _pickingID;
	}

	@Override
	public AABB getBoundsRef() {
		return _bounds;
	}

	/**
	 * Set the angle of the collision cone in radians
	 * @param angle
	 */
	public void setCollisionAngle(double angle) {
		_collisionAngle = angle;
	}

	@Override
	public double getCollisionDist(Ray r) {
		if (r == null) {
			return -1;
		}

		double boundsDist = _bounds.collisionDist(r);
		if (boundsDist < 0) { return boundsDist; } // no bounds collision

		double tan = Math.tan(_collisionAngle);

		Vector4d op = new Vector4d(); // Vector from ray start to

		double nearDist = Double.POSITIVE_INFINITY;

		for (Vector4d p : _points) {
			p.sub3(r.getStartRef(), op);
			double hypot2 = op.magSquared3();

			double dot = op.dot3(r.getDirRef()); // Dot is the distance along the ray to the nearest point

			double rayDist = Math.sqrt(hypot2 - dot*dot);

			double collsionThreshold = dot * tan;
			if (rayDist < collsionThreshold && dot < nearDist) {
				// This is the closest point so far
				nearDist = dot;
			}
		}

		if (nearDist == Double.POSITIVE_INFINITY) {
			return -1;
		}
		return nearDist;
	}

	@Override
	public boolean hasTransparent() {
		return false;
	}

	@Override
	public void renderTransparent(Map<Integer, Integer> vaoMap, Renderer renderer, Camera cam, Ray pickRay) {
	}

	@Override
	public boolean renderForView(int windowID) {
		return true;
	}


}
