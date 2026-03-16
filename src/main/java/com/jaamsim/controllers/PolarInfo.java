/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2026 JaamSim Software Inc.
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
package com.jaamsim.controllers;

import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Vec3d;

public class PolarInfo {

	double rotZ; // The spherical coordinate that rotates around Z (in radians)
	double rotX; // Ditto for X
	double radius; // The distance the camera is from the view center
	final Vec3d viewCenter; // centre of the polar coordinate system
	final Vec3d viewPosition; // postion of the camera
	final Vec3d viewDirection; // direction in which the camera is pointing

	/**
	 * Constructs the polar coordinates for the view camera.
	 * @param center - position along the camera's line of sight
	 * @param pos - position of the view camera
	 * @param dir - direction of the view camera
	 */
	public PolarInfo(Vec3d center, Vec3d pos, Vec3d dir) {
		viewCenter = new Vec3d(center);

		Vec3d viewDiff = new Vec3d();
		viewDiff.sub3(pos, center);
		radius = viewDiff.mag3();

		viewPosition = new Vec3d(pos);
		viewDirection = new Vec3d(dir);

		rotZ = Math.atan2(-dir.x, dir.y);
		if (MathUtils.near(dir.x, 0.0d) && MathUtils.near(dir.y, 0.0d))
			rotZ = 0.0d;

		double xyDist = Math.hypot(dir.x, dir.y);
		rotX = Math.atan2(xyDist, -dir.z);
	}

	public Quaternion getRotation() {
		Quaternion rot = new Quaternion();
		rot.setRotZAxis(rotZ);

		Quaternion tmp = new Quaternion();
		tmp.setRotXAxis(rotX);

		rot.mult(rot, tmp);
		return rot;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PolarInfo)) {
			return false;
		}
		PolarInfo pi = (PolarInfo)o;

		return pi.rotZ == rotZ && pi.rotX == rotX
				&& pi.radius == radius
				&& viewCenter.equals3(pi.viewCenter)
				&& viewPosition.equals3(pi.viewPosition)
				&& viewDirection.equals3(pi.viewDirection);
	}

	@Override
	public String toString() {
		return String.format("rotZ=%s, rotX=%s, viewPosition=%s, viewDirection=%s, radius=%s, viewCenter=%s",
				rotZ, rotX, viewPosition, viewDirection, radius, viewCenter);
	}

}
