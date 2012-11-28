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
 * The Transform class represents a linear transform consisting of a rotation, scale and translation
 * in 3-space. Internally it stores all 3 as discrete transforms but allows for arbitrary transforms to be chained,
 * collapsed and inverted. The primary limitation is it only allows for uniform (isotropic) scaling
 * @author Matt Chudleigh
 *
 */
public class Transform {

private Quaternion _rot;
private Vector4d _trans;
private double _scale;
private Matrix4d _mat;
private boolean _matrixDirty;

public static final Transform ident = new Transform(); // Static Identity transform

public Transform() {
	_trans = new Vector4d(); // Zero
	_rot = new Quaternion(); // Identity
	_scale = 1;
	_mat = new Matrix4d(); // Identity
	_matrixDirty = false;
}

public Transform(Transform t) {
	_trans = new Vector4d(t._trans);
	_rot = new Quaternion(t._rot);
	_scale = t._scale;
	_matrixDirty = true;
}

public Transform(Vector4d trans, Quaternion rot, double scale)
{
	_trans = new Vector4d(trans);
	_rot = new Quaternion(rot);
	_scale = scale;
	_matrixDirty = true;
}

public Transform(Vector4d trans) {
	this(trans, Quaternion.ident, 1);
}

public void copyFrom(Transform t) {
	_trans.copyFrom(t._trans);
	_rot.set(t._rot);
	_scale = t._scale;

	if (!t._matrixDirty) {
		_mat.copyFrom(t._mat);
	}
	_matrixDirty = t._matrixDirty;
}

public void setTrans(Vector4d trans) {
	if (_trans.equals(trans)) {
		return;
	}
	_trans.copyFrom(trans);
	_matrixDirty = true;
}

public void setRot(Quaternion q) {
	if (_rot.equals(q)) {
		return;
	}
	_rot.set(q);
	_matrixDirty = true;
}

public void setScale(double s) {
	if (MathUtils.near(_scale, s)) {
		return;
	}
	_scale = s;
	_matrixDirty = true;
}

public void getRot(Quaternion out) {
	out.set(_rot);
}

public Quaternion getRotRef() {
	return _rot;
}

public void getTrans(Vector4d out) {
	out.copyFrom(_trans);
}

public Vector4d getTransRef() {
	return _trans;
}

public double getScale() {
	return _scale;
}

/**
 * Calculated the 4x4 matrix corresponding to this transform
 * @param out - the 4x4 matrix
 */
public void getMatrix(Matrix4d out) {
	if (_matrixDirty) {
		updateMatrix();
	}

	out.copyFrom(_mat);
}

public Matrix4d getMatrixRef() {
	if (_matrixDirty) {
		updateMatrix();
	}

	return _mat;
}

private void updateMatrix() {
	if (_mat == null) {
		_mat = new Matrix4d();
	}

	Matrix4d scaleMat = Matrix4d.ScaleMatrix(_scale);
	Matrix4d transMat = Matrix4d.TranslationMatrix(_trans);
	Matrix4d rotMat = Matrix4d.RotationMatrix(_rot);

	_mat.copyFrom(transMat);
	_mat.mult(rotMat, _mat);
	_mat.mult(scaleMat, _mat);

	_matrixDirty = false;

}

/**
 * Populates a transform that is the merging of this and 'rhs'
 * The matrix from this new transform will be the same as if the matrices of both transforms
 * were multiplied (see the unit tests)
 * @param rhs - the right hand matrix to merge with
 * @param out
 */
public void merge(Transform rhs, Transform out) {

	Vector4d temp = new Vector4d(_trans);

	_rot.rotateVector(rhs._trans, out._trans);
	out._trans.scaleLocal3(_scale);
	out._trans.add3(temp, out._trans);
	out._matrixDirty = true;

	_rot.mult(rhs._rot, out._rot);

	out._scale = _scale * rhs._scale;

}

/**
 * Apply this transform to a vector
 * @param vect - the vector to transform
 * @param out - the transformed vector
 */
public void apply(Vector4d vect, Vector4d out) {
	if (_matrixDirty) {
		updateMatrix();
	}

	_mat.mult(vect,  out);
}

/**
 * Returns a transform that is the inverse of this transform, merging the two in any order will
 * result in the identity transform
 * @param out
 */
public void inverse(Transform out) {
	out._scale = 1/_scale;
	out._rot.conjugate(_rot);

	out._trans.copyFrom(_trans);
	out._trans.scaleLocal3(-out._scale);
	out._rot.rotateVector(out._trans, out._trans);

	out._matrixDirty = true;
}

public boolean equals(Transform other) {
	return _trans.equals(other._trans) && _rot.equals(other._rot) && MathUtils.near(_scale, other._scale);
}

public String toString()
{
	return "T: " + _trans.toString() + " R: " + _rot.toString() + " S: " + Double.toString(_scale);
}

} // class
