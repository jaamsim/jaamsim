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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestVec3d extends TestVec {

@Test
public void testConstructors() {
	Vec3d vec3 = new Vec3d();
	assertEqual(vec3, zero);

	vec3 = new Vec3d(seq);
	assertEqual(vec3, seq);

	vec3 = new Vec3d(1.0d, 2.0d, 3.0d);
	assertEqual(vec3, seq);
}

@Test
public void testString() {
	Vec3d vec = new Vec3d();
	vec.set3(seq);
	assertTrue("1.0  2.0  3.0".equals(vec.toString()));

	vec.set3(nseq);
	assertTrue("-1.0  -2.0  -3.0".equals(vec.toString()));

	vec.set3(allNaN);
	assertTrue("NaN  NaN  NaN".equals(vec.toString()));

	vec.set3(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	assertTrue("Infinity  -Infinity  Infinity".equals(vec.toString()));
}

@Test
public void testEquals() {
	Vec3d vec = new Vec3d(seq);
	assertTrue(vec.equals2(seq));
	assertFalse(vec.equals2(one));
	assertTrue(vec.equals3(seq));
	assertFalse(vec.equals3(one));

	// Test -0.0 and 0.0 compare as equal
	vec.set3(-0.0d, -0.0d, -0.0d);
	assertTrue(vec.equals2(zero));
	assertTrue(vec.equals3(zero));

	// any vec containing NaN will compare as not equal, even itself
	vec.set3(allNaN);
	assertFalse(vec.equals2(allNaN));
	assertFalse(vec.equals3(allNaN));
	assertFalse(allNaN.equals3(allNaN));
}

@Test
public void testSet() {
	Vec3d vec;

	// Test set from another vec
	vec = new Vec3d(seq);
	vec.set2(zero);
	assertEqual(vec, zseq2);

	vec = new Vec3d(seq);
	vec.set3(zero);
	assertEqual(vec, zseq3);

	// Test set from doubles
	vec = new Vec3d(seq);
	vec.set2(0.0d, 0.0d);
	assertEqual(vec, zseq2);

	vec = new Vec3d(seq);
	vec.set3(0.0d, 0.0d, 0.0d);
	assertEqual(vec, zseq3);
}

@Test
public void testAdd() {
	Vec3d vec;

	// Test add into vec
	vec = new Vec3d(seq);
	vec.add2(seq);
	assertEqual(vec, seqadd2);

	vec = new Vec3d(seq);
	vec.add3(seq);
	assertEqual(vec, seqadd3);

	// Test set vec to sum
	vec = new Vec3d(seq);
	vec.add2(seq, seq);
	assertEqual(vec, seqadd2);

	vec = new Vec3d(seq);
	vec.add3(seq, seq);
	assertEqual(vec, seqadd3);
}

@Test
public void testSub() {
	Vec3d vec;

	// Test sub into vec
	vec = new Vec3d(seq);
	vec.sub2(one);
	assertEqual(vec, seqsub2);

	vec = new Vec3d(seq);
	vec.sub3(one);
	assertEqual(vec, seqsub3);

	// Test set vec to sub
	vec = new Vec3d(seq);
	vec.sub2(seq, one);
	assertEqual(vec, seqsub2);

	vec = new Vec3d(seq);
	vec.sub3(seq, one);
	assertEqual(vec, seqsub3);
}

@Test
public void testScale() {
	Vec3d vec;

	vec = new Vec3d(seq);
	vec.scale2(2.0d);
	assertEqual(vec, scaleseq2);

	vec = new Vec3d(seq);
	vec.scale3(2.0d);
	assertEqual(vec, scaleseq3);

	vec = new Vec3d(seq);
	vec.scale2(2.0d, seq);
	assertEqual(vec, scaleseq2);

	vec = new Vec3d(seq);
	vec.scale3(2.0d, seq);
	assertEqual(vec, scaleseq3);
}

@Test
public void testInterpolation() {
	Vec3d vec;

	// Test add into vec
	vec = new Vec3d(seq);

	vec.interpolate2(seq, scaleseq4, 0.0d);
	assertEqual(vec, seq);

	vec.interpolate2(seq, scaleseq4, 1.0d);
	assertEqual(vec, scaleseq2);

	vec.interpolate2(seq, scaleseq4, 0.5d);
	assertEqual(vec, interpseq2);

	vec.interpolate3(seq, scaleseq4, 0.0d);
	assertEqual(vec, seq);

	vec.interpolate3(seq, scaleseq4, 1.0d);
	assertEqual(vec, scaleseq3);

	vec.interpolate3(seq, scaleseq4, 0.5d);
	assertEqual(vec, interpseq3);
}

@Test
public void testMul() {
	Vec3d vec;

	// Test mul into vec
	vec = new Vec3d(seq);
	vec.mul2(seq);
	assertEqual(vec, seqmul2);

	vec = new Vec3d(seq);
	vec.mul3(seq);
	assertEqual(vec, seqmul3);

	// Test set vec to mul
	vec = new Vec3d(seq);
	vec.mul2(seq, seq);
	assertEqual(vec, seqmul2);

	vec = new Vec3d(seq);
	vec.mul3(seq, seq);
	assertEqual(vec, seqmul3);
}

@Test
public void testMin() {
	Vec3d vec;

	// Test mul into vec
	vec = new Vec3d(seq);
	vec.min2(nseq);
	assertEqual(vec, seqmin2);

	vec = new Vec3d(seq);
	vec.min3(nseq);
	assertEqual(vec, seqmin3);

	// Test set vec to mul
	vec = new Vec3d(seq);
	vec.min2(seq, nseq);
	assertEqual(vec, seqmin2);

	vec = new Vec3d(seq);
	vec.min3(seq, nseq);
	assertEqual(vec, seqmin3);
}

@Test
public void testMax() {
	Vec3d vec;

	// Test mul into vec
	vec = new Vec3d(nseq);
	vec.max2(seq);
	assertEqual(vec, seqmax2);

	vec = new Vec3d(nseq);
	vec.max3(seq);
	assertEqual(vec, seqmax3);

	// Test set vec to mul
	vec = new Vec3d(nseq);
	vec.max2(seq, nseq);
	assertEqual(vec, seqmax2);

	vec = new Vec3d(nseq);
	vec.max3(seq, nseq);
	assertEqual(vec, seqmax3);
}

@Test
public void testDot() {
	Vec3d vec = new Vec3d(3.0d, 4.0d, 5.0d);
	assertTrue(vec.dot2(one) == 7.0d);
	assertTrue(vec.dot2(seq) == 11.0d);

	assertTrue(vec.dot3(one) == 12.0d);
	assertTrue(vec.dot3(seq) == 26.0d);

	assertTrue(vec.mag2() == 5.0d);
	assertTrue(vec.mag3() == Math.sqrt(50.0d));

	assertTrue(vec.magSquare2() == 25.0d);
	assertTrue(vec.magSquare3() == 50.0d);
}

@Test
public void testCross3() {
	Vec3d vec;

	vec = new Vec3d(xaxis);
	vec.cross3(yaxis);
	assertEqual(vec, zaxis);

	vec = new Vec3d(yaxis);
	vec.cross3(xaxis);
	assertEqual(vec, negzaxis);

	vec = new Vec3d();
	vec.cross3(xaxis, yaxis);
	assertEqual(vec, zaxis);

	vec = new Vec3d();
	vec.cross3(yaxis, xaxis);
	assertEqual(vec, negzaxis);
}

@Test
public void testNormalize() {
	Vec3d vec;

	vec = new Vec3d(one);
	vec.normalize2();
	assertNear(vec.magSquare2(), 1.0d);
	assertNear(vec.mag2(), 1.0d);

	vec = new Vec3d(one);
	vec.normalize3();
	assertNear(vec.magSquare3(), 1.0d);
	assertNear(vec.mag3(), 1.0d);

	vec.normalize2(one);
	assertNear(vec.magSquare2(), 1.0d);
	assertNear(vec.mag2(), 1.0d);

	vec.normalize3(one);
	assertNear(vec.magSquare3(), 1.0d);
	assertNear(vec.mag3(), 1.0d);
}

@Test
public void testMult() {
	Vec3d vec = new Vec3d();

	vec.mult2(TestMat4d.seqOne, one);
	assertEqual(vec, mult2);

	vec.mult2(one, TestMat4d.seqOne);
	assertEqual(vec, mult2tpose);

	vec.mult3(TestMat4d.seqOne, one);
	assertEqual(vec, mult3);

	vec.mult3(one, TestMat4d.seqOne);
	assertEqual(vec, mult3tpose);
}

@Test
public void testMultAndTrans() {
	Vec3d vec = new Vec3d();
	Vec3d expected = new Vec3d();

	Mat4d transMat= new Mat4d( 1, 0, 0, 5,
                               0, 2, 0, 6,
                               0, 0, 3, 8,
                               0, 0, 0, 1 );

	vec.set3(1, 2, 3);
	expected.set3(6, 10, 17);

	vec.multAndTrans3(transMat, vec);
	assertNear(vec, expected);

	vec.set3(2, 4, 6);
	expected.set3(7, 14, 26);

	vec.multAndTrans3(transMat, vec);
	assertNear(vec, expected);
}

@Test
public void testNormDegen() {
	Vec3d vec;

	vec = new Vec3d(zero);
	vec.normalize2();
	assertEqual(vec, yaxis);

	vec = new Vec3d(posInf);
	vec.normalize2();
	assertEqual(vec, deg2posInf);

	vec = new Vec3d(negInf);
	vec.normalize2();
	assertEqual(vec, deg2negInf);

	vec = new Vec3d(mixInf);
	vec.normalize2();
	assertEqual(vec, deg2mixInf);

	vec = new Vec3d(mixInfNaN);
	vec.normalize2();
	assertEqualNaN(vec, deg2mixInfNaN);

	vec = new Vec3d(oneNaN);
	vec.normalize2();
	assertEqual(vec, yaxis);

	vec = new Vec3d(zero);
	vec.normalize3();
	assertEqual(vec, zaxis);

	vec = new Vec3d(posInf);
	vec.normalize3();
	assertEqual(vec, deg3posInf);

	vec = new Vec3d(negInf);
	vec.normalize3();
	assertEqual(vec, deg3negInf);

	vec = new Vec3d(mixInf);
	vec.normalize3();
	assertEqual(vec, deg3mixInf);

	vec = new Vec3d(mixInfNaN);
	vec.normalize3();
	assertEqual(vec, deg3mixInfNaN);

	vec = new Vec3d(oneNaN);
	vec.normalize3();
	assertEqual(vec, zaxis);

	// Test one-arg version
	vec = new Vec3d();
	vec.normalize2(zero);
	assertEqual(vec, yaxis);

	vec = new Vec3d();
	vec.normalize2(posInf);
	assertEqual(vec, yaxis);

	vec = new Vec3d();
	vec.normalize2(negInf);
	assertEqual(vec, yaxis);

	vec = new Vec3d();
	vec.normalize2(mixInf);
	assertEqual(vec, yaxis);

	vec = new Vec3d();
	vec.normalize2(mixInfNaN);
	assertEqual(vec, yaxis);

	vec = new Vec3d();
	vec.normalize2(oneNaN);
	assertEqual(vec, yaxis);

	vec = new Vec3d();
	vec.normalize3(zero);
	assertEqual(vec, zaxis);

	vec = new Vec3d();
	vec.normalize3(posInf);
	assertEqual(vec, zaxis);

	vec = new Vec3d();
	vec.normalize3(negInf);
	assertEqual(vec, zaxis);

	vec = new Vec3d();
	vec.normalize3(mixInf);
	assertEqual(vec, zaxis);

	vec = new Vec3d();
	vec.normalize3(mixInfNaN);
	assertEqual(vec, zaxis);

	vec = new Vec3d();
	vec.normalize3(oneNaN);
	assertEqual(vec, zaxis);
}
}
