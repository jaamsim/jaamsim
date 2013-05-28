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
 * Quaternion class, stored as an array of 4 doubles
 * @author Matt Chudleigh
 *
 */
public class Quaternion {

public double x;
public double y;
public double z;
public double w;

/**
 * Default returns an identity quaternion (real = 1, imaginary = 0)
 */
public Quaternion() {
	x = 0.0d;
	y = 0.0d;
	z = 0.0d;
	w = 1.0d;
}

/**
 * Quaternion constructor with explicit data, may need to be explicitly normalized
 * @param ix
 * @param iy
 * @param iz
 * @param r
 */
public Quaternion(double ix, double iy, double iz, double r) {
	x = ix;
	y = iy;
	z = iz;
	w = r;
}

public Quaternion(Quaternion q) {
	x = q.x;
	y = q.y;
	z = q.z;
	w = q.w;
}

/**
 * Set this Quaternion with the values (q.x, q.y, q.z, q.w);
 * @param q the Quaternion containing the values
 * @throws NullPointerException if q is null
 */
public void set(Quaternion q) {
	this.x = q.x;
	this.y = q.y;
	this.z = q.z;
	this.w = q.w;
}

/**
 * Factory that creates a quaternion representing a rotation, created in axis angle representation
 * @param angle
 * @param axis
 * @return
 */
public static Quaternion Rotation(double angle, Vec4d axis) {
	Vec3d v = new Vec3d(axis.x, axis.y, axis.z);
	v.normalize3();
	v.scale3(Math.sin(angle/2.0));

	return new Quaternion(v.x, v.y, v.z, Math.cos(angle / 2.0d));
}

/**
 * Factory that creates a quaternion representing a rotation, created in axis angle representation
 * @param angle
 * @param axis
 * @return
 */
public static Quaternion Rotation(double angle, Vec3d axis) {
	Vec3d v = new Vec3d(axis);
	v.normalize3();
	v.scale3(Math.sin(angle/2.0));

	return new Quaternion(v.x, v.y, v.z, Math.cos(angle / 2.0d));
}

/**
 * Create a quaternion from Euler angles, specifically the kind of euler angles used by Java3d
 * (which seems to be rotation around global x, then y, then z
 * @param x - rotation in radians
 * @param y
 * @param z
 * @return
 */
public static Quaternion FromEuler(double x, double y, double z) {
	// This will almost certainly be a performance bottleneck before too long
	Quaternion ret = Rotation(x, Vec4d.X_AXIS);

	ret.mult(Rotation(y, Vec4d.Y_AXIS), ret);
	ret.mult(Rotation(z, Vec4d.Z_AXIS), ret);
	return ret;
}

/**
 * Create a quaternion from Euler angles, specifically the kind of euler angles used by Java3d
 * (which seems to be rotation around global x, then y, then z
 * @return
 */
public static Quaternion FromEuler(Vec4d rot) {
	return Quaternion.FromEuler(rot.x, rot.y, rot.z);
}

/**
 * Factory that returns a Quaternion that would rotate one direction into the other.
 * Only valid for vectors of the same length
 * @param from
 * @param to
 * @return
 */
public static Quaternion transformVectors(Vec4d from, Vec4d to) {

	Vec4d f = new Vec4d(from);
	Vec4d t = new Vec4d(to);

	f.normalize3();
	t.normalize3();

	Vec4d cross = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
	cross.cross3(f, t);

	double angle = Math.asin(cross.mag3());
	cross.normalize3();

	return Rotation(angle, cross);
}

private final double _dot4(Quaternion q1, Quaternion q2) {
	double ret;
	ret  = q1.x * q2.x;
	ret += q1.y * q2.y;
	ret += q1.z * q2.z;
	ret += q1.w * q2.w;
	return ret;
}

public double magSquared() {
	return _dot4(this, this);
}

public double mag() {
	return Math.sqrt(_dot4(this, this));
}

/**
 * Normalize the quaternion in place
 */
public void normalize() {
	double mag = _dot4(this, this);
	if (mag < Constants.EPSILON) { // The quaternion is of length 0, simply return an identity
		this.x = 0.0d;
		this.y = 0.0d;
		this.z = 0.0d;
		this.w = 1.0d;
		return;
	}

	mag = Math.sqrt(mag);
	this.x = this.x / mag;
	this.y = this.y / mag;
	this.z = this.z / mag;
	this.w = this.w / mag;
}

/**
 * Set this quaternion to the normalized value of q
 * @throws NullPointerException if q is null
 */
public void normalize(Quaternion q) {
	double mag = _dot4(q, q);
	if (mag < Constants.EPSILON) { // The quaternion is of length 0, simply return an identity
		this.x = 0.0d;
		this.y = 0.0d;
		this.z = 0.0d;
		this.w = 1.0d;
		return;
	}

	mag = Math.sqrt(mag);
	this.x = q.x / mag;
	this.y = q.y / mag;
	this.z = q.z / mag;
	this.w = q.w / mag;
}

/**
 * Set this Quarternion to its complex conjugate
 */
public void conjugate() {
	x *= -1.0d;
	y *= -1.0d;
	z *= -1.0d;
}

/**
 * Set this Quarternion to the complex conjugate of q
 * @throws NullPointerException if q is null
 */
public void conjugate(Quaternion q) {
	this.x = q.x * -1.0d;
	this.y = q.y * -1.0d;
	this.z = q.z * -1.0d;
	this.w = q.w;
}

/**
 * Quaternion multiplication, mathematically equivalent to applying both rotations in order.
 * Sets this to a*b
 * @param q
 * @param res
 */
public void mult(Quaternion a, Quaternion b) {
	double _x = a.w*b.x + a.x*b.w + a.y*b.z - a.z*b.y;
	double _y = a.w*b.y + a.y*b.w + a.z*b.x - a.x*b.z;
	double _z = a.w*b.z + a.z*b.w + a.x*b.y - a.y*b.x;
	double _w = a.w*b.w - a.x*b.x - a.y*b.y - a.z*b.z;

	x = _x;
	y = _y;
	z = _z;
	w = _w;
}

public boolean isNormal() {
	double magSquared = magSquared();
	return MathUtils.near(magSquared, 1.0);
}

public void rotateVector(Vec4d vect, Vec4d res) {
	Mat4d mat = new Mat4d();
	mat.setRot4(this);
	res.mult4(mat, vect);
}

public double dot(Quaternion q) {
	return _dot4(this, q);
}

/**
 * Weighted linear interpolation between quaternions when
 * weight = 1 -> res = q
 * weight = 0 -> res = this
 * @param q - the other quaternion
 * @param weight - the weight to blend with
 * @param res - the result
 */
public void lerp(Quaternion q, double weight, Quaternion res) {
	double weight1 = 1.0d - weight;
	res.x = x * weight1 + q.x * weight;
	res.y = y * weight1 + q.y * weight;
	res.z = z * weight1 + q.z * weight;
	res.w = w * weight1 + q.w * weight;
}

/**
 * Spherical linear interpolation between quaternions, look up slerp if you are unsure
 * weight = 1 -> res = q
 * weight = 0 -> res = this
 * @param q - the other quaternion
 * @param weight - the weight to blend with
 * @param res - the result
 */
public void slerp(Quaternion q, double weight, Quaternion res) {
	double cosTheta = dot(q);
	if (cosTheta > 0.95) { // close enough, just lerp it
		lerp(q, weight, res);
		res.normalize();
		return;
	}

	double theta = Math.acos(cosTheta);
	double sinTheta = Math.sin(theta);

	if (sinTheta < Constants.EPSILON) {
		// TODO: some kind of decent default as the two quaternions are nearly opposite
		throw new IllegalArgumentException("Cannot slerp two opposite quaternions");
	}
	double thisScale = Math.sin((1.0 - weight)*theta) / sinTheta;
	double qScale = Math.sin(weight*theta) / sinTheta;

	res.x = x * thisScale + q.x * qScale;
	res.y = y * thisScale + q.y * qScale;
	res.z = z * thisScale + q.z * qScale;
	res.w = w * thisScale + q.w * qScale;
}

@Override
public boolean equals(Object o) {
	if (!(o instanceof Quaternion)) return false;
	Quaternion q = (Quaternion)o;

	if (!MathUtils.near(x, q.x)) return false;
	if (!MathUtils.near(y, q.y)) return false;
	if (!MathUtils.near(z, q.z)) return false;
	if (!MathUtils.near(w, q.w)) return false;

	return true;
}

@Override
public int hashCode() {
	assert false : "hashCode not designed";
	return 42; // any arbitrary constant will do
}

@Override
public String toString()
{
	return "[(" + x + ", "  + y + ", "  + z + ")i, "  + w + "]";
}

} // class Quaternion
