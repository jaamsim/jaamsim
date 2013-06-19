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
