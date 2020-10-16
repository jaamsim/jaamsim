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
 * Set all elements of this matrix to m.
 * @throws NullPointerException if m is null
 */
public void set4(Mat4d m) {
	d00 = m.d00; d01 = m.d01; d02 = m.d02; d03 = m.d03;
	d10 = m.d10; d11 = m.d11; d12 = m.d12; d13 = m.d13;
	d20 = m.d20; d21 = m.d21; d22 = m.d22; d23 = m.d23;
	d30 = m.d30; d31 = m.d31; d32 = m.d32; d33 = m.d33;
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
private final void _mult3(Mat4d m1, Mat4d m2) {
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
public void mult3(Mat4d m) {
	_mult3(this, m);
}

/**
 * Sets the upper 3x3 of this matrix by multiplying m1 with m2: this = m1 x m2
 * @throws NullPointerException if m1 or m2 are null
 */
public void mult3(Mat4d m1, Mat4d m2) {
	_mult3(m1, m2);
}

/**
 * Sets the matrix by multiplying m1 with m2: this = m1 x m2
 * @throws NullPointerException if m1 or m2 are null
 */
private final void _mult4(Mat4d m1, Mat4d m2) {
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
public void mult4(Mat4d m) {
	_mult4(this, m);
}

/**
 * Sets the matrix by multiplying m1 with m2: this = m1 x m2
 * @throws NullPointerException if m1 or m2 are null
 */
public void mult4(Mat4d m1, Mat4d m2) {
	_mult4(m1, m2);
}

/**
 * Fill the upper 3x3 with a rotation represented by the Quaternion q
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
 * Sets the upper 3x3 the the rotation represented by the Quaternion q
 * @throws NullPointerException if q is null
 */
public void setRot3(Quaternion q) {
	_rot3(q);
}

/**
 * Sets the upper 3x3 the the rotation represented by the Quaternion q
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
 * Fill the upper 3x3 with a rotation specified as 3 independent euler
 * rotations.
 * @throws NullPointerException if v is null
 */
private final void _euler3(Vec3d v) {
	double sinx = Math.sin(v.x);
	double siny = Math.sin(v.y);
	double sinz = Math.sin(v.z);
	double cosx = Math.cos(v.x);
	double cosy = Math.cos(v.y);
	double cosz = Math.cos(v.z);

	// Calculate a 3x3 rotation matrix
	d00 = cosy * cosz;
	d01 = -(cosx * sinz) + (sinx * siny * cosz);
	d02 = (sinx * sinz) + (cosx * siny * cosz);

	d10 = cosy * sinz;
	d11 = (cosx * cosz) + (sinx * siny * sinz);
	d12 = -(sinx * cosz) + (cosx * siny * sinz);

	d20 = -siny;
	d21 = sinx * cosy;
	d22 = cosx * cosy;
}

/**
 * Set the upper 3x3 with a rotation specified as 3 independent euler
 * rotations.
 * @throws NullPointerException if v is null
 */
public void setEuler3(Vec3d v) {
	_euler3(v);
}

/**
 * Set the upper 3x3 with a rotation specified as 3 independent euler
 * rotations.
 *
 * Clears the remaining elements to the identity matrix
 * @throws NullPointerException if v is null
 */
public void setEuler4(Vec3d v) {
	_euler3(v);
	d03 = 0.0d; d13 = 0.0d; d23 = 0.0d;
	d30 = 0.0d; d31 = 0.0d; d32 = 0.0d;
	d33 = 1.0d;
}

/**
 * Sets the 3 translation components without modifying any other components
 * @throws NullPointerException if v is null
 */
public void setTranslate3(Vec3d v) {
	this.d03 = v.x;
	this.d13 = v.y;
	this.d23 = v.z;
}

/**
 * Scale the upper 2x2 by the given value
 * @throws NullPointerException if v is null
 */
public void scale2(double scale) {
	d00 *= scale; d01 *= scale;
	d10 *= scale; d11 *= scale;
}

/**
 * Scale the upper 3x3 by the given value
 * @throws NullPointerException if v is null
 */
public void scale3(double scale) {
	d00 *= scale; d01 *= scale; d02 *= scale;
	d10 *= scale; d11 *= scale; d12 *= scale;
	d20 *= scale; d21 *= scale; d22 *= scale;
}

/**
 * Scale the upper 4x4 by the given value
 * @throws NullPointerException if v is null
 */
public void scale4(double scale) {
	d00 *= scale; d01 *= scale; d02 *= scale; d03 *= scale;
	d10 *= scale; d11 *= scale; d12 *= scale; d13 *= scale;
	d20 *= scale; d21 *= scale; d22 *= scale; d23 *= scale;
	d30 *= scale; d31 *= scale; d32 *= scale; d33 *= scale;
}

/**
 * Scale the first two rows by the given vector values
 * @throws NullPointerException if v is null
 */
public void scaleRows2(Vec2d v) {
	d00 *= v.x; d01 *= v.x; d02 *= v.x; d03 *= v.x;
	d10 *= v.y; d11 *= v.y; d12 *= v.y; d13 *= v.y;
}

/**
 * Scale the first three rows by the given vector values
 * @throws NullPointerException if v is null
 */
public void scaleRows3(Vec3d v) {
	d00 *= v.x; d01 *= v.x; d02 *= v.x; d03 *= v.x;
	d10 *= v.y; d11 *= v.y; d12 *= v.y; d13 *= v.y;
	d20 *= v.z; d21 *= v.z; d22 *= v.z; d23 *= v.z;
}

/**
 * Scale the first four rows by the given vector values
 * @throws NullPointerException if v is null
 */
public void scaleRows4(Vec4d v) {
	d00 *= v.x; d01 *= v.x; d02 *= v.x; d03 *= v.x;
	d10 *= v.y; d11 *= v.y; d12 *= v.y; d13 *= v.y;
	d20 *= v.z; d21 *= v.z; d22 *= v.z; d23 *= v.z;
	d30 *= v.w; d31 *= v.w; d32 *= v.w; d33 *= v.w;
}

/**
 * Scale the first two columns by the given vector values
 * @throws NullPointerException if v is null
 */
public void scaleCols2(Vec2d v) {
	d00 *= v.x; d01 *= v.y;
	d10 *= v.x; d11 *= v.y;
	d20 *= v.x; d21 *= v.y;
	d30 *= v.x; d31 *= v.y;
}

/**
 * Scale the first three columns by the given vector values
 * @throws NullPointerException if v is null
 */
public void scaleCols3(Vec3d v) {
	d00 *= v.x; d01 *= v.y; d02 *= v.z;
	d10 *= v.x; d11 *= v.y; d12 *= v.z;
	d20 *= v.x; d21 *= v.y; d22 *= v.z;
	d30 *= v.x; d31 *= v.y; d32 *= v.z;
}

/**
 * Scale the first four columns by the given vector values
 * @throws NullPointerException if v is null
 */
public void scaleCols4(Vec4d v) {
	d00 *= v.x; d01 *= v.y; d02 *= v.z; d03 *= v.w;
	d10 *= v.x; d11 *= v.y; d12 *= v.z; d13 *= v.w;
	d20 *= v.x; d21 *= v.y; d22 *= v.z; d23 *= v.w;
	d30 *= v.x; d31 *= v.y; d32 *= v.z; d33 *= v.w;
}

/**
 * Add the values of m to this
 * @throws NullPointerException if v is null
 */
public void add4(Mat4d m) {
	d00 += m.d00;
	d01 += m.d01;
	d02 += m.d02;
	d03 += m.d03;

	d10 += m.d10;
	d11 += m.d11;
	d12 += m.d12;
	d13 += m.d13;

	d20 += m.d20;
	d21 += m.d21;
	d22 += m.d22;
	d23 += m.d23;

	d30 += m.d30;
	d31 += m.d31;
	d32 += m.d32;
	d33 += m.d33;
}

/**
 * Return the determinant of the matrix.
 */
public double determinant() {
	// As the final row tends to be 0,0,0,1, calculate the cofactor
	// expansion along that row with fastpath for zeros.
	double det = 0.0d;
	if (d30 != 0.0d) {
		det -= d30*(d01*d12*d23 + d02*d13*d21 + d03*d11*d22 -
		            d01*d13*d22 - d02*d11*d23 - d03*d12*d21);
	}

	if (d31 != 0.0d) {
		det += d31*(d00*d12*d23 + d02*d13*d20 + d03*d10*d22 -
		            d00*d13*d22 - d02*d10*d23 - d03*d12*d20);
	}

	if (d32 != 0.0d) {
		det -= d32*(d00*d11*d23 + d01*d13*d20 + d03*d10*d21 -
		            d00*d13*d21 - d01*d10*d23 - d03*d11*d20);
	}

	if (d33 != 0.0d) {
		det += d33*(d00*d11*d22 + d01*d12*d20 + d02*d10*d21 -
		            d00*d12*d21 - d01*d10*d22 - d02*d11*d20);
	}

	return det;
}

/**
 * Returns the inverse of this matrix, or null if the matrix is not invertible
 */
public Mat4d inverse() {
	double det = determinant();

	if (det == 0.0) {
		return null;
	}

	double invDet = 1 / det;

	Mat4d ret = new Mat4d();
	double[] data = toCMDataArray();
	double[] scratch = new double[9];
	ret.d00 = invDet * cofactor(0, 0, data, scratch);
	ret.d01 = invDet * cofactor(0, 1, data, scratch);
	ret.d02 = invDet * cofactor(0, 2, data, scratch);
	ret.d03 = invDet * cofactor(0, 3, data, scratch);

	ret.d10 = invDet * cofactor(1, 0, data, scratch);
	ret.d11 = invDet * cofactor(1, 1, data, scratch);
	ret.d12 = invDet * cofactor(1, 2, data, scratch);
	ret.d13 = invDet * cofactor(1, 3, data, scratch);

	ret.d20 = invDet * cofactor(2, 0, data, scratch);
	ret.d21 = invDet * cofactor(2, 1, data, scratch);
	ret.d22 = invDet * cofactor(2, 2, data, scratch);
	ret.d23 = invDet * cofactor(2, 3, data, scratch);

	ret.d30 = invDet * cofactor(3, 0, data, scratch);
	ret.d31 = invDet * cofactor(3, 1, data, scratch);
	ret.d32 = invDet * cofactor(3, 2, data, scratch);
	ret.d33 = invDet * cofactor(3, 3, data, scratch);

	return ret;
}

private double cofactor(int x, int y, double[] data, double[] sub) {
	int nextVal = 0;
	for (int row = 0; row < 4; ++row) {
		if (row == x) continue;
		for (int col = 0; col < 4; ++col) {
			if (col == y) continue;

			sub[nextVal++] = data[row*4 + col];
		}
	}
	// Now determine the determinant of the submat
	double ret = 0;
	ret += sub[0] * sub[4] * sub[8];
	ret += sub[1] * sub[5] * sub[6];
	ret += sub[2] * sub[3] * sub[7];

	ret -= sub[2] * sub[4] * sub[6];
	ret -= sub[1] * sub[3] * sub[8];
	ret -= sub[0] * sub[5] * sub[7];

	if ((x+y) % 2 != 0) {
		ret *= -1;
	}
	return ret;
}

/**
 * Returns a column major (for historical reasons) array of the elements of this matrix
 */
public double[] toCMDataArray() {
	double[] ret = new double[16];
	ret[ 0] = d00;
	ret[ 1] = d10;
	ret[ 2] = d20;
	ret[ 3] = d30;

	ret[ 4] = d01;
	ret[ 5] = d11;
	ret[ 6] = d21;
	ret[ 7] = d31;

	ret[ 8] = d02;
	ret[ 9] = d12;
	ret[10] = d22;
	ret[11] = d32;

	ret[12] = d03;
	ret[13] = d13;
	ret[14] = d23;
	ret[15] = d33;

	return ret;
}

/**
 * Debugging feature. Check that this matrix is very close to the identity matrix
 */
public boolean nearIdentity() {
	return nearIdentityThresh(0.0001);
}

public boolean nearIdentityThresh(double threshold) {
	boolean ret = true;
	ret = ret && (Math.abs(d00 - 1) < threshold);
	ret = ret && (Math.abs(d11 - 1) < threshold);
	ret = ret && (Math.abs(d22 - 1) < threshold);
	ret = ret && (Math.abs(d33 - 1) < threshold);

	ret = ret && (Math.abs(d01 - 0) < threshold);
	ret = ret && (Math.abs(d02 - 0) < threshold);
	ret = ret && (Math.abs(d03 - 0) < threshold);

	ret = ret && (Math.abs(d10 - 0) < threshold);
	ret = ret && (Math.abs(d12 - 0) < threshold);
	ret = ret && (Math.abs(d13 - 0) < threshold);

	ret = ret && (Math.abs(d20 - 0) < threshold);
	ret = ret && (Math.abs(d21 - 0) < threshold);
	ret = ret && (Math.abs(d23 - 0) < threshold);

	ret = ret && (Math.abs(d30 - 0) < threshold);
	ret = ret && (Math.abs(d31 - 0) < threshold);
	ret = ret && (Math.abs(d32 - 0) < threshold);

	return ret;

}

/**
 * Debugging feature. Check that this matrix is very close to the identity matrix
 */
public boolean nearIdentity3() {
	return nearIdentityThresh3(0.0001);
}

public boolean nearIdentityThresh3(double threshold) {
	boolean ret = true;
	ret = ret && (Math.abs(d00 - 1) < threshold);
	ret = ret && (Math.abs(d11 - 1) < threshold);
	ret = ret && (Math.abs(d22 - 1) < threshold);

	ret = ret && (Math.abs(d01 - 0) < threshold);
	ret = ret && (Math.abs(d02 - 0) < threshold);

	ret = ret && (Math.abs(d10 - 0) < threshold);
	ret = ret && (Math.abs(d12 - 0) < threshold);

	ret = ret && (Math.abs(d20 - 0) < threshold);
	ret = ret && (Math.abs(d21 - 0) < threshold);


	return ret;

}

public boolean near4(Mat4d m) {
	return MathUtils.near(d00, m.d00) &&
	       MathUtils.near(d01, m.d01) &&
	       MathUtils.near(d02, m.d02) &&
	       MathUtils.near(d03, m.d03) &&

	       MathUtils.near(d10, m.d10) &&
	       MathUtils.near(d11, m.d11) &&
	       MathUtils.near(d12, m.d12) &&
	       MathUtils.near(d13, m.d13) &&

	       MathUtils.near(d20, m.d20) &&
	       MathUtils.near(d21, m.d21) &&
	       MathUtils.near(d22, m.d22) &&
	       MathUtils.near(d23, m.d23) &&

	       MathUtils.near(d30, m.d30) &&
	       MathUtils.near(d31, m.d31) &&
	       MathUtils.near(d32, m.d32) &&
	       MathUtils.near(d33, m.d33);
}

}
