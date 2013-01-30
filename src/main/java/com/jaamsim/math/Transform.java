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
private Vec4d _trans;
private double _scale;
private Mat4d _mat4d;
private boolean _matrixDirty;

public static final Transform ident = new Transform(); // Static Identity transform

public Transform() {
	_trans = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d); // Zero
	_rot = new Quaternion(); // Identity
	_scale = 1;
	_mat4d = new Mat4d();
	_matrixDirty = false;
}

public Transform(Transform t) {
	_trans = new Vec4d(t._trans);
	_rot = new Quaternion(t._rot);
	_scale = t._scale;
	_matrixDirty = true;
}

public Transform(Vec3d trans, Quaternion rot, double scale)
{
	_trans = new Vec4d(trans.x, trans.y, trans.z, 1.0d);
	if (rot == null)
		_rot = new Quaternion();
	else
		_rot = new Quaternion(rot);
	_scale = scale;
	_matrixDirty = true;
}

public Transform(Vec3d trans) {
	this(trans, null, 1.0d);
}

public void copyFrom(Transform t) {
	_trans.set4(t._trans);
	_rot.set(t._rot);
	_scale = t._scale;

	if (!t._matrixDirty) {
		_mat4d.set4(t._mat4d);
	}
	_matrixDirty = t._matrixDirty;
}

public void setTrans(Vec3d trans) {
	if (_trans.equals(trans)) {
		return;
	}
	_trans = new Vec4d(trans.x, trans.y, trans.z, 1.0d);
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

public Vec4d getTransRef() {
	return _trans;
}

public void getTrans(Vec4d out) {
	out.set4(_trans.x, _trans.y, _trans.z, 1);
}

public double getScale() {
	return _scale;
}

/**
 * Calculated the 4x4 matrix corresponding to this transform
 * @param out - the 4x4 matrix
 */
public void getMat4d(Mat4d out) {
	if (_matrixDirty) {
		updateMatrix();
	}

	out.set4(_mat4d);

}

public Mat4d getMat4dRef() {
	if (_matrixDirty) {
		updateMatrix();
	}

	return _mat4d;
}


private void updateMatrix() {
	assert(_trans.x != Double.NaN);
	assert(_trans.y != Double.NaN);
	assert(_trans.z != Double.NaN);

	_mat4d = new Mat4d();

	_mat4d.setTranslate3(_trans);
	_mat4d.setRot3(_rot);
	_mat4d.scale3(_scale);

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

	Vec4d temp = new Vec4d(_trans);

	_rot.rotateVector(rhs._trans, out._trans);
	out._trans.scale3(_scale);

	out._trans.add3(temp);
	out._matrixDirty = true;

	_rot.mult(rhs._rot, out._rot);

	out._scale = _scale * rhs._scale;

}

/**
 * Apply this transform to a vector
 * @param vect - the vector to transform
 * @param out - the transformed vector
 */
public void apply(Vec4d vect, Vec4d out) {
	if (_matrixDirty) {
		updateMatrix();
	}

	out.mult4(_mat4d, vect);
}

public void apply(Vec3d vect, Vec3d out) {
	if (_matrixDirty) {
		updateMatrix();
	}

	out.multAndTrans3(_mat4d, vect);
}

/**
 * Returns a transform that is the inverse of this transform, merging the two in any order will
 * result in the identity transform
 * @param out
 */
public void inverse(Transform out) {
	out._scale = 1/_scale;
	out._rot.conjugate(_rot);

	out._trans.set4(_trans);
	out._trans.scale3(-out._scale);
	out._rot.rotateVector(out._trans, out._trans);

	out._matrixDirty = true;
}

public boolean equals(Transform other) {
	return _trans.near4(other._trans) && _rot.equals(other._rot) && MathUtils.near(_scale, other._scale);
}

public String toString()
{
	return "T: " + _trans.toString() + " R: " + _rot.toString() + " S: " + Double.toString(_scale);
}

} // class
