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

public class Vec2d {

public double x;
public double y;

/**
 * Construct a Vec2d initialized to (0,0);
 */
public Vec2d() {
	x = 0.0d;
	y = 0.0d;
}

/**
 * Construct a Vec2d initialized to (v.x, v.y);
 * @param v the Vec2d containing the initial values
 * @throws NullPointerException if v is null
 */
public Vec2d(Vec2d v) {
	x = v.x;
	y = v.y;
}

/**
 * Construct a Vec2d initialized to (x, y);
 * @param x the initial x value
 * @param y the initial y value
 */
public Vec2d(double x, double y) {
	this.x = x;
	this.y = y;
}

/**
 * Set this Vec2d with the values (v.x, v.y);
 * @param v the Vec2d containing the values
 * @throws NullPointerException if v is null
 */
public void set2(Vec3d v) {
	this.x = v.x;
	this.y = v.y;
}

/**
 * Set this Vec3d with the values (x, y);
 */
public void set2(double x, double y) {
	this.x = x;
	this.y = y;
}

/**
 * Add v to this Vec2d: this = this + v
 * @throws NullPointerException if v is null
 */
public void add2(Vec2d v) {
	this.x = this.x + v.x;
	this.y = this.y + v.y;
}

/**
 * Add v1 to v2 into this Vec2d: this = v1 + v2
 * @throws NullPointerException if v1 or v2 are null
 */
public void add2(Vec2d v1, Vec2d v2) {
	this.x = v1.x + v2.x;
	this.y = v1.y + v2.y;
}

/**
 * Returns a new Vec2d initialized to v1 + v2
 * @throws NullPointerException if v1 or v2 are null
 */
public static final Vec2d getAdd2(Vec2d v1, Vec2d v2) {
	Vec2d tmp = new Vec2d(v1);
	tmp.add2(v2);
	return tmp;
}

/**
 * Subtract v from this Vec2d: this = this - v
 * @throws NullPointerException if v is null
 */
public void sub2(Vec2d v) {
	this.x = this.x - v.x;
	this.y = this.y - v.y;
}

/**
 * Subtract v2 from v1 into this Vec2d: this = v1 - v2
 * @throws NullPointerException if v1 or v2 are null
 */
public void sub2(Vec2d v1, Vec2d v2) {
	this.x = v1.x - v2.x;
	this.y = v1.y - v2.y;
}

/**
 * Returns a new Vec2d initialized to v1 - v2
 * @throws NullPointerException if v1 or v2 are null
 */
public static final Vec2d getSub2(Vec2d v1, Vec2d v2) {
	Vec2d tmp = new Vec2d(v1);
	tmp.sub2(v2);
	return tmp;
}

/**
 * Multiply the elements of this Vec2d by v: this = this * v
 * @throws NullPointerException if v is null
 */
public void mul2(Vec2d v) {
	this.x = this.x * v.x;
	this.y = this.y * v.y;
}

/**
 * Multiply the elements of v1 and v2 into this Vec2d: this = v1 * v2
 * @throws NullPointerException if v1 or v2 are null
 */
public void mul2(Vec2d v1, Vec2d v2) {
	this.x = v1.x * v2.x;
	this.y = v1.y * v2.y;
}

/**
 * Returns a new Vec2d initialized to v1 * v2
 * @throws NullPointerException if v1 or v2 are null
 */
public static final Vec2d getMul2(Vec2d v1, Vec2d v2) {
	Vec2d tmp = new Vec2d(v1);
	tmp.mul2(v2);
	return tmp;
}

/**
 * Set this Vec2d to the minimum of this and v: this = min(this, v)
 * @throws NullPointerException if v is null
 */
public void min2(Vec2d v) {
	this.x = Math.min(this.x, v.x);
	this.y = Math.min(this.y, v.y);
}

/**
 * Set this Vec2d to the minimum of v1 and v2: this = min(v1, v2)
 * @throws NullPointerException if v is null
 */
public void min2(Vec2d v1, Vec2d v2) {
	this.x = Math.min(v1.x, v2.x);
	this.y = Math.min(v1.y, v2.y);
}

/**
 * Returns a new Vec2d initialized to min(v1, v2)
 * @throws NullPointerException if v1 or v2 are null
 */
public static final Vec2d getMin2(Vec2d v1, Vec2d v2) {
	Vec2d tmp = new Vec2d(v1);
	tmp.min2(v2);
	return tmp;
}

/**
 * Set this Vec2d to the maximum of this and v: this = max(this, v)
 * @throws NullPointerException if v is null
 */
public void max2(Vec2d v) {
	this.x = Math.max(this.x, v.x);
	this.y = Math.max(this.y, v.y);
}

/**
 * Set this Vec2d to the maximum of v1 and v2: this = max(v1, v2)
 * @throws NullPointerException if v is null
 */
public void max2(Vec2d v1, Vec2d v2) {
	this.x = Math.max(v1.x, v2.x);
	this.y = Math.max(v1.y, v2.y);
}

/**
 * Returns a new Vec2d initialized to max(v1, v2)
 * @throws NullPointerException if v1 or v2 are null
 */
public static final Vec2d getMax2(Vec2d v1, Vec2d v2) {
	Vec2d tmp = new Vec2d(v1);
	tmp.max2(v2);
	return tmp;
}

/**
 * Return the 2-component dot product of v1 and v2
 * Internal helper to help with dot, mag and magSquared
 */
private final double _dot2(Vec2d v1, Vec2d v2) {
	double ret;
	ret  = v1.x * v2.x;
	ret += v1.y * v2.y;
	return ret;
}

/**
 * Return the 2-component dot product of this Vec2d with v
 * @throws NullPointerException if v is null
 */
public double dot2(Vec2d v) {
	return _dot2(this, v);
}

/**
 * Return the 2-component magnitude of this Vec2d
 */
public double mag2() {
	return Math.sqrt(_dot2(this, this));
}

/**
 * Return the 2-component magnitude squared of this Vec2d
 */
public double magSquare2() {
	return _dot2(this, this);
}

/**
 * Returns whether the given magnitude can be used to normalize a Vec
 * @param mag
 * @return
 */
static final boolean nonNormalMag(double mag) {
	return mag == 0.0d || Double.isNaN(mag) || Double.isInfinite(mag);
}

/**
 * Normalize the first two components in-place
 *
 * If the Vec has a zero magnitude or contains NaN or Inf, this sets
 * all components but the last to zero, the last component is set to one.
 */
public void normalize2() {
	double mag = _dot2(this, this);
	if (nonNormalMag(mag)) {
		assert false;
		this.x = 0.0d;
		this.y = 1.0d;
		return;
	}

	mag = Math.sqrt(mag);
	this.x = this.x / mag;
	this.y = this.y / mag;
}

/**
 * Set the first two components to the normalized values of v
 *
 * If the Vec has a zero magnitude or contains NaN or Inf, this sets
 * all components but the last to zero, the last component is set to one.
 * @throws NullPointerException if v is null
 */
public void normalize2(Vec2d v) {
	double mag = _dot2(v, v);
	if (nonNormalMag(mag)) {
		assert false;
		this.x = 0.0d;
		this.y = 1.0d;
	}
	else {
		mag = Math.sqrt(mag);
		this.x = v.x / mag;
		this.y = v.y / mag;
	}
}

/**
 * Scale the first two components of this Vec: this = scale * this
 */
public void scale2(double scale) {
	this.x = this.x * scale;
	this.y = this.y * scale;
}

/**
 * Scale the first two components of v into this Vec: this = scale * v
 * @throws NullPointerException if v is null
 */
public void scale2(double scale, Vec2d v) {
	this.x = v.x * scale;
	this.y = v.y * scale;
}

/**
 * Returns a new Vec2d initialized to scale * v
 * @throws NullPointerException if v is null
 */
public static final Vec2d getScale2(double scale, Vec2d v) {
	Vec2d tmp = new Vec2d(v);
	tmp.scale2(scale);
	return tmp;
}

/**
 * Multiply v by m and store into this Vec: this = m x v
 * @throws NullPointerException if m or v are null
 */
public void mult2(Mat4d m, Vec2d v) {
	double _x = m.d00 * v.x + m.d01 * v.y;
	double _y = m.d10 * v.x + m.d11 * v.y;

	this.x = _x;
	this.y = _y;
}

/**
 * Multiply m by v and store into this Vec: this = v x m
 * @throws NullPointerException if m or v are null
 */
public void mult2(Vec2d v, Mat4d m) {
	double _x = v.x * m.d00 + v.y * m.d10;
	double _y = v.x * m.d01 + v.y * m.d11;

	this.x = _x;
	this.y = _y;
}
}
