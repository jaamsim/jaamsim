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

/**
 * A simple representation of a Ray in 3 space. Like all rays, it's a position and direction.
 * @author Matt Chudleigh
 *
 */
public class Ray {

	private Vec4d _start;
	private Vec4d _direction;

	public Ray() {
		_start = new Vec4d(0, 0, 0, 1.0d);
		_direction = new Vec4d(1, 0 ,0, 0);
	}

	public Ray(Vec4d start, Vec4d dir) {
		_start = new Vec4d(start);
		_direction = new Vec4d(dir);
		_direction.normalize3();
		_direction.w = 0; // Direction is a direction...
	}

	public Vec4d getStartRef() {
		return _start;
	}

	public Vec4d getDirRef() {
		return _direction;
	}

	/**
	 * Returns a new Ray as though this was passed through the Transform trans
	 * @param trans
	 */
	public Ray transform(Transform trans) {
		return transform(trans.getMat4dRef());
	}

	/**
	 * Returns a new Ray as though this was passed through the Matrix mat
	 * @param mat
	 */
	public Ray transform(Mat4d mat) {
		Vec4d startTransed = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		startTransed.mult4(mat, _start);

		Vec4d dirTransed = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		dirTransed.mult4(mat, _direction);
		dirTransed.normalize3();

		return new Ray(startTransed, dirTransed);
	}

	/**
	 * Returns a new vector4d representing the point 'dist' distance along this ray
	 * @param dist
	 */
	public Vec3d getPointAtDist(double dist) {
		Vec3d ret = new Vec3d(_direction);
		ret.scale3(dist);
		ret.add3(_start);
		return ret;
	}

	/**
	 * Returns the distance along the ray to the point on the ray closest to given point, this
	 * can be negative if the point given is effectively behind the ray
	 * @param point
	 */
	public double getDistAlongRay(Vec3d point) {
		Vec3d diff = new Vec3d(point);
		diff.sub3(_start);
		return diff.dot3(_direction);
	}

	@Override
	public String toString() {
		return "Orig: " + _start.toString() + " Dir: " + _direction.toString();
	}
}


