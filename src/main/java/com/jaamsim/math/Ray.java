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

	private Vector4d _start;
	private Vector4d _direction;

	public Ray() {
		_start = new Vector4d(0, 0, 0);
		_direction = new Vector4d(1, 0 ,0, 0);
	}

	public Ray(Vector4d start, Vector4d dir) {
		_start = new Vector4d(start);
		_direction = new Vector4d(dir);
		_direction.normalizeLocal3();
		_direction.data[3] = 0; // Direction is a direction...
	}

	public Vector4d getStartRef() {
		return _start;
	}

	public Vector4d getDirRef() {
		return _direction;
	}

	/**
	 * Returns a new Ray as though this was passed through the Transform trans
	 * @param trans
	 * @return
	 */
	public Ray transform(Transform trans) {
		return transform(trans.getMatrixRef());
	}

	/**
	 * Returns a new Ray as though this was passed through the Matrix mat
	 * @param mat
	 * @return
	 */
	public Ray transform(Matrix4d mat) {
		Vector4d startTransed = new Vector4d();
		mat.mult(_start, startTransed);

		Vector4d dirTransed = new Vector4d();
		mat.mult(_direction, dirTransed);
		dirTransed.normalizeLocal3();

		return new Ray(startTransed, dirTransed);
	}

	/**
	 * Returns a new vector4d representing the point 'dist' distance along this ray
	 * @param dist
	 * @return
	 */
	public Vector4d getPointAtDist(double dist) {
		Vector4d ret = new Vector4d();
		_direction.scale3(dist, ret);
		ret.add3(_start, ret);
		return ret;
	}
	@Override
	public String toString() {
		return "Orig: " + _start.toString() + " Dir: " + _direction.toString();
	}
}


