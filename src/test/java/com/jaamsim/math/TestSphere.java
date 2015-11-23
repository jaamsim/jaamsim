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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestSphere {

@Test
public void testSphereDistance() {
	Sphere s0 = new Sphere(new Vec3d(0, 0, 0), 3);

	Sphere s1 = new Sphere(new Vec3d(3, 4, 0), 1);

	// sphere to sphere
	double dist0 = s0.getDistance(s1);
	double dist1 = s1.getDistance(s0);

	assertTrue(MathUtils.near(dist0, dist1));
	assertTrue(MathUtils.near(dist0, 1));

	// Sphere to plane
	Plane p = new Plane(new Vec4d(0, 1, 0, 1.0d), 1);
	dist0 = s1.getDistance(p);
	assertTrue(MathUtils.near(dist0, 2));

	// Sphere to point
	Vec4d point = new Vec4d(-3, 0, -4, 1.0d);
	dist0 = s0.getDistance(point);
	assertTrue(MathUtils.near(dist0, 2));

}

}
