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

/**
 * Some handy static methods to make life easier else where
 * @author Matt Chudleigh
 *
 */
public class MathUtils {

public static boolean near(double a, double b) {
	double diff = Math.abs(a - b);
	return diff < Constants.EPSILON;
}

/**
 * Checks for line segment overlap
 * @param a0
 * @param a1
 * @param b0
 * @param b1
 */
public static boolean segOverlap(double a0, double a1, double b0, double b1) {
	if (a0 == b0) return true;

	if (a0 < b0) {
		return b0 <= a1;
	}
	return a0 <= b1;
}

/**
 * Checks for line segment overlap, with a fudge factor
 * @param a0
 * @param a1
 * @param b0
 * @param b1
 * @param fudge - The fudge factor to allow
 */
public static boolean segOverlap(double a0, double a1, double b0, double b1, double fudge) {
	if (a0 == b0) return true;

	if (a0 < b0) {
		return b0 <= a1 + fudge;
	}
	return a0 <= b1 + fudge;
}

/**
 * Perform a bounds check on val, returns something in the range [min, max]
 * @param val
 * @param min
 * @param max
 * @return
 */
public static double bound(double val, double min, double max) {
	//assert(min <= max);

	if (val < min) { return min; }
	if (val > max) { return max; }
	return val;
}

/**
 * Return a matrix that rotates points and projects them onto the ray's view plane.
 * IE: the new coordinate system has the ray pointing in the +Z direction from the origin.
 * This is useful for ray-line collisions and ray-point collisions
 * @return
 */
public static Mat4d RaySpace(Ray r) {

	// Create a new orthonormal basis starting with the y-axis, if the ray is
	// nearly parallel to Y, build our new basis from X instead
	Vec3d t = new Vec3d(0.0d, 1.0d, 0.0d);
	double dist = Math.abs(t.dot3(r.getDirRef()));
	if (MathUtils.near(dist, 1.0d))
		t.set3(1.0d, 0.0d, 0.0d);

	Mat4d ret = new Mat4d();

	// Calculate a new basis to populate the rows of the return matrix
	t.cross3(r.getDirRef(), t);
	t.normalize3();
	ret.d00 = t.x; ret.d01 = t.y; ret.d02 = t.z;

	t.cross3(r.getDirRef(), t);
	t.normalize3();
	ret.d10 = t.x; ret.d11 = t.y; ret.d12 = t.z;

	t.set3(r.getDirRef());
	ret.d20 = t.x; ret.d21 = t.y; ret.d22 = t.z;

	// Now use this rotation matrix to calculate the rotated translation part
	t.mult3(ret, r.getStartRef());
	ret.d03 = -t.x; ret.d13 = -t.y; ret.d23 = -t.z;

	return ret;
}

/**
 * Returns a Transform representing a rotation around a non-origin point
 * @param rot - the rotation (in world coordinates) to apply
 * @param point - the point to rotate around
 * @return
 */
public static Transform rotateAroundPoint(Quaternion rot, Vec4d point) {
	Vec4d negPoint = new Vec4d(point);
	negPoint.scale3(-1);

	Transform ret = new Transform(point);
	ret.merge(ret, new Transform(Vec4d.ORIGIN, rot, 1));
	ret.merge(ret, new Transform(negPoint));
	return ret;
}

} // class
