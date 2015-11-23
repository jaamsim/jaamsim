/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.render;

import java.nio.FloatBuffer;
import java.util.List;

import com.jaamsim.math.AABB;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Vec4d;

public class DebugLine implements Renderable {

	private FloatBuffer _fb;
	private List<Vec4d> _lineSegments;
	private final float[] _colour;
	private final float[] _hoverColour;
	private double _lineWidth;
	private long _pickingID;
	private double _collisionFudge;
	private VisibilityInfo _visInfo;

	private double _collisionAngle = 0.01309; // 0.75 degrees in radians

	private AABB _bounds;

	public DebugLine(List<Vec4d> lineSegments, Color4d colour, Color4d hoverColour, double lineWidth, VisibilityInfo visInfo, long pickingID) {
		_lineSegments = lineSegments;
		_colour = colour.toFloats();
		_hoverColour = hoverColour.toFloats();
		_lineWidth = lineWidth;
		_pickingID = pickingID;
		_visInfo = visInfo;

		_bounds = new AABB(lineSegments);
		_collisionFudge = _bounds.radius.mag3() * 0.1; // Allow a 10% fudge factor on the overall AABB size
		_fb = FloatBuffer.allocate(3 * lineSegments.size());
		for (Vec4d vert : lineSegments) {
			RenderUtils.putPointXYZ(_fb, vert);
		}
		_fb.flip();

	}

	@Override
	public void render(int contextID, Renderer renderer,
			Camera cam, Ray pickRay) {

		float[] renderColour = _colour;
		if (pickRay != null && getCollisionDist(pickRay, false) > 0)
			renderColour = _hoverColour;

		DebugUtils.renderLine(contextID, renderer, _fb, renderColour, _lineWidth, cam);
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
	public double getCollisionDist(Ray r, boolean precise) {
		if (r == null) {
			return -1;
		}

		double boundsDist = _bounds.collisionDist(r, _collisionFudge);
		if (boundsDist < 0) { return boundsDist; } // no bounds collision

		// Otherwise perform collision cone tests on individual line segments
		Mat4d rayMatrix = MathUtils.RaySpace(r);

		Vec4d[] linesArray = _lineSegments.toArray(new Vec4d[_lineSegments.size()]);
		return MathUtils.collisionDistLines(rayMatrix, linesArray, _collisionAngle);
	}

	@Override
	public boolean hasTransparent() {
		return false;
	}

	@Override
	public void renderTransparent(int contextID, Renderer renderer, Camera cam, Ray pickRay) {
	}

	@Override
	public boolean renderForView(int viewID, Camera cam) {
		double dist = cam.distToBounds(getBoundsRef());

		return _visInfo.isVisible(viewID, dist);
	}
}
