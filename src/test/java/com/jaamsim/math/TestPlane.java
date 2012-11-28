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

import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Plane;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;

public class TestPlane {

private Plane p;

@Before
public void setup() {
	p = new Plane(new Vector4d(1, 1, 0), Math.sqrt(2));
}

@Test
public void TestDist() {
	double dist = p.getNormalDist(new Vector4d(1, 1, 1));
	assertTrue(MathUtils.near(dist, 0));

	dist = p.getNormalDist(new Vector4d(1, 1, 42));
	assertTrue(MathUtils.near(dist, 0));

	dist = p.getNormalDist(new Vector4d(2, 0, 42));
	assertTrue(MathUtils.near(dist, 0));

	dist = p.getNormalDist(new Vector4d(0, 0, 0));
	assertTrue(MathUtils.near(dist, -Math.sqrt(2)));
}

@Test
public void TestConstruct() {
	Plane p0 = new Plane(new Vector4d(0, 0, 1), 2);
	Plane p1 = new Plane(new Vector4d(0, 1, 2),
	                     new Vector4d(1, 1, 2),
	                     new Vector4d(-42, 12, 2));

	assertTrue(p0.equals(p1));
}

@Test
public void TestTransform() {
	Plane p0 = new Plane(new Vector4d(1, 0, 0), 13);

	Transform tx = new Transform(new Vector4d(0, 0, 0), Quaternion.Rotation(Math.PI, Vector4d.X_AXIS), 1);


	// Rotating P0 around the X axis should have no effect
	Plane res = new Plane();
	p0.transform(tx,  res);

	assertTrue(p0.equals(res));

	Transform ty = new Transform(new Vector4d(0, 0, 3), Quaternion.Rotation(Math.PI/2, Vector4d.Y_AXIS), 2);

	// Rotation around the y axis should point the plane in the -Z direction
	Plane expected = new Plane(new Vector4d(0, 0, -1), 23);
	p0.transform(ty, res);

	assertTrue(expected.equals(res));


	// Now test self assignment
	p0.transform(ty, p0);
	assertTrue(expected.equals(p0));
}

} // class TestPlane
