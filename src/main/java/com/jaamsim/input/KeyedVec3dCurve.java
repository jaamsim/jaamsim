/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
