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
	 * @return
	 */
	public Ray transform(Transform trans) {
		return transform(trans.getMat4dRef());
	}

	/**
	 * Returns a new Ray as though this was passed through the Matrix mat
	 * @param mat
	 * @return
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
	 * @return
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
	 * @return
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


