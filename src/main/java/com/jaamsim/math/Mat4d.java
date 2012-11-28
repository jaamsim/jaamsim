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

public class Mat4d {

public double d00, d01, d02, d03;
public double d10, d11, d12, d13;
public double d20, d21, d22, d23;
public double d30, d31, d32, d33;

/**
 * Construct a Mat4d initialized to the identity matrix
 */
public Mat4d() {
	_identity();
}

/**
 * Construct a Mat4d from the given Matrix
 * @throws NullPointerException if mat is null
 */
public Mat4d(Mat4d mat) {
	d00 = mat.d00; d01 = mat.d01; d02 = mat.d02; d03 = mat.d03;
	d10 = mat.d10; d11 = mat.d11; d12 = mat.d12; d13 = mat.d13;
	d20 = mat.d20; d21 = mat.d21; d22 = mat.d22; d23 = mat.d23;
	d30 = mat.d30; d31 = mat.d31; d32 = mat.d32; d33 = mat.d33;
}

/**
 * Construct a Mat4d from the given array (row-major order)
 * @throws NullPointerException if mat is null
 * @throws IllegalArgumentException if mat has fewer than 16 elements
 */
public Mat4d(double... mat) {
	if (mat.length < 16)
		throw new IllegalArgumentException("Fewer than 16 elements in argument");

	d00 = mat[ 0]; d01 = mat[ 1]; d02 = mat[ 2]; d03 = mat[ 3];
	d10 = mat[ 4]; d11 = mat[ 5]; d12 = mat[ 6]; d13 = mat[ 7];
	d20 = mat[ 8]; d21 = mat[ 9]; d22 = mat[10]; d23 = mat[11];
	d30 = mat[12]; d31 = mat[13]; d32 = mat[14]; d33 = mat[15];
}

/**
 * Internal helper to set the values to the identity matrix
 */
private final void _identity() {
	d00 = 1.0d; d01 = 0.0d; d02 = 0.0d; d03 = 0.0d;
	d10 = 0.0d; d11 = 1.0d; d12 = 0.0d; d13 = 0.0d;
	d20 = 0.0d; d21 = 0.0d; d22 = 1.0d; d23 = 0.0d;
	d30 = 0.0d; d31 = 0.0d; d32 = 0.0d; d33 = 1.0d;
}

/**
 * Internal helper to zero the entire matrix
 */
private final void _zero() {
	d00 = 0.0d; d01 = 0.0d; d02 = 0.0d; d03 = 0.0d;
	d10 = 0.0d; d11 = 0.0d; d12 = 0.0d; d13 = 0.0d;
	d20 = 0.0d; d21 = 0.0d; d22 = 0.0d; d23 = 0.0d;
	d30 = 0.0d; d31 = 0.0d; d32 = 0.0d; d33 = 0.0d;
}

/**
 * Set all values in the matrix to zero
 */
public void zero() {
	_zero();
}

/**
 * Set all values in the matrix to the identity matrix
 */
public void identity() {
	_identity();
}

/**
 * Transpose this matrix in-place
 */
public void transpose4() {
	double tmp;

	tmp = d01; d01 = d10; d10 = tmp;
	tmp = d02; d02 = d20; d20 = tmp;
	tmp = d03; d03 = d30; d30 = tmp;
	tmp = d12; d12 = d21; d21 = tmp;
	tmp = d13; d13 = d31; d31 = tmp;
	tmp = d23; d23 = d32; d32 = tmp;

}

/**
 * Set this matrix to the transpose of the given matrix
 * @throws NullPointerException if m is null
 */
public void transpose4(Mat4d m) {
	// We can safely set the diagonal, even if m is 'this'
	this.d00 = m.d00; this.d11 = m.d11; this.d22 = m.d22; this.d33 = m.d33;

	// Be careful in case this was passed to itself
	double tmp;
	tmp = m.d01; this.d01 = m.d10; this.d10 = tmp;
	tmp = m.d02; this.d02 = m.d20; this.d20 = tmp;
	tmp = m.d03; this.d03 = m.d30; this.d30 = tmp;
	tmp = m.d12; this.d12 = m.d21; this.d21 = tmp;
	tmp = m.d13; this.d13 = m.d31; this.d31 = tmp;
	tmp = m.d23; this.d23 = m.d32; this.d32 = tmp;
}

/**
 * Sets the upper 3x3 of this matrix by multiplying m1 with m2: this = m1 x m2
 * @throws NullPointerException if m1 or m2 are null
 */
private final void _mul3(Mat4d m1, Mat4d m2) {
	// Do everything in temp vars in case m1 or m2 == this
	double _d00, _d01, _d02;
	_d00 = m1.d00 * m2.d00 + m1.d01 * m2.d10 + m1.d02 * m2.d20;
	_d01 = m1.d00 * m2.d01 + m1.d01 * m2.d11 + m1.d02 * m2.d21;
	_d02 = m1.d00 * m2.d02 + m1.d01 * m2.d12 + m1.d02 * m2.d22;

	double _d10, _d11, _d12;
	_d10 = m1.d10 * m2.d00 + m1.d11 * m2.d10 + m1.d12 * m2.d20;
	_d11 = m1.d10 * m2.d01 + m1.d11 * m2.d11 + m1.d12 * m2.d21;
	_d12 = m1.d10 * m2.d02 + m1.d11 * m2.d12 + m1.d12 * m2.d22;

	double _d20, _d21, _d22;
	_d20 = m1.d20 * m2.d00 + m1.d21 * m2.d10 + m1.d22 * m2.d20;
	_d21 = m1.d20 * m2.d01 + m1.d21 * m2.d11 + m1.d22 * m2.d21;
	_d22 = m1.d20 * m2.d02 + m1.d21 * m2.d12 + m1.d22 * m2.d22;

	this.d00 = _d00; this.d01 = _d01; this.d02 = _d02;
	this.d10 = _d10; this.d11 = _d11; this.d12 = _d12;
	this.d20 = _d20; this.d21 = _d21; this.d22 = _d22;
}

/**
 * Sets the upper 3x3 of this matrix by multiplying this with m: this = this x m
 * @throws NullPointerException if m is null
 */
public void mul3(Mat4d m) {
	_mul3(this, m);
}

/**
 * Sets the upper 3x3 of this matrix by multiplying m1 with m2: this = m1 x m2
 * @throws NullPointerException if m1 or m2 are null
 */
public void mul3(Mat4d m1, Mat4d m2) {
	_mul3(m1, m2);
}

/**
 * Sets the matrix by multiplying m1 with m2: this = m1 x m2
 * @throws NullPointerException if m1 or m2 are null
 */
private final void _mul4(Mat4d m1, Mat4d m2) {
	double _d00, _d01, _d02, _d03;
	_d00 = m1.d00 * m2.d00 + m1.d01 * m2.d10 + m1.d02 * m2.d20 + m1.d03 * m2.d30;
	_d01 = m1.d00 * m2.d01 + m1.d01 * m2.d11 + m1.d02 * m2.d21 + m1.d03 * m2.d31;
	_d02 = m1.d00 * m2.d02 + m1.d01 * m2.d12 + m1.d02 * m2.d22 + m1.d03 * m2.d32;
	_d03 = m1.d00 * m2.d03 + m1.d01 * m2.d13 + m1.d02 * m2.d23 + m1.d03 * m2.d33;

	double _d10, _d11, _d12, _d13;
	_d10 = m1.d10 * m2.d00 + m1.d11 * m2.d10 + m1.d12 * m2.d20 + m1.d13 * m2.d30;
	_d11 = m1.d10 * m2.d01 + m1.d11 * m2.d11 + m1.d12 * m2.d21 + m1.d13 * m2.d31;
	_d12 = m1.d10 * m2.d02 + m1.d11 * m2.d12 + m1.d12 * m2.d22 + m1.d13 * m2.d32;
	_d13 = m1.d10 * m2.d03 + m1.d11 * m2.d13 + m1.d12 * m2.d23 + m1.d13 * m2.d33;

	double _d20, _d21, _d22, _d23;
	_d20 = m1.d20 * m2.d00 + m1.d21 * m2.d10 + m1.d22 * m2.d20 + m1.d23 * m2.d30;
	_d21 = m1.d20 * m2.d01 + m1.d21 * m2.d11 + m1.d22 * m2.d21 + m1.d23 * m2.d31;
	_d22 = m1.d20 * m2.d02 + m1.d21 * m2.d12 + m1.d22 * m2.d22 + m1.d23 * m2.d32;
	_d23 = m1.d20 * m2.d03 + m1.d21 * m2.d13 + m1.d22 * m2.d23 + m1.d23 * m2.d33;

	double _d30, _d31, _d32, _d33;
	_d30 = m1.d30 * m2.d00 + m1.d31 * m2.d10 + m1.d32 * m2.d20 + m1.d33 * m2.d30;
	_d31 = m1.d30 * m2.d01 + m1.d31 * m2.d11 + m1.d32 * m2.d21 + m1.d33 * m2.d31;
	_d32 = m1.d30 * m2.d02 + m1.d31 * m2.d12 + m1.d32 * m2.d22 + m1.d33 * m2.d32;
	_d33 = m1.d30 * m2.d03 + m1.d31 * m2.d13 + m1.d32 * m2.d23 + m1.d33 * m2.d33;

	this.d00 = _d00; this.d01 = _d01; this.d02 = _d02; this.d03 = _d03;
	this.d10 = _d10; this.d11 = _d11; this.d12 = _d12; this.d13 = _d13;
	this.d20 = _d20; this.d21 = _d21; this.d22 = _d22; this.d23 = _d23;
	this.d30 = _d30; this.d31 = _d31; this.d32 = _d32; this.d33 = _d33;
}

/**
 * Sets the matrix by multiplying this with m: this = this x m
 * @throws NullPointerException if m is null
 */
public void mul4(Mat4d m) {
	_mul4(this, m);
}

/**
 * Sets the matrix by multiplying m1 with m2: this = m1 x m2
 * @throws NullPointerException if m1 or m2 are null
 */
public void mul4(Mat4d m1, Mat4d m2) {
	_mul4(m1, m2);
}

/**
 * Fill the upper 3x3 with a rotation represented by the Quarternion q
 * @throws NullPointerException if q is null
 */
private final void _rot3(Quaternion q) {
	double xsq = q.x * q.x;
	double ysq = q.y * q.y;
	double zsq = q.z * q.z;
	double wsq = q.w * q.w;

	d00 = wsq + xsq - ysq - zsq;
	d01 = 2.0d * (q.y * q.x - q.w * q.z);
	d02 = 2.0d * (q.z * q.x + q.w * q.y);

	d10 = 2.0d * (q.x * q.y + q.w * q.z);
	d11 = wsq - xsq + ysq - zsq;
	d12 = 2.0d * (q.z * q.y - q.w * q.x);

	d20 = 2.0d * (q.x * q.z - q.w * q.y);
	d21 = 2.0d * (q.y * q.z + q.w * q.x);
	d22 = wsq - xsq - ysq + zsq;
}

/**
 * Sets the upper 3x3 the the rotation represented by the Quarternion q
 * @throws NullPointerException if q is null
 */
public void setRot3(Quaternion q) {
	_rot3(q);
}

/**
 * Sets the upper 3x3 the the rotation represented by the Quarternion q
 *
 * Clears the remaining elements to the identity matrix
 * @throws NullPointerException if q is null
 */
public void setRot4(Quaternion q) {
	_rot3(q);
	d03 = 0.0d; d13 = 0.0d; d23 = 0.0d;
	d30 = 0.0d; d31 = 0.0d; d32 = 0.0d;
	d33 = 1.0d;
}

/**
 * Sets the 3 translation components without modifying any other componenets
 * @throws NullPointerException if v is null
 */
public void setTrans3(Vec3d v) {
	this.d03 = v.x;
	this.d13 = v.y;
	this.d23 = v.z;
}
}
