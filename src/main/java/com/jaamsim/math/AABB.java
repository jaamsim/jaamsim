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
	private final Vec4d _posPoint = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);

	/**
	 * The most negative point (MinX, MinY, MinZ)
	 */
	private final Vec4d _negPoint = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);

	private final Vec4d _center = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
	private final Vec4d _radius = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);

	private final Vec4d _radTemp = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);

	public AABB() {
		this._isEmpty = true;
	}

	/**
	 * Copy constructor for defensive copies
	 * @param other
	 */
	public AABB(AABB other) {
		this._isEmpty = other._isEmpty;
		this._negPoint.set4(other._negPoint);
		this._posPoint.set4(other._posPoint);

		updateCenterAndRadius();
	}

	public AABB(Vec4d posPoint, Vec4d negPoint) {
		_posPoint.set4(posPoint);
		_negPoint.set4(negPoint);

		updateCenterAndRadius();
	}

	/**
	 * Build an AABB with an expanded area
	 * @param points
	 * @param expansion
	 */
	public AABB(List<Vec4d> points, double fudge) {
		this(points);
		_posPoint.x += fudge;
		_posPoint.y += fudge;
		_posPoint.z += fudge;

		_negPoint.x -= fudge;
		_negPoint.y -= fudge;
		_negPoint.z -= fudge;

		updateCenterAndRadius();

	}

	/**
	 * Build an AABB that contains all the supplied points
	 * @param points
	 */
	public AABB(List<Vec4d> points) {
		if (points.size() == 0) {
			_isEmpty = true;
			return;
		}

		_posPoint.set4(points.get(0));
		_negPoint.set4(points.get(0));
		for (Vec4d p : points) {
			_posPoint.max3(p);
			_negPoint.min3(p);
		}

		updateCenterAndRadius();

	}

	/**
	 * Build an AABB that contains all the supplied points, transformed by trans
	 * @param points
	 */
	/**
	 * Build an AABB that contains all the supplied points
	 * @param points
	 */
	public AABB(List<Vec4d> points, Mat4d trans) {
		if (points.size() == 0) {
			_isEmpty = true;
			return;
		}

		Vec4d p = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		p.mult4(trans, points.get(0));

		_posPoint.set4(p);
		_negPoint.set4(p);
		for (Vec4d p_orig : points) {
			p.mult4(trans, p_orig);
			_posPoint.max3(p);
			_negPoint.min3(p);
		}

		updateCenterAndRadius();

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

		Vec4d newPos = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		newPos.max3(_posPoint, other._posPoint);

		Vec4d newNeg = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		newNeg.min3(_negPoint, other._negPoint);

		return new AABB(newPos, newNeg);
	}

	/**
	 * Check collision, but allow for a fudge factor on the AABB
	 */
	public boolean collides(Vec4d point, double fudge) {
		if (_isEmpty) {
			return false;
		}

		boolean bX = point.x > _negPoint.x - fudge && point.x < _posPoint.x + fudge;
		boolean bY = point.y > _negPoint.y - fudge && point.y < _posPoint.y + fudge;
		boolean bZ = point.z > _negPoint.z - fudge && point.z < _posPoint.z + fudge;
		return bX && bY && bZ;
	}

	public boolean collides(Vec4d point) {
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

		boolean bX = MathUtils.segOverlap(_negPoint.x, _posPoint.x, other._negPoint.x, other._posPoint.x, fudge);
		boolean bY = MathUtils.segOverlap(_negPoint.y, _posPoint.y, other._negPoint.y, other._posPoint.y, fudge);
		boolean bZ = MathUtils.segOverlap(_negPoint.z, _posPoint.z, other._negPoint.z, other._posPoint.z, fudge);
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

		Vec4d rayDir = r.getDirRef();
		// Iterate over the 3 axes
		for (int axis = 0; axis < 3; ++axis) {
			if (MathUtils.near(rayDir.getComp(axis), 0)) {
				continue; // The ray is parallel to the box in this axis
			}

			Vec4d faceNorm = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
			double faceDist = 0;
			if (rayDir.getComp(axis) > 0) {
				// Collides with the negative face
				faceNorm.setComp(axis, -1);
				faceDist = -_negPoint.getComp(axis) - fudge;
			} else {
				faceNorm.setComp(axis, 1);
				faceDist = _posPoint.getComp(axis) + fudge;
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
			Vec4d contactPoint = r.getPointAtDist(rayCollisionDist);

			if (contactPoint.getComp(a1) < _negPoint.getComp(a1) - fudge || contactPoint.getComp(a1) > _posPoint.getComp(a1) + fudge) {
				continue; // No contact
			}

			if (contactPoint.getComp(a2) < _negPoint.getComp(a2) - fudge || contactPoint.getComp(a2) > _posPoint.getComp(a2) + fudge) {
				continue; // No contact
			}
			// Collision!
			return rayCollisionDist;
		}

		return -1.0;
	}

	private void updateCenterAndRadius() {
		_center.set4(_posPoint);
		_center.add3(_negPoint);
		_center.scale3(0.5);

		_radius.set4(_posPoint);
		_radius.sub3(_negPoint);
		_radius.scale3(0.5);

	}

	/**
	 * Get the center point of the AABB, this is not valid for empty AABBs
	 * @return
	 */
	public Vec4d getCenter() {
		return _center;
	}

	/**
	 * Get the 'radius' of this AABB, effectively the distance from the center to the
	 * positive most point, this is not valid for empty AABBs
	 * @return
	 */
	public Vec4d getRadius() {
		return _radius;
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

		_radTemp.set4(_radius);
		Vec4d pNorm = p.getNormalRef();
		// Make sure the radius points in the same direction of the normal
		if (pNorm.x < 0) { _radTemp.x *= -1; }
		if (pNorm.y < 0) { _radTemp.y *= -1; }
		if (pNorm.z < 0) { _radTemp.z *= -1; }

		double effectiveRadius = _radTemp.dot3(pNorm);

		double centerDist = p.getNormalDist(_center);
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
