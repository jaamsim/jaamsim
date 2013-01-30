/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.input;

import com.jaamsim.math.Vec3d;

public class KeyedVec3dCurve extends KeyedCurve<Vec3d> {

	@Override
	protected Vec3d interpVal(Vec3d val0, Vec3d val1, double ratio) {
		Vec3d ret = new Vec3d();
		double oneMinus = 1 - ratio;
		ret.x = val0.x*oneMinus + val1.x*ratio;
		ret.y = val0.y*oneMinus + val1.y*ratio;
		ret.z = val0.z*oneMinus + val1.z*ratio;
		return ret;
	}

}
