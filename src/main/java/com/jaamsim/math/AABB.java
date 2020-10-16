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
package com.jaamsim.math;

import java.util.List;

/**
 * AABB (or Axis Aligned Bounding Box) is a coarse level culling
 * @author Matt.Chudleigh
 *
 */
public class AABB {

	private boolean _isEmpty = false;

	/** The most positive point (MaxX, MaxY, MaxZ) */
	public final Vec3d maxPt = new Vec3d();

	/** The most negative point (MinX, MinY, MinZ) */
	public final Vec3d minPt = new Vec3d();

	public final Vec3d center = new Vec3d();
	public final Vec3d radius = new Vec3d();

	public AABB() {
		this._isEmpty = true;
	}

	/**
	 * Copy constructor for defensive copies
	 * @param other
	 */
	public AABB(AABB other) {
		this._isEmpty = other._isEmpty;
		this.minPt.set3(other.minPt);
		this.maxPt.set3(other.maxPt);

		updateCenterAndRadius();
	}

	public AABB(Vec3d posPoint, Vec3d negPoint) {
		maxPt.set3(posPoint);
		minPt.set3(negPoint);

		updateCenterAndRadius();
	}

	/**
	 * Build an AABB with an expanded area
	 * @param points
	 * @param fudge
	 */
	public AABB(List<? extends Vec3d> points, double fudge) {
		this(points);
		maxPt.x += fudge;
		maxPt.y += fudge;
		maxPt.z += fudge;

		minPt.x -= fudge;
		minPt.y -= fudge;
		minPt.z -= fudge;

		updateCenterAndRadius();

	}

	/**
	 * Build an AABB that contains all the supplied points
	 * @param points
	 */
	public AABB(List<? extends Vec3d> points) {
		if (points.size() == 0) {
			_isEmpty = true;
			return;
		}

		maxPt.set3(points.get(0));
		minPt.set3(points.get(0));
		for (Vec3d p : points) {
			maxPt.max3(p);
			minPt.min3(p);
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
	public AABB(List<? extends Vec3d> points, Mat4d trans) {
		if (points.size() == 0) {
			_isEmpty = true;
			return;
		}

		Vec3d p = new Vec3d();
		p.multAndTrans3(trans, points.get(0));

		maxPt.set3(p);
		minPt.set3(p);
		for (Vec3d p_orig : points) {
			p.multAndTrans3(trans, p_orig);
			maxPt.max3(p);
			minPt.min3(p);
		}

		updateCenterAndRadius();

	}

	/**
	 * Check collision, but allow for a fudge factor on the AABB
	 */
	public boolean collides(Vec3d point, double fudge) {
		if (_isEmpty) {
			return false;
		}

		boolean bX = point.x > minPt.x - fudge && point.x < maxPt.x + fudge;
		boolean bY = point.y > minPt.y - fudge && point.y < maxPt.y + fudge;
		boolean bZ = point.z > minPt.z - fudge && point.z < maxPt.z + fudge;
		return bX && bY && bZ;
	}

	public boolean collides(Vec3d point) {
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

		boolean bX = MathUtils.segOverlap(minPt.x, maxPt.x, other.minPt.x, other.maxPt.x, fudge);
		boolean bY = MathUtils.segOverlap(minPt.y, maxPt.y, other.minPt.y, other.maxPt.y, fudge);
		boolean bZ = MathUtils.segOverlap(minPt.z, maxPt.z, other.minPt.z, other.maxPt.z, fudge);
		return bX && bY && bZ;
	}

	/**
	 * Get the distance that this ray collides with the AABB, a negative number indicates no collision
	 * @param r
	 */
	public double collisionDist(Ray r) {
		return collisionDist(r, 0);
	}


	public void setComp(Vec4d v, int i, double val) {
		if (i == 0) { v.x = val; return; }
		if (i == 1) { v.y = val; return; }
		if (i == 2) { v.z = val; return; }
		if (i == 3) { v.w = val; return; }
		assert(false);
		return ;
	}

	private double getComp(Vec3d v, int i) {
		if (i == 0) return v.x;
		if (i == 1) return v.y;
		if (i == 2) return v.z;
		assert(false);
		return 0;
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
			if (MathUtils.near(getComp(rayDir, axis), 0)) {
				continue; // The ray is parallel to the box in this axis
			}

			Vec4d faceNorm = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
			double faceDist = 0;
			if (getComp(rayDir, axis) > 0) {
				// Collides with the negative face
				setComp(faceNorm, axis, -1.0d);
				faceDist = -getComp(minPt, axis) - fudge;
			} else {
				setComp(faceNorm, axis, 1.0d);
				faceDist = getComp(maxPt, axis) + fudge;
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
			Vec3d contactPoint = r.getPointAtDist(rayCollisionDist);

			if (getComp(contactPoint, a1) < getComp(minPt, a1) - fudge ||
			    getComp(contactPoint, a1) > getComp(maxPt, a1) + fudge) {
				continue; // No contact
			}

			if (getComp(contactPoint, a2) < getComp(minPt, a2) - fudge ||
			    getComp(contactPoint, a2) > getComp(maxPt, a2) + fudge) {
				continue; // No contact
			}
			// Collision!
			return rayCollisionDist;
		}

		return -1.0;
	}

	private void updateCenterAndRadius() {
		center.add3(maxPt, minPt);
		center.scale3(0.5);

		radius.sub3(maxPt, minPt);
		radius.scale3(0.5);
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
	 */
	public PlaneTestResult testToPlane(Plane p) {
		if (_isEmpty) {
			return PlaneTestResult.EMPTY;
		}

		// Make sure the radius points in the same direction of the normal
		double effectiveRadius = 0.0d;
		effectiveRadius += radius.x * Math.abs(p.normal.x);
		effectiveRadius += radius.y * Math.abs(p.normal.y);
		effectiveRadius += radius.z * Math.abs(p.normal.z);

		double centerDist = p.getNormalDist(center);
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
