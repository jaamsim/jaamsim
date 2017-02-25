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
	Quaternion q = new Quaternion();
	q.setRotXAxis(Math.PI);
	Transform tx = new Transform(new Vec4d(0, 0, 0, 1.0d), q, 1);

	// Rotating P0 around the X axis should have no effect
	Plane res = new Plane();
	res.transform(tx,  p0);

	assertTrue(p0.near(res));

	q.setRotYAxis(Math.PI / 2);
	Transform ty = new Transform(new Vec4d(0, 0, 3, 1.0d), q, 2);

	// Rotation around the y axis should point the plane in the -Z direction
	Plane expected = new Plane(new Vec4d(0, 0, -1, 1.0d), 23);
	res.transform(ty, p0);

	assertTrue(expected.near(res));


	// Now test self assignment
	p0.transform(ty, p0);
	assertTrue(expected.near(p0));
}

@Test
public void testIntersect() {
	Vec4d xAxis = new Vec4d(1, 0, 0, 0);
	Vec4d yAxis = new Vec4d(0, 1, 0, 0);
	Vec4d zAxis = new Vec4d(0, 0, 1, 0);

	Plane pXY = new Plane(zAxis, 0);
	Plane pXZ = new Plane(yAxis, 0);
	Plane pYZ = new Plane(xAxis, 0);

	Vec3d intersect = MathUtils.collidePlanes(pXY, pXZ, pYZ);

	Vec3d origin = new Vec3d();

	assertTrue(intersect.near3(origin));

	Plane pXY1 = new Plane(zAxis, 1.0);
	Vec3d expected = new Vec3d(0,0,1);
	intersect = MathUtils.collidePlanes(pXY1, pXZ, pYZ);
	assertTrue(intersect.near3(expected));

	// Try other permutations
	intersect = MathUtils.collidePlanes(pXZ, pXY1, pYZ);
	assertTrue(intersect.near3(expected));
	intersect = MathUtils.collidePlanes(pXZ, pYZ, pXY1);
	assertTrue(intersect.near3(expected));

	// Check parallel handling
	intersect = MathUtils.collidePlanes(pXZ, pYZ, pYZ);
	assertTrue(intersect == null);

}

} // class TestPlane
