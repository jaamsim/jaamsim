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

public class TestVec4d extends TestVec {

@Test
public void testConstructors() {
	Vec4d vec4 = new Vec4d();
	vec4.w = 0;
	assertEqual(vec4, zero);

	vec4 = new Vec4d(seq);
	assertEqual(vec4, seq);

	vec4 = new Vec4d(1.0d, 2.0d, 3.0d, 4.0d);
	assertEqual(vec4, seq);
}

@Test
public void testString() {
	Vec4d vec = new Vec4d();
	vec.set4(seq);
	assertTrue("(1.0, 2.0, 3.0, 4.0)".equals(vec.toString()));

	vec.set4(nseq);
	assertTrue("(-1.0, -2.0, -3.0, -4.0)".equals(vec.toString()));

	vec.set4(allNaN);
	assertTrue("(NaN, NaN, NaN, NaN)".equals(vec.toString()));

	vec.set4(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
	assertTrue("(Infinity, -Infinity, Infinity, -Infinity)".equals(vec.toString()));
}

@Test
public void testEquals() {
	Vec4d vec = new Vec4d(seq);
	assertTrue(vec.equals2(seq));
	assertFalse(vec.equals2(one));
	assertTrue(vec.equals3(seq));
	assertFalse(vec.equals3(one));
	assertTrue(vec.equals4(seq));
	assertFalse(vec.equals4(one));

	// Test -0.0 and 0.0 compare as equal
	vec.set4(-0.0d, -0.0d, -0.0d, -0.0d);
	assertTrue(vec.equals2(zero));
	assertTrue(vec.equals3(zero));
	assertTrue(vec.equals4(zero));

	// any vec containing NaN will compare as not equal, even itself
	vec.set4(allNaN);
	assertFalse(vec.equals2(allNaN));
	assertFalse(vec.equals3(allNaN));
	assertFalse(vec.equals4(allNaN));
	assertFalse(allNaN.equals4(allNaN));
}

@Test
public void testSet() {
	Vec4d vec;

	// Test set from another vec
	vec = new Vec4d(seq);
	vec.set2(zero);
	assertEqual(vec, zseq2);

	vec = new Vec4d(seq);
	vec.set3(zero);
	assertEqual(vec, zseq3);

	vec = new Vec4d(seq);
	vec.set4(zero);
	assertEqual(vec, zseq4);

	// Test set from doubles
	vec = new Vec4d(seq);
	vec.set2(0.0d, 0.0d);
	assertEqual(vec, zseq2);

	vec = new Vec4d(seq);
	vec.set3(0.0d, 0.0d, 0.0d);
	assertEqual(vec, zseq3);

	vec = new Vec4d(seq);
	vec.set4(0.0d, 0.0d, 0.0d, 0.0d);
	assertEqual(vec, zseq4);
}

@Test
public void testAdd() {
	Vec4d vec;

	// Test add into vec
	vec = new Vec4d(seq);
	vec.add2(seq);
	assertEqual(vec, seqadd2);

	vec = new Vec4d(seq);
	vec.add3(seq);
	assertEqual(vec, seqadd3);

	vec = new Vec4d(seq);
	vec.add4(seq);
	assertEqual(vec, seqadd4);

	// Test set vec to sum
	vec = new Vec4d(seq);
	vec.add2(seq, seq);
	assertEqual(vec, seqadd2);

	vec = new Vec4d(seq);
	vec.add3(seq, seq);
	assertEqual(vec, seqadd3);

	vec = new Vec4d(seq);
	vec.add4(seq, seq);
	assertEqual(vec, seqadd4);
}

@Test
public void testSub() {
	Vec4d vec;

	// Test sub into vec
	vec = new Vec4d(seq);
	vec.sub2(one);
	assertEqual(vec, seqsub2);

	vec = new Vec4d(seq);
	vec.sub3(one);
	assertEqual(vec, seqsub3);

	vec = new Vec4d(seq);
	vec.sub4(one);
	assertEqual(vec, seqsub4);

	// Test set vec to sub
	vec = new Vec4d(seq);
	vec.sub2(seq, one);
	assertEqual(vec, seqsub2);

	vec = new Vec4d(seq);
	vec.sub3(seq, one);
	assertEqual(vec, seqsub3);

	vec = new Vec4d(seq);
	vec.sub4(seq, one);
	assertEqual(vec, seqsub4);
}

@Test
public void testScale() {
	Vec4d vec;

	vec = new Vec4d(seq);
	vec.scale2(2.0d);
	assertEqual(vec, scaleseq2);

	vec = new Vec4d(seq);
	vec.scale3(2.0d);
	assertEqual(vec, scaleseq3);

	vec = new Vec4d(seq);
	vec.scale4(2.0d);
	assertEqual(vec, scaleseq4);

	vec = new Vec4d(seq);
	vec.scale2(2.0d, seq);
	assertEqual(vec, scaleseq2);

	vec = new Vec4d(seq);
	vec.scale3(2.0d, seq);
	assertEqual(vec, scaleseq3);

	vec = new Vec4d(seq);
	vec.scale4(2.0d, seq);
	assertEqual(vec, scaleseq4);
}

@Test
public void testInterpolation() {
	Vec4d vec;

	// Test add into vec
	vec = new Vec4d(seq);

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

	vec.interpolate4(seq, scaleseq4, 0.0d);
	assertEqual(vec, seq);

	vec.interpolate4(seq, scaleseq4, 1.0d);
	assertEqual(vec, scaleseq4);

	vec.interpolate4(seq, scaleseq4, 0.5d);
	assertEqual(vec, interpseq4);
}

@Test
public void testMul() {
	Vec4d vec;

	// Test mul into vec
	vec = new Vec4d(seq);
	vec.mul2(seq);
	assertEqual(vec, seqmul2);

	vec = new Vec4d(seq);
	vec.mul3(seq);
	assertEqual(vec, seqmul3);

	vec = new Vec4d(seq);
	vec.mul4(seq);
	assertEqual(vec, seqmul4);

	// Test set vec to mul
	vec = new Vec4d(seq);
	vec.mul2(seq, seq);
	assertEqual(vec, seqmul2);

	vec = new Vec4d(seq);
	vec.mul3(seq, seq);
	assertEqual(vec, seqmul3);

	vec = new Vec4d(seq);
	vec.mul4(seq, seq);
	assertEqual(vec, seqmul4);
}

@Test
public void testMin() {
	Vec4d vec;

	// Test mul into vec
	vec = new Vec4d(seq);
	vec.min2(nseq);
	assertEqual(vec, seqmin2);

	vec = new Vec4d(seq);
	vec.min3(nseq);
	assertEqual(vec, seqmin3);

	vec = new Vec4d(seq);
	vec.min4(nseq);
	assertEqual(vec, seqmin4);

	// Test set vec to mul
	vec = new Vec4d(seq);
	vec.min2(seq, nseq);
	assertEqual(vec, seqmin2);

	vec = new Vec4d(seq);
	vec.min3(seq, nseq);
	assertEqual(vec, seqmin3);

	vec = new Vec4d(seq);
	vec.min4(seq, nseq);
	assertEqual(vec, seqmin4);
}

@Test
public void testMax() {
	Vec4d vec;

	// Test mul into vec
	vec = new Vec4d(nseq);
	vec.max2(seq);
	assertEqual(vec, seqmax2);

	vec = new Vec4d(nseq);
	vec.max3(seq);
	assertEqual(vec, seqmax3);

	vec = new Vec4d(nseq);
	vec.max4(seq);
	assertEqual(vec, seqmax4);

	// Test set vec to mul
	vec = new Vec4d(nseq);
	vec.max2(seq, nseq);
	assertEqual(vec, seqmax2);

	vec = new Vec4d(nseq);
	vec.max3(seq, nseq);
	assertEqual(vec, seqmax3);

	vec = new Vec4d(nseq);
	vec.max4(seq, nseq);
	assertEqual(vec, seqmax4);
}

@Test
public void testDot() {
	Vec4d vec = new Vec4d(3.0d, 4.0d, 5.0d, 6.0d);
	assertTrue(vec.dot2(one) == 7.0d);
	assertTrue(vec.dot2(seq) == 11.0d);

	assertTrue(vec.dot3(one) == 12.0d);
	assertTrue(vec.dot3(seq) == 26.0d);

	assertTrue(vec.dot4(one) == 18.0d);
	assertTrue(vec.dot4(seq) == 50.0d);

	assertTrue(vec.mag2() == 5.0d);
	assertTrue(vec.mag3() == Math.sqrt(50.0d));
	assertTrue(vec.mag4() == Math.sqrt(86.0d));

	assertTrue(vec.magSquare2() == 25.0d);
	assertTrue(vec.magSquare3() == 50.0d);
	assertTrue(vec.magSquare4() == 86.0d);
}

@Test
public void testCross3() {
	Vec4d vec;

	vec = new Vec4d(xaxis);
	vec.w = 0;
	vec.cross3(yaxis);
	assertEqual(vec, zaxis);

	vec = new Vec4d(yaxis);
	vec.w = 0;
	vec.cross3(xaxis);
	assertEqual(vec, negzaxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.cross3(xaxis, yaxis);
	assertEqual(vec, zaxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.cross3(yaxis, xaxis);
	assertEqual(vec, negzaxis);
}

@Test
public void testNormalize() {
	Vec4d vec;

	vec = new Vec4d(one);
	vec.normalize2();
	assertNear(vec.magSquare2(), 1.0d);
	assertNear(vec.mag2(), 1.0d);

	vec = new Vec4d(one);
	vec.normalize3();
	assertNear(vec.magSquare3(), 1.0d);
	assertNear(vec.mag3(), 1.0d);

	vec = new Vec4d(one);
	vec.normalize4();
	assertNear(vec.magSquare4(), 1.0d);
	assertNear(vec.mag4(), 1.0d);

	vec.normalize2(one);
	assertNear(vec.magSquare2(), 1.0d);
	assertNear(vec.mag2(), 1.0d);

	vec.normalize3(one);
	assertNear(vec.magSquare3(), 1.0d);
	assertNear(vec.mag3(), 1.0d);

	vec.normalize4(one);
	assertNear(vec.magSquare4(), 1.0d);
	assertNear(vec.mag4(), 1.0d);
}

@Test
public void testMult() {
	Vec4d vec = new Vec4d();
	vec.w = 0;

	vec.mult2(TestMat4d.seqOne, one);
	assertEqual(vec, mult2);

	vec.mult2(one, TestMat4d.seqOne);
	assertEqual(vec, mult2tpose);

	vec.mult3(TestMat4d.seqOne, one);
	assertEqual(vec, mult3);

	vec.mult3(one, TestMat4d.seqOne);
	assertEqual(vec, mult3tpose);

	vec.mult4(TestMat4d.seqOne, one);
	assertEqual(vec, mult4);

	vec.mult4(one, TestMat4d.seqOne);
	assertEqual(vec, mult4tpose);
}

@Test
public void testNormDegen() {
	Vec4d vec;

	vec = new Vec4d(zero);
	vec.normalize2();
	assertEqual(vec, yaxis);

	vec = new Vec4d(posInf);
	vec.normalize2();
	assertEqual(vec, deg2posInf);

	vec = new Vec4d(negInf);
	vec.normalize2();
	assertEqual(vec, deg2negInf);

	vec = new Vec4d(mixInf);
	vec.normalize2();
	assertEqual(vec, deg2mixInf);

	vec = new Vec4d(mixInfNaN);
	vec.normalize2();
	assertEqualNaN(vec, deg2mixInfNaN);

	vec = new Vec4d(oneNaN);
	vec.normalize2();
	assertEqual(vec, yaxis);

	vec = new Vec4d(zero);
	vec.normalize3();
	assertEqual(vec, zaxis);

	vec = new Vec4d(posInf);
	vec.normalize3();
	assertEqual(vec, deg3posInf);

	vec = new Vec4d(negInf);
	vec.normalize3();
	assertEqual(vec, deg3negInf);

	vec = new Vec4d(mixInf);
	vec.normalize3();
	assertEqual(vec, deg3mixInf);

	vec = new Vec4d(mixInfNaN);
	vec.normalize3();
	assertEqual(vec, deg3mixInfNaN);

	vec = new Vec4d(oneNaN);
	vec.normalize3();
	assertEqual(vec, zaxis);

	vec = new Vec4d(zero);
	vec.normalize4();
	assertEqual(vec, waxis);

	vec = new Vec4d(posInf);
	vec.normalize4();
	assertEqual(vec, waxis);

	vec = new Vec4d(negInf);
	vec.normalize4();
	assertEqual(vec, waxis);

	vec = new Vec4d(mixInf);
	vec.normalize4();
	assertEqual(vec, waxis);

	vec = new Vec4d(mixInfNaN);
	vec.normalize4();
	assertEqual(vec, waxis);

	vec = new Vec4d(oneNaN);
	vec.normalize4();
	assertEqual(vec, waxis);

	// Test one-arg version
	vec = new Vec4d();
	vec.w = 0;
	vec.normalize2(zero);
	assertEqual(vec, yaxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.normalize2(posInf);
	assertEqual(vec, yaxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.normalize2(negInf);
	assertEqual(vec, yaxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.normalize2(mixInf);
	assertEqual(vec, yaxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.normalize2(mixInfNaN);
	assertEqual(vec, yaxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.normalize2(oneNaN);
	assertEqual(vec, yaxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.normalize3(zero);
	assertEqual(vec, zaxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.normalize3(posInf);
	assertEqual(vec, zaxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.normalize3(negInf);
	assertEqual(vec, zaxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.normalize3(mixInf);
	assertEqual(vec, zaxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.normalize3(mixInfNaN);
	assertEqual(vec, zaxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.normalize3(oneNaN);
	assertEqual(vec, zaxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.normalize4(zero);
	assertEqual(vec, waxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.normalize4(posInf);
	assertEqual(vec, waxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.normalize4(negInf);
	assertEqual(vec, waxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.normalize4(mixInf);
	assertEqual(vec, waxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.normalize4(mixInfNaN);
	assertEqual(vec, waxis);

	vec = new Vec4d();
	vec.w = 0;
	vec.normalize4(oneNaN);
	assertEqual(vec, waxis);
}
}
