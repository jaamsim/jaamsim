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
package com.jaamsim.math;

import java.util.List;

/**
 * AABB (or Axis Aligned Bounding Box) is a coarse level culling
 * @author Matt.Chudleigh
 *
 */
public class AABB {

	private boolean _isEmpty = false;

	/**
	 * The most positive point (MaxX, MaxY, MaxZ)
	 */
	private Vector4d _posPoint;

	/**
	 * The most negative point (MinX, MinY, MinZ)
	 */
	private Vector4d _negPoint;

	/**
	 * Copy constructor for defensive copies
	 * @param other
	 */
	public AABB(AABB other) {
		this._isEmpty = other._isEmpty;
		this._negPoint = new Vector4d(other._negPoint);
		this._posPoint = new Vector4d(other._posPoint);
	}

	public AABB(Vector4d posPoint, Vector4d negPoint) {
		_posPoint = new Vector4d(posPoint);
		_negPoint = new Vector4d(negPoint);
	}

	/**
	 * Build an AABB with an expanded area
	 * @param points
	 * @param expansion
	 */
	public AABB(List<Vector4d> points, double fudge) {
		this(points);
		_posPoint.data[0] += fudge;
		_posPoint.data[1] += fudge;
		_posPoint.data[2] += fudge;

		_negPoint.data[0] -= fudge;
		_negPoint.data[1] -= fudge;
		_negPoint.data[2] -= fudge;

	}

	/**
	 * Build an AABB that contains all the supplied points
	 * @param points
	 */
	public AABB(List<Vector4d> points) {
		if (points.size() == 0) {
			_isEmpty = true;
			return;
		}

		_posPoint = new Vector4d(points.get(0));
		_negPoint = new Vector4d(points.get(0));
		for (Vector4d p : points) {
			for (int i = 0; i < 3; ++i)
			{
				if (p.data[i] > _posPoint.data[i]) { _posPoint.data[i] = p.data[i]; }
				if (p.data[i] < _negPoint.data[i]) { _negPoint.data[i] = p.data[i]; }
			}
		}
	}

	/**
	 * Build an AABB that contains all the supplied points, transformed by trans
	 * @param points
	 */
	public AABB(List<Vector4d> points, Matrix4d trans) {
		if (points.size() == 0) {
			_isEmpty = true;
			return;
		}

		// Initialize to the first point in the list (transformed of course)
		Vector4d p = new Vector4d();
		trans.mult(points.get(0), p);

		_posPoint = new Vector4d(p);
		_negPoint = new Vector4d(p);
		for (Vector4d p_orig : points) {
			trans.mult(p_orig, p);

			for (int i = 0; i < 3; ++i)
			{
				if (p.data[i] > _posPoint.data[i]) { _posPoint.data[i] = p.data[i]; }
				if (p.data[i] < _negPoint.data[i]) { _negPoint.data[i] = p.data[i]; }
			}
		}
	}

	/**
	 * Return an AABB that contains both this and 'other'
	 * @param other
	 * @return
	 */
	public AABB superBox(AABB other) {
		if (other._isEmpty) {
			return new AABB(this);
		}

		if (this._isEmpty) {
			return new AABB(other);
		}

		Vector4d newPos = new Vector4d(
				Math.max(_posPoint.x(), other._posPoint.x()),
				Math.max(_posPoint.y(), other._posPoint.y()),
				Math.max(_posPoint.z(), other._posPoint.z()));

		Vector4d newNeg = new Vector4d(
				Math.min(_negPoint.x(), other._negPoint.x()),
				Math.min(_negPoint.y(), other._negPoint.y()),
				Math.min(_negPoint.z(), other._negPoint.z()));

		return new AABB(newPos, newNeg);
	}

	/**
	 * Check collision, but allow for a fudge factor on the AABB
	 */
	public boolean collides(Vector4d point, double fudge) {
		if (_isEmpty) {
			return false;
		}

		boolean bX = point.x() > _negPoint.x() - fudge && point.x() < _posPoint.x() + fudge;
		boolean bY = point.y() > _negPoint.y() - fudge && point.y() < _posPoint.y() + fudge;
		boolean bZ = point.z() > _negPoint.z() - fudge && point.z() < _posPoint.z() + fudge;
		return bX && bY && bZ;
	}

	public boolean collides(Vector4d point) {
		return collides(point, 0);
	}

