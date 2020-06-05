/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2020 JaamSim Software Inc.
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

import com.jaamsim.math.Vec3d;

public class PolarInfo {

	double rotZ; // The spherical coordinate that rotates around Z (in radians)
	double rotX; // Ditto for X
	double radius; // The distance the camera is from the view center
	final Vec3d viewCenter; // centre of the polar coordinate system

	/**
	 * Constructs the polar coordinates for the view camera.
	 * @param center - position along the camera's line of sight
	 * @param pos - position of the view camera
	 */
	PolarInfo(Vec3d center, Vec3d pos) {
		viewCenter = new Vec3d(center);

		Vec3d viewDiff = new Vec3d();
		viewDiff.sub3(pos, center);
		radius = viewDiff.mag3();

		rotZ = Math.atan2(viewDiff.x, -viewDiff.y);
		if (viewDiff.x == 0.0d && viewDiff.y == 0.0d)  // degenerate case
			rotZ = 0.0d;

		double xyDist = Math.hypot(viewDiff.x, viewDiff.y);
		rotX = Math.atan2(xyDist, viewDiff.z);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PolarInfo)) {
			return false;
		}
		PolarInfo pi = (PolarInfo)o;

		return pi.rotZ == rotZ && pi.rotX == rotX && pi.radius == radius &&
		       viewCenter.equals3(pi.viewCenter);
	}

}
