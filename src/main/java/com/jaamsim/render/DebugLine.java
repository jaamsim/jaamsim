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
import com.jaamsim.math.Matrix4d;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Vector4d;

public class DebugLine implements Renderable {

	private List<Vector4d> _lineSegments;
	private final float[] _colour;
	private final float[] _hoverColour;
	private double _lineWidth;
	private long _pickingID;
	private double _collisionFudge;

	private double _collisionAngle = 0.01309; // 0.75 degrees in radians

	private AABB _bounds;

	public DebugLine(List<Vector4d> lineSegments, Color4d colour, Color4d hoverColour, double lineWidth, long pickingID) {
		_lineSegments = lineSegments;
		_colour = colour.toFloats();
		_hoverColour = hoverColour.toFloats();
		_lineWidth = lineWidth;
		_pickingID = pickingID;

		_bounds = new AABB(lineSegments);
		_collisionFudge = _bounds.getRadius().mag3() * 0.1; // Allow a 10% fudge factor on the overall AABB size
	}

	@Override
	public void render(Map<Integer, Integer> vaoMap, Renderer renderer,
			Camera cam, Ray pickRay) {

		float[] renderColour = _colour;
		if (pickRay != null && getCollisionDist(pickRay) > 0)
			renderColour = _hoverColour;

		DebugUtils.renderLine(vaoMap, renderer, _lineSegments, renderColour, _lineWidth, cam);
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

	/**
	 * This collision test relies on a collision cone with an angle of _collisionAngle, for non-default cones
	 * call setCollisionAngle() first.
	 */
	@Override
	public double getCollisionDist(Ray r) {
		if (r == null) {
			return -1;
		}

		double boundsDist = _bounds.collisionDist(r, _collisionFudge);
		if (boundsDist < 0) { return boundsDist; } // no bounds collision

		// Otherwise perform collision cone tests on individual line segments
		Matrix4d rayMatrix = Matrix4d.RaySpace(r);

		double shortDist = Double.POSITIVE_INFINITY;

		for (int i = 0; i < _lineSegments.size(); i+=2) {
			Vector4d nearPoint = RenderUtils.rayClosePoint(rayMatrix, _lineSegments.get(i), _lineSegments.get(i+1));

			double angle = RenderUtils.angleToRay(rayMatrix, nearPoint);
			if (angle < 0) {
				continue;
			}

			Vector4d raySpaceNear = new Vector4d();
			rayMatrix.mult(nearPoint, raySpaceNear);

			if (angle < _collisionAngle && raySpaceNear.z() < shortDist) {
				shortDist = raySpaceNear.z();
			}
		}

		// Short dist is the shortest collision distance
		if (shortDist == Double.POSITIVE_INFINITY) {
			return -1; // No collision
		}
		return shortDist;
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
