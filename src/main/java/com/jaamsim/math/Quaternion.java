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
 * The identity quaternion to avoid allocating this excessively
 */
public static Quaternion ident;

static {
	ident = new Quaternion();
}

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
public static Quaternion Rotation(double angle, Vector4d axis) {
	Vec3d v = new Vec3d(axis.data[0], axis.data[1], axis.data[2]);
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
	Quaternion ret = Rotation(x, Vector4d.X_AXIS);

	Rotation(y, Vector4d.Y_AXIS).mult(ret, ret);
	Rotation(z, Vector4d.Z_AXIS).mult(ret, ret);
	return ret;
}

/**
 * Create a quaternion from Euler angles, specifically the kind of euler angles used by Java3d
 * (which seems to be rotation around global x, then y, then z
 * @return
 */
public static Quaternion FromEuler(Vector4d rot) {
	return Quaternion.FromEuler(rot.x(), rot.y(), rot.z());
}

/**
 * Factory that returns a Quaternion that would rotate one direction into the other.
 * Only valid for vectors of the same length
 * @param from
 * @param to
 * @return
 */
public static Quaternion transformVectors(Vector4d from, Vector4d to) {

	Vector4d f = new Vector4d(from);
	Vector4d t = new Vector4d(to);

	f.normalizeLocal3();
	t.normalizeLocal3();

	Vector4d cross = new Vector4d();
	f.cross(t, cross);

	double angle = Math.asin(cross.mag3());
	cross.normalizeLocal3();

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
 * Quaternion multiplication, mathematically equivalent to applying both rotations in order
 * @param q
 * @param res
 */
public void mult(Quaternion rhs, Quaternion res) {
	double _x = w*rhs.x + x*rhs.w + y*rhs.z - z*rhs.y;
	double _y = w*rhs.y + y*rhs.w + z*rhs.x - x*rhs.z;
	double _z = w*rhs.z + z*rhs.w + x*rhs.y - y*rhs.x;
	double _w = w*rhs.w - x*rhs.x - y*rhs.y - z*rhs.z;

	res.x = _x;
	res.y = _y;
	res.z = _z;
	res.w = _w;
}

public boolean isNormal() {
	double magSquared = magSquared();
	return MathUtils.near(magSquared, 1.0);
}

public void toRotationMatrix(Matrix4d res) {
	res.data[ 0] = w*w + x*x - y*y - z*z;
	res.data[ 1] = 2*x*y + 2*w*z;
	res.data[ 2] = 2*x*z - 2*w*y;
	res.data[ 3] = 0;

	res.data[ 4] = 2*y*x - 2*w*z;
	res.data[ 5] = w*w - x*x + y*y - z*z;
	res.data[ 6] = 2*y*z + 2*w*x;
	res.data[ 7] = 0;

	res.data[ 8] = 2*z*x + 2*w*y;
	res.data[ 9] = 2*z*y - 2*w*x;
	res.data[10] = w*w - x*x - y*y + z*z;
	res.data[11] = 0;

	res.data[12] = 0;
	res.data[13] = 0;
	res.data[14] = 0;
	res.data[15] = 1;
}

public void rotateVector(Vector4d vect, Vector4d res) {
	Matrix4d mat = new Matrix4d();
	toRotationMatrix(mat);
	mat.mult(vect, res);
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

public boolean equals(Quaternion other) {
	if (!MathUtils.near(x, other.x)) return false;
	if (!MathUtils.near(y, other.y)) return false;
	if (!MathUtils.near(z, other.z)) return false;
	if (!MathUtils.near(w, other.w)) return false;

	return true;
}

public String toString()
{
	return "[(" + x + ", "  + y + ", "  + z + ")i, "  + w + "]";
}

} // class Quaternion
