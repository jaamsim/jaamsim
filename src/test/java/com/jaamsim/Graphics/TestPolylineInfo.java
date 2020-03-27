/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2020 JaamSim Software Inc.
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
package com.jaamsim.Graphics;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Vec3d;

public class TestPolylineInfo {

	@Test
	public void testGetNearestPosition() {
		ArrayList<Vec3d> pts = new ArrayList<>();
		pts.add(new Vec3d(2.0d, 1.0d, 0.0d));
		pts.add(new Vec3d(4.0d, 1.0d, 0.0d));
		pts.add(new Vec3d(6.0d, 3.0d, 0.0d));
		double length = PolylineInfo.getLength(pts);

		double pos1 = PolylineInfo.getNearestPosition(pts, new Vec3d(3.0d, 2.0d, 0.0d));
		//System.out.format("pos1=%s%n", pos1);
		assertTrue( MathUtils.near(pos1, 1.0d/length) );

		double pos2 = PolylineInfo.getNearestPosition(pts, new Vec3d(4.9d, 2.1d, 0.0d));
		//System.out.format("pos2=%s%n", pos2);
		assertTrue( MathUtils.near(pos2, (2.0d + Math.sqrt(2.0d))/length) );
	}

}
