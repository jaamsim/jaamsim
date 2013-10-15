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
package com.jaamsim.render;

import com.jaamsim.math.AABB;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Plane;
import com.jaamsim.math.Sphere;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;

/**
 * In the JaamRenderer, Camera's represent a single viewer of the world. Camera's are responsible for providing
 * the Projection and View matrices at render time and are capable of view frustum culling against bounding
 * spheres and AABBs (both in world coords)
 * @author Matt Chudleigh
 *
 */
public class Camera {

// Tuning parameters for logarithmic depth buffer
// Thanks to this algorithm, we can have a near distance of 0.1mm, and a far distance of around
// 10,000km
// This uses the algorithm from the following article:
// http://outerra.blogspot.ca/2012/11/maximizing-depth-buffer-range-and.html
public static final float C = 1.0f;
public static final float FC;
public static final float near = 0.0001f;
public static final float far = 100000000f;
static {
	FC = (float)(1.0/Math.log(far*C + 1));
}


/**
 * All the basic camera configuration information is stored in a CameraInfo, this can be copied and passed
 * back to the app. All other members are renderer owned information
 */
private CameraInfo _info;
private double _aspectRatio;
private Transform invTrans = new Transform();

private Mat4d _projMat;
private boolean _projMatDirty = true;

/**
 * An array of 6 Planes that represent the view frustum in world coordinates
 */
private final Plane[] _frustum;
private boolean _frustumDirty = true;

{
	_frustum = new Plane[5];
	for (int i = 0; i < _frustum.length; i++)
		_frustum[i] = new Plane();
}

/**
 * Construct a new camera, the parameters provided are needed to determine the view frustum and the
 * project matrix. Default camera is the openGL camera, at the origin looking in the -Z direction
 * @param FOV
 * @param aspectRatio
 * @param near
 * @param far
 */
public Camera(double FOV, double aspectRatio, double near, double far) {
	_info = new CameraInfo(FOV, near, far, Transform.ident, null);

	_aspectRatio = aspectRatio;
	_info.trans.inverse(invTrans);

	_frustumDirty = true;
	_projMatDirty = true;
}

public Camera(CameraInfo camInfo, double aspectRatio) {
	_info = camInfo;

	_aspectRatio = aspectRatio;
	_info.trans.inverse(invTrans);

	_frustumDirty = true;
	_projMatDirty = true;
}

public void getTransform(Transform out) {
	out.copyFrom(_info.trans);
}

public Transform getTransformRef() {
	return _info.trans;
}
/**
 * Update the camera position
 * @param t
 */
public void setTransform(Transform t) {
	_info.trans.copyFrom(t);
	_frustumDirty = true;
	_info.trans.inverse(invTrans);

}

/**
 * Apply this transform to the existing camera transform.
 * Useful for incremental changes like rotating in place
 * @param t
 */
public void applyTransform(Transform t) {
	_info.trans.merge(t, _info.trans);

	_frustumDirty = true;
	_info.trans.inverse(invTrans);

}

/**
 * Fills a Matrix4d with the projection matrix for this camera, lookup gluPerspective to understand the math
 * @param projOut
 */
private void updateProjMat() {
	if (_projMat == null) {
		_projMat = new Mat4d();
	}
	_projMat.zero();

	double f = 1/Math.tan(_info.FOV/2);
	double denom = 1 / (_info.nearDist - _info.farDist);

	double fx, fy;
	if(_aspectRatio > 1) {
		fx = f;
		fy = f * _aspectRatio;
	} else {
		fy = f;
		fx = f / _aspectRatio;
	}

	_projMat.d00 = fx;
	_projMat.d11 = fy;
	_projMat.d22 = (_info.farDist + _info.nearDist) * denom;
	_projMat.d32 = -1;
	_projMat.d23 = 2*_info.nearDist*_info.farDist * denom;

	_projMatDirty = false;
}

/**
 * Gets a reference to the projection matrix for this camera
 * @return
 */
public Mat4d getProjMat4d() {
	if (_projMatDirty) {
		updateProjMat();
	}

	return _projMat;
}

/**
 * Get the view matrix for the current position (defined by the transform)
 * @param viewOut
 */
public void getViewMat4d(Mat4d viewOut) {

	invTrans.getMat4d(viewOut);

}

public void getRotMat4d(Mat4d rotOut) {
	rotOut.setRot4(invTrans.getRotRef());
}

public void getViewTrans(Transform transOut) {
	transOut.copyFrom(invTrans);
}

public double getNear() {
	return _info.nearDist;
}
public double getFar() {
	return _info.farDist;
}
public double getAspectRatio() {
	return _aspectRatio;
}
/**
 * Get the camera FOV in the y direction in radians
 * @return
 */
public double getFOV() {
	return _info.FOV;
}

public void setNear(double near) {
	boolean dirty =  !MathUtils.near(_info.nearDist, near);
	_frustumDirty = dirty || _frustumDirty;
	_projMatDirty = dirty || _projMatDirty;
	_info.nearDist = near;
}

public void setFar(double far) {
	boolean dirty = !MathUtils.near(_info.farDist, far);
	_frustumDirty = dirty || _frustumDirty;
	_projMatDirty = dirty || _projMatDirty;
	_info.farDist = far;
}

public void setAspectRatio(double aspect) {
	boolean dirty = !MathUtils.near(_aspectRatio, aspect);
	_frustumDirty = dirty || _frustumDirty;
	_projMatDirty = dirty || _projMatDirty;
	_aspectRatio = aspect;
}

public void setFOV(double FOV) {
	boolean dirty = !MathUtils.near(_info.FOV, FOV);
	_frustumDirty = dirty || _frustumDirty;
	_projMatDirty = dirty || _projMatDirty;
	_info.FOV = FOV;
}

/**
 * Get a direction vector in the direction of camera view (only accurate for the center of the view, as this
 * is a perspective transform)
 * @param dirOut
 */
public void getViewDir(Vec4d dirOut) {
	_info.trans.apply(new Vec4d(0 ,0, -1, 0), dirOut);
}

/**
 * Test frustum collision with sphere s
 * @param s
 * @return
 */
public boolean collides(Sphere s) {
	updateFrustum();

	// The sphere needs to be inside (or touching) all planes to be in the frustum
	for (Plane p : _frustum) {
		double dist = s.getDistance(p);
		if (dist < - s.radius) {
			return false;
		}
	}
	return true;
}

public boolean collides(AABB aabb) {
	if (aabb.isEmpty()) {
		return false;
	}

	updateFrustum();

	for (Plane p : _frustum) {
		// Check if the AABB is completely outside any frustum plane
		if (aabb.testToPlane(p) == AABB.PlaneTestResult.NEGATIVE) {
			return false;
		}
	}
	return true;
}

/**
 * Update the stored frustum planes to account for the current parameters and transform
 */
private void updateFrustum() {
	if (!_frustumDirty) {
		return; // Already done
	}

	double thetaX, thetaY;
	if (_aspectRatio > 1) {
		thetaX = _info.FOV/2;
		thetaY = Math.atan(Math.tan(thetaX) / _aspectRatio);
	} else {
		thetaY = _info.FOV/2;
		thetaX = Math.atan(Math.tan(thetaY) * _aspectRatio);
	}

	double sinX = Math.sin(thetaX);
	double sinY = Math.sin(thetaY);

	double cosX = Math.cos(thetaX);
	double cosY = Math.cos(thetaY);

	Vec3d v = new Vec3d();
	// Create the planes that define the frustum, anything on the positive side
	// of all planes is in the frustum
	// +Y
	v.set3(0,  cosY, -sinY);
	_frustum[0].set(v, 0.0d);
	// -Y
	v.set3(0, -cosY, -sinY);
	_frustum[1].set(v, 0.0d);

	// +X
	v.set3( cosX, 0, -sinX);
	_frustum[2].set(v, 0.0d);
	// -X
	v.set3(-cosX, 0, -sinX);
	_frustum[3].set(v, 0.0d);

	// -Z
	v.set3(0.0d, 0.0d, 1.0d);
	_frustum[4].set(v, -_info.farDist);

	// Apply the current transform to the planes. Puts the planes in world space
	for (Plane p : _frustum) {
		p.transform(_info.trans, p, v);
	}

}

public CameraInfo getInfo() {
	return new CameraInfo(_info);
}

CameraInfo getInfoRef() {
	return _info;
}

private Vec3d centerTemp = new Vec3d();

public double distToBounds(AABB bounds) {

	invTrans.multAndTrans(bounds.center, centerTemp);

	return centerTemp.z * -1; // In camera space, Z is in the negative direction

}
/**
 * Overwrite all core camera state in one go
 */
public void setInfo(CameraInfo newInfo) {
	boolean dirty = !_info.isSame(newInfo);
	_info = new CameraInfo(newInfo);
	_info.trans.inverse(invTrans);

	_frustumDirty = dirty || _frustumDirty;
	_projMatDirty = dirty || _projMatDirty;
}

} // class Camera
