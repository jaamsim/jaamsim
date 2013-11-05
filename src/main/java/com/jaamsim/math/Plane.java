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

public class Plane {

	public static Plane XY_PLANE = new Plane();

/**
 * The normal direction of the plane, should always be of unit length
 */
public final Vec3d normal = new Vec3d();
/**
 * The shortest distance from the plane to the origin, by normal direction (affects sign)
 */
private double _dist;

/**
 * Create a plane defined by a normal, and a closest distance to the origin
 * This is the storage format and similar to the common mathematical definition for a plane
 * @param norm
 * @param distance
 */
public Plane(Vec3d norm, double distance) {
	if (norm == null) {
		normal.set3(0.0d, 0.0d, 1.0d);
		_dist = distance;
		return;
	}
	this.set(norm, distance);
}

/**
 * By default return the XY plane
 */
public Plane() {
	normal.set3(0.0d, 0.0d, 1.0d);
	_dist = 0.0d;
}

/**
 * Create a plane defined by 3 points
 * @param p0
 * @param p1
 * @param p2
 */

public Plane(Vec3d p0, Vec3d p1, Vec3d p2) {
	this.set(p0, p1, p2);
}

public void set(Vec3d norm, double distance) {
	normal.normalize3(norm);
	_dist = distance;
}

public void set(Vec3d p0, Vec3d p1, Vec3d p2) {
	Vec3d v0 = new Vec3d();
	v0.sub3(p1, p0);

	Vec3d v1 = new Vec3d();
	v1.sub3(p2, p1);

	normal.cross3(v0, v1);
	normal.normalize3();
	_dist = normal.dot3(p0);
}

/**
 * Get the shortest distance from the plane to this point, effectively just a convenient dot product
 * @param point
 * @return
 */
public double getNormalDist(Vec3d point) {
	double dot = point.dot3(normal);
	return dot - _dist;
}

/**
 * Transform this plane by the coordinate transform 't'. Safe for self assignment
 * @param t - the Transform
 * @param out - the output
 */
public void transform(Transform t, Plane p, Vec3d temp) {
	Mat4d tmat = t.getMat4dRef();
	Vec3d closePoint = temp;

	// The point closest to the origin (need any point on the plane
	closePoint.scale3(p._dist, p.normal);
	// Now close point is the transformed point
	closePoint.multAndTrans3(tmat, closePoint);

	this.normal.mult3(tmat, p.normal);
	this.normal.normalize3();

	this._dist = this.normal.dot3(closePoint);

}

public boolean near(Plane p) {
	return normal.near3(p.normal) && MathUtils.near(_dist, p._dist);
}

@Override
public boolean equals(Object o) {
	if (!(o instanceof Plane)) return false;
	Plane p = (Plane)o;
	return normal.equals3(p.normal) && MathUtils.near(_dist, p._dist);
}

@Override
public int hashCode() {
	assert false : "hashCode not designed";
	return 42; // any arbitrary constant will do
}

/**
 * Get the distance along a ray that it collides with this plane, this can return
 * infinity if the ray is parallel
 * @param r
 * @return
 */
public double collisionDist(Ray r) {

	// cos = plane-Normal dot ray-direction
	double cos = -1 * normal.dot3(r.getDirRef());

	if (MathUtils.near(cos, 0.0)) {
		// The ray is nearly parallel to the plane, so no collision
		return Double.POSITIVE_INFINITY;
	}

	return ( normal.dot3(r.getStartRef()) - _dist ) / cos;

}

/**
 * Returns if ray 'r' collides with the back of the plane
 * @param r
 * @return
 */
public boolean backFaceCollision(Ray r) {

	return normal.dot3(r.getDirRef()) > 0;
}

} // class Plane
