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
	 * The normal direction of the plane, should always have w = 0 and be of unit length
	 */
private final Vec4d _normal = new Vec4d();
/**
 * The shortest distance from the plane to the origin, by normal direction (affects sign)
 */
private double _dist;

/**
 * Create a plane defined by a normal, and a closest distance to the origin
 * This is the storage format and similar to the common mathematical definition for a plane
 * @param normal
 * @param distance
 */
public Plane(Vec4d normal, double distance) {
	this.set(normal, distance);
}

/**
 * By default return the XY plane
 */
public Plane() {
	_normal.set4(0.0d, 0.0d, 1.0d, 0.0d);
	_dist = 0.0d;
}

/**
 * Create a plane defined by 3 points
 * @param p0
 * @param p1
 * @param p2
 */

public Plane(Vec4d p0, Vec4d p1, Vec4d p2) {
	Vec4d v0 = new Vec4d(p1);
	v0.sub3(p0);
	v0.normalize3();

	Vec4d v1 = new Vec4d(p2);
	v1.sub3(p1);
	v1.normalize3();

	_normal.cross3(v0, v1);
	_normal.normalize3();
	_normal.w = 0;
	_dist = _normal.dot3(p0);
}

public void set(Vec3d normal, double distance) {
	_normal.normalize3(normal);
	_normal.w = 0.0d;

	_dist = distance;
}

public void getNormal(Vec4d norm) {
	norm.set4(_normal);
}

public Vec4d getNormalRef() {
	return _normal;
}

public double getDist() {
	return _dist;
}

/**
 * Get the shortest distance from the plane to this point, effectively just a convenient dot product
 * @param point
 * @return
 */
public double getNormalDist(Vec3d point) {
	double dot = point.dot3(_normal);
	return dot - _dist;
}

/**
 * Transform this plane by the coordinate transform 't'. Safe for self assignment
 * @param t - the Transform
 * @param out - the output
 */
public void transform(Transform t, Plane out) {
	transform(t, out, new Vec4d());
}

public void transform(Transform t, Plane out, Vec4d temp) {

	Vec4d closePoint = temp;

	closePoint.scale3(_dist, _normal); // The point closest to the origin (need any point on the plane
	closePoint.w = 1;

	t.apply(_normal, out._normal);
	out._normal.normalize3();

	// Now close point is the transformed point
	t.apply(closePoint, closePoint);

	out._dist = out._normal.dot3(closePoint);

}

public boolean near(Plane p) {
	return _normal.near4(p._normal) && MathUtils.near(_dist, p._dist);
}

@Override
public boolean equals(Object o) {
	if (!(o instanceof Plane)) return false;
	Plane p = (Plane)o;
	return _normal.equals4(p._normal) && MathUtils.near(_dist, p._dist);
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
	double cos = -1 * _normal.dot3(r.getDirRef());

	if (MathUtils.near(cos, 0.0)) {
		// The ray is nearly parallel to the plane, so no collision
		return Double.POSITIVE_INFINITY;
	}

	return ( _normal.dot3(r.getStartRef()) - _dist ) / cos;

}

/**
 * Returns if ray 'r' collides with the back of the plane
 * @param r
 * @return
 */
public boolean backFaceCollision(Ray r) {

	return _normal.dot3(r.getDirRef()) > 0;
}

} // class Plane
