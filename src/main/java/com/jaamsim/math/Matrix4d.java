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
 * Matrix4d is the basic representation of a 4x4 matrix for the JaamRender project
 * It's a very garden variety matrix implementation, using doubles for components and is stored internally
 * As a list of columns. Multiplication is post multiply
 * @author Matt Chudleigh
 *
 */
public class Matrix4d {


public static final Matrix4d IDENT = new Matrix4d();

public double[] data;

/**
 * Create an identity matrix
 */
public Matrix4d() {
	data = new double[16];
	for (int i = 0; i < 16; ++i) {
		data[i] = (i % 5 == 0) ? 1.0 : 0.0; // Diagonal entries are 1, all others are 0
	}
}

public Matrix4d(Vector4d[] columns) {
	if (columns.length != 4) {
		throw new IllegalArgumentException("Matrix4d requires 4 columns");
	}

	data = new double[16];
	for (int col = 0; col < 4; ++col) {
		for (int row = 0; row < 4; ++row) {
			data[col * 4 + row] = columns[col].data[row];
		}
	}
}

public Matrix4d(double[] values) {
	if (values.length != 16) {
		throw new IllegalArgumentException("Matrix4d requires 16 values");
	}
	data = new double[16];
	for (int i = 0; i < 16; ++i) {
		data[i] = values[i];
	}
}

public Matrix4d(Matrix4d m) {
	data = new double[16];
	for (int i = 0; i < 16; ++i) {
		data[i] = m.data[i];
	}
}

public static Matrix4d ScaleMatrix(double sx, double sy, double sz)
{
	Matrix4d ret = new Matrix4d();
	ret.data[0] = sx;
	ret.data[5] = sy;
	ret.data[10] = sz;
	return ret;
}

/**
 * Return a matrix that rotates points and projects them onto the ray's view plane.
 * IE: the new coordinate system has the ray pointing in the +Z direction from the origin.
 * This is useful for ray-line collisions and ray-point collisions
 * @return
 */
public static Matrix4d RaySpace(Ray r) {

	// Create a new orthonormal basis.
	Vector4d basisSeed = Vector4d.Y_AXIS;
	if (MathUtils.near(basisSeed.dot3(r.getDirRef()), 1) ||
		MathUtils.near(basisSeed.dot3(r.getDirRef()), -1)) {
		// The ray is nearly parallel to Y
		basisSeed = Vector4d.X_AXIS; // So let's build our new basis from X instead
	}

	Vector4d[] newBasis = new Vector4d[3];
	newBasis[0] = new Vector4d();
	r.getDirRef().cross(basisSeed, newBasis[0]);
	newBasis[0].normalizeLocal3();
	newBasis[0].data[3] = 0;

	newBasis[1] = new Vector4d();
	r.getDirRef().cross(newBasis[0], newBasis[1]);
	newBasis[1].normalizeLocal3();
	newBasis[1].data[3] = 0;

	newBasis[2] = r.getDirRef();

	Matrix4d ret = new Matrix4d();
	// Use the new basis to populate the rows of the return matrix
	ret.data[0] = newBasis[0].data[0];
	ret.data[4] = newBasis[0].data[1];
	ret.data[8] = newBasis[0].data[2];

	ret.data[1] = newBasis[1].data[0];
	ret.data[5] = newBasis[1].data[1];
	ret.data[9] = newBasis[1].data[2];

	ret.data[ 2] = newBasis[2].data[0];
	ret.data[ 6] = newBasis[2].data[1];
	ret.data[10] = newBasis[2].data[2];

	ret.data[15] = 1;

	// Now use this rotation matrix to calculate the rotated translation part
	Vector4d newTrans = new Vector4d();
	ret.mult(r.getStartRef(), newTrans);

	ret.data[12] = -newTrans.data[0];
	ret.data[13] = -newTrans.data[1];
	ret.data[14] = -newTrans.data[2];

	return ret;
}

public static Matrix4d RotationMatrix(Quaternion q)
{
	Matrix4d ret = new Matrix4d();
	q.toRotationMatrix(ret);
	return ret;
}

public static Matrix4d RotationMatrix(double angle, Vector4d axis)
{
	Matrix4d ret = new Matrix4d();
	Quaternion q = Quaternion.Rotation(angle, axis);
	q.toRotationMatrix(ret);
	return ret;
}

public static Matrix4d ScaleMatrix(double scale)
{
	Matrix4d ret = new Matrix4d();
	ret.data[0] = scale;
	ret.data[5] = scale;
	ret.data[10] = scale;
	return ret;
}

/**
 * Non uniform scale
 * @param scale
 * @return
 */
public static Matrix4d ScaleMatrix(Vector4d scale)
{
	Matrix4d ret = new Matrix4d();
	ret.data[0] = scale.data[0];
	ret.data[5] = scale.data[1];
	ret.data[10] = scale.data[2];
	return ret;
}

public static Matrix4d TranslationMatrix(Vector4d trans)
{
	Matrix4d ret = new Matrix4d();
	ret.data[12] = trans.data[0];
	ret.data[13] = trans.data[1];
	ret.data[14] = trans.data[2];
	return ret;
}

public float[] toFloats() {
	float[] ret = new float[16];
	for (int i = 0; i < 16; ++i) {
		ret[i] = (float)data[i];
	}
	return ret;
}

/**
 * Populate this matrix with the data from 'source'
 * @param source - the matrix to copy
 */
public void copyFrom(Matrix4d source) {
	for (int i = 0; i < 16; ++i) {
		data[i] = source.data[i];
	}
}

/**
 * Zero all components in the matrix. Can be handy when building sparse matrices
 */
public void zero() {
	for ( int i = 0; i < 16; ++i) {
		data[i] = 0;
	}
}

/**
 * Post multiply this column vector
 * @param vect - column vector to apply
 * @param out - result
 * @return
 */
public void mult(Vector4d vect, Vector4d out) {
	double[] storage = out.data;
	boolean useScratch = false;
	if (vect == out) {
		storage = new double[16];
		useScratch = true;
	}

	for (int i = 0; i < 4; ++i) {
		storage[i]  = data[ 0 + i] * vect.data[0];
		storage[i] += data[ 4 + i] * vect.data[1];
		storage[i] += data[ 8 + i] * vect.data[2];
		storage[i] += data[12 + i] * vect.data[3];
	}

	if (useScratch) {
		for (int i = 0; i < 4; ++i) {
			out.data[i] = storage[i];
		}
	}
}

/**
 * 4x4 matrix multiplication
 * @param mat - the right hand matrix
 * @param out - the result, can not be this
 */
public void mult(Matrix4d mat, Matrix4d out) {
	double[] storage = out.data;
	boolean useScratch = false;
	if (out == mat || out == this)	{
		storage = new double[16];
		useScratch = true;
	}

	for (int col = 0; col < 4; ++col) {
		for (int row = 0; row < 4; ++row) {
			storage[4*col + row]  = data[ 0 + row] * mat.data[4*col + 0];
			storage[4*col + row] += data[ 4 + row] * mat.data[4*col + 1];
			storage[4*col + row] += data[ 8 + row] * mat.data[4*col + 2];
			storage[4*col + row] += data[12 + row] * mat.data[4*col + 3];
		}
	}

	if (useScratch) {
		for (int i = 0; i < 16; ++i) {
			out.data[i] = storage[i];
		}
	}
}

/**
 * A more usable (and chainable) version of the matrix multiplication code
 * will heap allocate a new matrix to return, so this should be avoided in really high performance code
 * @param mat - right hand side matrix
 * @return - the product matrix
 */
public Matrix4d mult(Matrix4d mat) {
	Matrix4d ret = new Matrix4d();
	mult(mat, ret);
	return ret;
}

/**
 * Matrix addition, component wise
 * @param mat - the other matrix
 * @param out - the result
 */
public void add(Matrix4d mat, Matrix4d out){
	for (int i = 0; i < 16; ++i)
	{
		out.data[i] = data[i] + mat.data[i];
	}
}

/**
 * Matrix addition, added to this
 * @param mat - the other matrix
 * @param out - the result
 */
public void addLocal(Matrix4d mat){
	for (int i = 0; i < 16; ++i)
	{
		data[i] += mat.data[i];
	}
}

/**
 * Matrix subtraction, component wise
 * @param mat - the other matrix
 * @param out - the result
 */
public void sub(Matrix4d mat, Matrix4d out){
	for (int i = 0; i < 16; ++i)
	{
		out.data[i] = data[i] - mat.data[i];
	}
}

/**
 * Matrix subtraction, subtracted from this
 * @param mat - the other matrix
 * @param out - the result
 */
public void subLocal(Matrix4d mat){
	for (int i = 0; i < 16; ++i)
	{
		data[i] -= mat.data[i];
	}
}

/**
 * Scale the matrix by a scalar
 * @param scalar - the scalar
 * @param out - the result
 */
public void scale(double scalar, Matrix4d out)
{
	for (int i = 0; i < 16; ++i) {
		out.data[i] = data[i] * scalar;
	}
}
/**
 * Scale this matrix by a scalar
 * @param scalar
 */
public void scaleLocal(double scalar)
{
	for (int i = 0; i < 16; ++i) {
		data[i] *= scalar;
	}
}

/**
 * Matrix transposition
 * @param out - the transposed matrix
 */
public void transpose(Matrix4d out) {
	for (int col = 0; col < 4; ++col) {
		for (int row = 0; row < 4; ++row) {
			out.data[row*4 + col] = data[col*4 + row];
		}
	}
}

/**
 * Matrix transposition in place
 */
public void transposeLocal(){
	for (int col = 1; col < 4; ++col) {
		for (int row = 0; row < col; ++row) {
			double temp = data[4*col + row];
			data[4*col + row] = data[4*row + col];
			data[4*row + col] = temp;

		}
	}
}

/**
 * Set the x, y, z components of the 4th column to the x, y, z components of trans
 * @param trans - the new translation
 */
public void setTranslationPart(Vector4d trans) {
	data[12] = trans.data[0];
	data[13] = trans.data[1];
	data[14] = trans.data[2];
}

/**
 * Tests equality with the other matrix if all values are closers than Constants.EPSILON
 * @param other - the other matrix
 * @return - equality
 */
public boolean equals(Matrix4d other) {
	for (int i = 0; i < 16; ++i) {
		if (!MathUtils.near(data[i], other.data[i]))
			return false;
	}

	return true;
}

/**
 * Returns the cofactor term Cxy
 * @param x - column number
 * @param y - row number
 * @return
 */
public double cofactor(int x, int y) {
	double[] sub = new double[9];
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

	if ((x+y) % 2 == 1) {
		ret *= -1;
	}
	return ret;
}

public double determinant() {
	double ret = 0;
	for (int i = 0; i < 4; ++i) {
		// Due to the nature of CG matrices, the bottom row is usually 0s and 1s,
		// so we will use that row to help with mathematical stability
		ret += data[i*4 + 3] * cofactor(i, 3);
	}
	return ret;
}

/**
 * Returns a new matrix that is the inverse of this matrix. Returns null if this matrix is not invertible
 * @return
 */
public Matrix4d inverse() {
	double det = determinant();

	if (Math.abs(det) < 0.000000000001) {
		return null;
	}
	Matrix4d ret = new Matrix4d();
	for (int row = 0; row < 4; ++row) {
		for (int col = 0; col < 4; ++col) {
			ret.data[col*4 + row] = cofactor(row, col) / det;
		}
	}
	return ret;
}

@Override
public String toString() {
	String ret = new String();
	ret += "[" + data[0] + ", " + data[4] + ", " + data[ 8]  + ", " + data[12] + "]\n";
	ret += "[" + data[1] + ", " + data[5] + ", " + data[ 9]  + ", " + data[13] + "]\n";
	ret += "[" + data[2] + ", " + data[6] + ", " + data[10]  + ", " + data[14] + "]\n";
	ret += "[" + data[3] + ", " + data[7] + ", " + data[11]  + ", " + data[15] + "]\n";
	return ret;
}

} // class
