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

	// Create a new orthonormal basis.
	Vec4d basisSeed = Vec4d.Y_AXIS;
	if (MathUtils.near(basisSeed.dot3(r.getDirRef()), 1) ||
		MathUtils.near(basisSeed.dot3(r.getDirRef()), -1)) {
		// The ray is nearly parallel to Y
		basisSeed = Vec4d.X_AXIS; // So let's build our new basis from X instead
	}

	Vec4d[] newBasis = new Vec4d[3];
	newBasis[0] = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
	newBasis[0].cross3(r.getDirRef(), basisSeed);
	newBasis[0].normalize3();
	newBasis[0].w = 0;

	newBasis[1] = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
	newBasis[1].cross3(r.getDirRef(), newBasis[0]);
	newBasis[1].normalize3();
	newBasis[1].w = 0;

	newBasis[2] = r.getDirRef();

	Mat4d ret = new Mat4d();
	// Use the new basis to populate the rows of the return matrix
	ret.d00 = newBasis[0].x;
	ret.d01 = newBasis[0].y;
	ret.d02 = newBasis[0].z;

	ret.d10 = newBasis[1].x;
	ret.d11 = newBasis[1].y;
	ret.d12 = newBasis[1].z;

	ret.d20 = newBasis[2].x;
	ret.d21 = newBasis[2].y;
	ret.d22 = newBasis[2].z;

	ret.d33 = 1;

	// Now use this rotation matrix to calculate the rotated translation part
	Vec4d newTrans = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
	newTrans.mult4(ret, r.getStartRef());

	ret.d03 = -newTrans.x;
	ret.d13 = -newTrans.y;
	ret.d23 = -newTrans.z;

	return ret;
}

} // class