	public boolean collides(AABB other) {
		return collides(other, 0);
	}
	/**
	 * Check collision, but allow for a fudge factor on the AABB
	 */
	public boolean collides(AABB other, double fudge) {
		if (this._isEmpty || other._isEmpty) {
			return false;
		}

		boolean bX = MathUtils.segOverlap(_negPoint.x(), _posPoint.x(), other._negPoint.x(), other._posPoint.x(), fudge);
		boolean bY = MathUtils.segOverlap(_negPoint.y(), _posPoint.y(), other._negPoint.y(), other._posPoint.y(), fudge);
		boolean bZ = MathUtils.segOverlap(_negPoint.z(), _posPoint.z(), other._negPoint.z(), other._posPoint.z(), fudge);
		return bX && bY && bZ;
	}

	/**
	 * Get the distance that this ray collides with the AABB, a negative number indicates no collision
	 * @param r
	 * @return
	 */
	public double collisionDist(Ray r) {
		return collisionDist(r, 0);
	}

	public double collisionDist(Ray r, double fudge) {
		if (_isEmpty) {
			return -1;
		}

		if (collides(r.getStartRef(), fudge)) {
			return 0.0; // The ray starts in the AABB
		}

		Vector4d rayDir = r.getDirRef();
		// Iterate over the 3 axes
		for (int axis = 0; axis < 3; ++axis) {
			if (MathUtils.near(rayDir.data[axis], 0)) {
				continue; // The ray is parallel to the box in this axis
			}

			Vector4d faceNorm = new Vector4d();
			double faceDist = 0;
			if (rayDir.data[axis] > 0) {
				// Collides with the negative face
				faceNorm.data[axis] = -1;
				faceDist = -_negPoint.data[axis] - fudge;
			} else {
				faceNorm.data[axis] = 1;
				faceDist = _posPoint.data[axis] + fudge;
			}

			Plane facePlane = new Plane(faceNorm, faceDist);

			// Get the distance along the ray the ray collides with the plane
			double rayCollisionDist = facePlane.collisionDist(r);
			if (Double.isInfinite(rayCollisionDist)) {
				continue; // Parallel (but we should have already tested for this)
			}
			if (rayCollisionDist < 0) {
				// Behind the ray
				continue;
			}


			// Finally check if the collision point is actually inside the face we are testing against
			int a1 = (axis + 1) % 3;
			int a2 = (axis + 2) % 3;

			// Figure out the point of contact
			Vector4d contactPoint = r.getPointAtDist(rayCollisionDist);

			if (contactPoint.data[a1] < _negPoint.data[a1] - fudge || contactPoint.data[a1] > _posPoint.data[a1] + fudge) {
				continue; // No contact
			}

			if (contactPoint.data[a2] < _negPoint.data[a2] - fudge || contactPoint.data[a2] > _posPoint.data[a2] + fudge) {
				continue; // No contact
			}
			// Collision!
			return rayCollisionDist;
		}

		return -1.0;
	}

	/**
	 * Get the center point of the AABB, this is not valid for empty AABBs
	 * @return
	 */
	public Vector4d getCenter() {
		Vector4d ret = new Vector4d(_posPoint);
		ret.addLocal3(_negPoint);
		ret.scaleLocal3(0.5);
		return ret;
	}

	/**
	 * Get the 'radius' of this AABB, effectively the distance from the center to the
	 * positive most point, this is not valid for empty AABBs
	 * @return
	 */
	public Vector4d getRadius() {
		Vector4d ret = new Vector4d(_posPoint);
		ret.subLocal3(_negPoint);
		ret.scaleLocal3(0.5);
		return ret;
	}

	public boolean isEmpty() {
		return _isEmpty;
	}

	public enum PlaneTestResult {
		COLLIDES, POSITIVE, NEGATIVE, EMPTY
	}

	/**
	 * Is the AABB completely on one side of this plane, or colliding?
	 * @param p
	 * @return
	 */
	public PlaneTestResult testToPlane(Plane p) {
		if (_isEmpty) {
			return PlaneTestResult.EMPTY;
		}

		Vector4d rad = getRadius();
		Vector4d pNorm = p.getNormalRef();
		// Make sure the radius points in the same direction of the normal
		for (int i = 0; i < 3; ++i) {
			if (pNorm.data[i] < 0) { rad.data[i] *= -1; }
		}
		double effectiveRadius = rad.dot3(pNorm);

		double centerDist = p.getNormalDist(getCenter());
		// If the effective radius is greater than the distance to the center, we're good
		if (centerDist > effectiveRadius) {
			return PlaneTestResult.POSITIVE;
		}

		if (centerDist < -effectiveRadius) {
			// Complete
			return PlaneTestResult.NEGATIVE;
		}

		return PlaneTestResult.COLLIDES;
	}
}
