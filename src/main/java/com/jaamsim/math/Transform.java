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

/**
 * The Transform class represents a linear transform consisting of a rotation, scale and translation
 * in 3-space. Internally it stores all 3 as discrete transforms but allows for arbitrary transforms to be chained,
 * collapsed and inverted. The primary limitation is it only allows for uniform (isotropic) scaling
 * @author Matt Chudleigh
 *
 */
public class Transform {

private final Quaternion _rot;
private final Vec3d _trans;
private double _scale;
private final Mat4d _mat4d = new Mat4d();
private boolean _matrixDirty;

public static final Transform ident = new Transform(); // Static Identity transform

public Transform() {
	_trans = new Vec3d();
	_rot = new Quaternion(); // Identity
	_scale = 1;
	_matrixDirty = false;
}

public Transform(Transform t) {
	_trans = new Vec3d(t._trans);
	_rot = new Quaternion(t._rot);
	_scale = t._scale;
	_matrixDirty = true;
}

public Transform(Vec3d trans, Quaternion rot, double scale)
{
	if (trans == null)
		_trans = new Vec3d();
	else
		_trans = new Vec3d(trans);

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
	_trans.set3(t._trans);
	_rot.set(t._rot);
	_scale = t._scale;

	if (!t._matrixDirty) {
		_mat4d.set4(t._mat4d);
	}
	_matrixDirty = t._matrixDirty;
}

public void setTrans(Vec3d trans) {
	if (_trans.equals3(trans)) {
		return;
	}
	_trans.set3(trans);
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

public Vec3d getTransRef() {
	return _trans;
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
	assert(!Double.isNaN(_trans.x));
	assert(!Double.isNaN(_trans.y));
	assert(!Double.isNaN(_trans.z));

	_mat4d.setRot4(_rot);
	_mat4d.setTranslate3(_trans);
	_mat4d.scale3(_scale);

	_matrixDirty = false;

}

/**
 * Populates a transform that is the merging of this and 'rhs'
 * The matrix from this new transform will be the same as if the matrices of both transforms
 * were multiplied (see the unit tests)
 * @param a - the right hand matrix to merge with
 * @param b
 */
public void merge(Transform a, Transform b) {
	Vec3d temp = new Vec3d(a._trans);

	Mat4d rotTemp = new Mat4d();
	rotTemp.setRot3(a._rot);

	_trans.mult3(rotTemp, b._trans);
	_trans.scale3(a._scale);

	_trans.add3(temp);
	_matrixDirty = true;

	_rot.mult(a._rot, b._rot);

	_scale = a._scale * b._scale;

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

public void multAndTrans(Vec3d vect, Vec3d out) {
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

	out._trans.set3(_trans);
	out._trans.scale3(-out._scale);

	Mat4d rotTemp = new Mat4d();
	rotTemp.setRot3(out._rot);
	out._trans.mult3(rotTemp, out._trans);

	out._matrixDirty = true;
}

@Override
public boolean equals(Object o) {
	if (!(o instanceof Transform)) return false;
	Transform t = (Transform)o;

	return _trans.equals3(t._trans) && _rot.equals(t._rot) && MathUtils.near(_scale, t._scale);
}

public boolean near(Transform t) {
	return _trans.near3(t._trans) && _rot.equals(t._rot) && MathUtils.near(_scale, t._scale);
}

@Override
public int hashCode() {
	//assert false : "hashCode not designed";
	return 42; // any arbitrary constant will do
}

@Override
public String toString()
{
	return "T: " + _trans.toString() + " R: " + _rot.toString() + " S: " + Double.toString(_scale);
}

} // class
