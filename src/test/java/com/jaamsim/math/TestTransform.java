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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestTransform {

@Test
public void testIdentity() {
	Transform t = new Transform();
	Vec4d in = new Vec4d(1, 2, 3, 1.0d);
	Vec4d expected = new Vec4d(in);

	Vec4d res = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
	t.apply(in, res);
	assertTrue(res.equals4(expected));
}

@Test
public void testTranslation() {
	Transform t = new Transform();
	Vec4d trans = new Vec4d(3, 4, 5, 1.0d);
	t.setTrans(trans);
	Vec4d in = new Vec4d(1, 2, 3, 1.0d);

	Vec4d expected = new Vec4d(4, 6, 8, 1.0d);

	Vec4d res = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
	t.apply(in, res);
	assertTrue(res.equals4(expected));
}

@Test
public void testRotation() {
	Transform t = new Transform();
	Quaternion rot = new Quaternion();
	rot.setRotXAxis(Math.PI/2);
	t.setRot(rot);
	Vec4d in = new Vec4d(1, 2, 3, 1.0d);

	Vec4d expected = new Vec4d(1, -3, 2, 1.0d);

	Vec4d res = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
	t.apply(in, res);
	assertTrue(res.near4(expected));
}

@Test
public void testScale() {
	Transform t = new Transform();
	t.setScale(3);
	Vec4d in = new Vec4d(1, 2, 3, 1.0d);

	Vec4d expected = new Vec4d(3, 6, 9, 1.0d);

	Vec4d res = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
	t.apply(in, res);
	assertTrue(res.near4(expected));
}

@Test
public void testInverse()
{
	Vec4d trans = new Vec4d(1, 2, 3, 1.0d);
	Quaternion rot = new Quaternion();
	rot.setRotZAxis(Math.PI/4);
	Transform t = new Transform(trans, rot, 3);

	Transform invT = new Transform();
	t.inverse(invT);

	Transform ident = new Transform();

	Transform test = new Transform();
	test.merge(t, invT);

	assertTrue(test.near(ident));

	Mat4d mat = new Mat4d();
	Mat4d matInv = new Mat4d();
	t.getMat4d(mat);
	invT.getMat4d(matInv);

	Mat4d identMat = new Mat4d();
	Mat4d resMat = new Mat4d();

	resMat.mult4(mat, matInv);
	assertNear(resMat, identMat);

	// Test inverse both ways
	resMat.mult4(matInv, mat);
	assertNear(resMat, identMat);
}

private static final double EPS = 1e-12d;
public static void assertNear(Mat4d m1, Mat4d m2) {
	assertEquals(m1.d00, m2.d00, EPS);
	assertEquals(m1.d01, m2.d01, EPS);
	assertEquals(m1.d02, m2.d02, EPS);
	assertEquals(m1.d03, m2.d03, EPS);

	assertEquals(m1.d10, m2.d10, EPS);
	assertEquals(m1.d11, m2.d11, EPS);
	assertEquals(m1.d12, m2.d12, EPS);
	assertEquals(m1.d13, m2.d13, EPS);

	assertEquals(m1.d20, m2.d20, EPS);
	assertEquals(m1.d21, m2.d21, EPS);
	assertEquals(m1.d22, m2.d22, EPS);
	assertEquals(m1.d23, m2.d23, EPS);

	assertEquals(m1.d30, m2.d30, EPS);
	assertEquals(m1.d31, m2.d31, EPS);
	assertEquals(m1.d32, m2.d32, EPS);
	assertEquals(m1.d33, m2.d33, EPS);
}

/**
 * Make sure the transforms can write to themselves cleanly
 */
@Test
public void testSelfAssignment() {
	Vec4d trans1 = new Vec4d(1, 2, 3, 1.0d);
	Quaternion rot1 = new Quaternion();
	rot1.setRotZAxis(Math.PI/4);
	Transform t1 = new Transform(trans1, rot1, 3);

	Vec4d trans2 = new Vec4d(3, 2, 1, 1.0d);
	Quaternion rot2 = new Quaternion();
	rot2.setRotXAxis(Math.PI/3);
	Transform t2 = new Transform(trans2, rot2, 6);

	Transform t3 = new Transform();
	t3.merge(t1, t2);
	t1.merge(t1, t2);
	assertTrue(t1.equals(t3));

	t3.merge(t1, t2);
	t2.merge(t1, t2);
	assertTrue(t2.equals(t3));

	Transform t4 = new Transform();
	t2.inverse(t4);
	t2.inverse(t2);
	assertTrue(t2.equals(t4));
}

}
