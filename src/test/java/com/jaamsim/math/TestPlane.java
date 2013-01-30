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

import org.junit.Before;
import org.junit.Test;

public class TestPlane {

private Plane p;

@Before
public void setup() {
	p = new Plane(new Vec4d(1, 1, 0, 1.0d), Math.sqrt(2));
}

@Test
public void TestDist() {
	double dist = p.getNormalDist(new Vec4d(1, 1, 1, 1.0d));
	assertTrue(MathUtils.near(dist, 0));

	dist = p.getNormalDist(new Vec4d(1, 1, 42, 1.0d));
	assertTrue(MathUtils.near(dist, 0));

	dist = p.getNormalDist(new Vec4d(2, 0, 42, 1.0d));
	assertTrue(MathUtils.near(dist, 0));

	dist = p.getNormalDist(new Vec4d(0, 0, 0, 1.0d));
	assertTrue(MathUtils.near(dist, -Math.sqrt(2)));
}

@Test
public void TestConstruct() {
	Plane p0 = new Plane(new Vec4d(0, 0, 1, 1.0d), 2);
	Plane p1 = new Plane(new Vec4d(0, 1, 2, 1.0d),
	                     new Vec4d(1, 1, 2, 1.0d),
	                     new Vec4d(-42, 12, 2, 1.0d));

	assertTrue(p0.near(p1));
}

@Test
public void TestTransform() {
	Plane p0 = new Plane(new Vec4d(1, 0, 0, 1.0d), 13);

	Transform tx = new Transform(new Vec4d(0, 0, 0, 1.0d), Quaternion.Rotation(Math.PI, Vec4d.X_AXIS), 1);

	// Rotating P0 around the X axis should have no effect
	Plane res = new Plane();
	p0.transform(tx,  res);

	assertTrue(p0.near(res));

	Transform ty = new Transform(new Vec4d(0, 0, 3, 1.0d), Quaternion.Rotation(Math.PI/2, Vec4d.Y_AXIS), 2);

	// Rotation around the y axis should point the plane in the -Z direction
	Plane expected = new Plane(new Vec4d(0, 0, -1, 1.0d), 23);
	p0.transform(ty, res);

	assertTrue(expected.near(res));


	// Now test self assignment
	p0.transform(ty, p0);
	assertTrue(expected.near(p0));
}

} // class TestPlane
