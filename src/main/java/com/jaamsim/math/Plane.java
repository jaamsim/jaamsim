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

public class Plane {
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
 * Transform plane p by the coordinate transform and store the result in 'this'
 * @param t - the Transform
 * @param p - the plane to transform
 */
public void transform(Transform t, Plane p) {
	transform(t.getMat4dRef(), t.getMat4dRef(), p);
}

/**
 * Transform plane p by the coordinate transform matrix 'mat' and store the result in 'this'
 * @param mat - the Transform matrix
 * @param normalMat - the normal matrix, should be the inverse transpose of 'mat' or equal to 'mat' if there is no non-uniform scaling
 * @param p - the plane to transform
 */
public void transform(Mat4d mat, Mat4d normalMat, Plane p) {

	Vec3d closePoint = new Vec3d();

	// The point closest to the origin (need any point on the plane
	closePoint.scale3(p._dist, p.normal);
	// Now close point is the transformed point
	closePoint.multAndTrans3(mat, closePoint);

	this.normal.mult3(normalMat, p.normal);
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
	//assert false : "hashCode not designed";
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

// Return the 'ray' resulting from colliding two planes, or null if the planes are parallel
public Ray collide(Plane p) {
	double normDot = normal.dot3(p.normal);
	if (MathUtils.near(normDot, 1.0) || MathUtils.near(normDot, -1.0)) {
		return null; // These planes are parallel
	}

	// Ray dir is the direction of the new ray
	Vec3d rayDir = new Vec3d();
	rayDir.cross3(normal, p.normal);
	rayDir.normalize3();

	// Now, we need a point on both planes, so we will find any ray in this plane and intersect it with the other
	Vec3d intDir = new Vec3d();
	intDir.cross3(rayDir, normal); // Take the cross of our new direction and the normal, this must be in the plane
	intDir.normalize3();
	Vec3d intStart = new Vec3d(normal);
	intStart.scale3(_dist); // intStart is in this plane
	Ray intersectRay = new Ray(new Vec4d(intStart, 1.0),
	                           new Vec4d(intDir, 0.0));
	double intDist = p.collisionDist(intersectRay);
	Vec3d intPoint = new Vec3d(intDir);
	intPoint.scale3(intDist);
	intPoint.add3(intStart);

	return new Ray(new Vec4d(intPoint, 1.0),
	               new Vec4d(rayDir, 0.0));
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
