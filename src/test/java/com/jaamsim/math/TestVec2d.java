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

public class TestVec2d extends TestVec {

@Test
public void testConstructors() {
	Vec2d vec2 = new Vec2d();
	assertEqual(vec2, zero);

	vec2 = new Vec2d(seq);
	assertEqual(vec2, seq);

	vec2 = new Vec2d(1.0d, 2.0d);
	assertEqual(vec2, seq);
}

@Test
public void testString() {
	Vec2d vec = new Vec2d();
	vec.set2(seq);
	assertTrue("(1.0, 2.0)".equals(vec.toString()));

	vec.set2(nseq);
	assertTrue("(-1.0, -2.0)".equals(vec.toString()));

	vec.set2(allNaN);
	assertTrue("(NaN, NaN)".equals(vec.toString()));

	vec.set2(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
	assertTrue("(Infinity, -Infinity)".equals(vec.toString()));
}

@Test
public void testEquals() {
	Vec2d vec = new Vec2d(seq);
	assertTrue(vec.equals2(seq));
	assertFalse(vec.equals2(one));

	// Test -0.0 and 0.0 compare as equal
	vec.set2(-0.0d, -0.0d);
	assertTrue(vec.equals2(zero));

	// any vec containing NaN will compare as not equal, even itself
	vec.set2(allNaN);
	assertFalse(vec.equals2(allNaN));
	assertFalse(allNaN.equals2(allNaN));
}

@Test
public void testSet() {
	Vec2d vec;

	// Test set from another vec
	vec = new Vec2d(seq);
	vec.set2(zero);
	assertEqual(vec, zseq2);

	// Test set from doubles
	vec = new Vec2d(seq);
	vec.set2(0.0d, 0.0d);
	assertEqual(vec, zseq2);
}

@Test
public void testAdd() {
	Vec2d vec;

	// Test add into vec
	vec = new Vec2d(seq);
	vec.add2(seq);
	assertEqual(vec, seqadd2);

	// Test set vec to sum
	vec = new Vec2d(seq);
	vec.add2(seq, seq);
	assertEqual(vec, seqadd2);
}

@Test
public void testSub() {
	Vec2d vec;

	// Test sub into vec
	vec = new Vec2d(seq);
	vec.sub2(one);
	assertEqual(vec, seqsub2);

	// Test set vec to sub
	vec = new Vec2d(seq);
	vec.sub2(seq, one);
	assertEqual(vec, seqsub2);
}

@Test
public void testScale() {
	Vec2d vec;

	vec = new Vec2d(seq);
	vec.scale2(2.0d);
	assertEqual(vec, scaleseq2);

	vec = new Vec2d(seq);
	vec.scale2(2.0d, seq);
	assertEqual(vec, scaleseq2);
}

@Test
public void testInterpolation() {
	Vec2d vec;

	// Test add into vec
	vec = new Vec2d(seq);

	vec.interpolate2(seq, scaleseq4, 0.0d);
	assertEqual(vec, seq);

	vec.interpolate2(seq, scaleseq4, 1.0d);
	assertEqual(vec, scaleseq2);

	vec.interpolate2(seq, scaleseq4, 0.5d);
	assertEqual(vec, interpseq2);
}

@Test
public void testMul() {
	Vec2d vec;

	// Test mul into vec
	vec = new Vec2d(seq);
	vec.mul2(seq);
	assertEqual(vec, seqmul2);

	// Test set vec to mul
	vec = new Vec2d(seq);
	vec.mul2(seq, seq);
	assertEqual(vec, seqmul2);
}

@Test
public void testMin() {
	Vec2d vec;

	// Test mul into vec
	vec = new Vec2d(seq);
	vec.min2(nseq);
	assertEqual(vec, seqmin2);

	// Test set vec to mul
	vec = new Vec2d(seq);
	vec.min2(seq, nseq);
	assertEqual(vec, seqmin2);
}

@Test
public void testMax() {
	Vec2d vec;

	// Test mul into vec
	vec = new Vec2d(nseq);
	vec.max2(seq);
	assertEqual(vec, seqmax2);

	// Test set vec to mul
	vec = new Vec2d(nseq);
	vec.max2(seq, nseq);
	assertEqual(vec, seqmax2);
}

@Test
public void testDot() {
	Vec2d vec = new Vec2d(3.0d, 4.0d);

	assertTrue(vec.dot2(one) == 7.0d);
	assertTrue(vec.dot2(seq) == 11.0d);

	assertTrue(vec.mag2() == 5.0d);
	assertTrue(vec.magSquare2() == 25.0d);
}

@Test
public void testNormalize() {
	Vec2d vec;

	vec = new Vec2d(one);
	vec.normalize2();
	assertNear(vec.magSquare2(), 1.0d);
	assertNear(vec.mag2(), 1.0d);

	vec.normalize2(one);
	assertNear(vec.magSquare2(), 1.0d);
	assertNear(vec.mag2(), 1.0d);
}

@Test
public void testMult() {
	Vec2d vec = new Vec2d();

	vec.mult2(TestMat4d.seqOne, one);
	assertEqual(vec, mult2);

	vec.mult2(one, TestMat4d.seqOne);
	assertEqual(vec, mult2tpose);
}

@Test
public void testNormDegen() {
	Vec2d vec;

	vec = new Vec2d(zero);
	vec.normalize2();
	assertEqual(vec, yaxis);

	vec = new Vec2d(posInf);
	vec.normalize2();
	assertEqual(vec, deg2posInf);

	vec = new Vec2d(negInf);
	vec.normalize2();
	assertEqual(vec, deg2negInf);

	vec = new Vec2d(mixInf);
	vec.normalize2();
	assertEqual(vec, deg2mixInf);

	vec = new Vec2d(mixInfNaN);
	vec.normalize2();
	assertEqual(vec, deg2mixInfNaN);

	vec = new Vec2d(oneNaN);
	vec.normalize2();
	assertEqual(vec, yaxis);

	// Test one-arg version
	vec = new Vec2d();
	vec.normalize2(zero);
	assertEqual(vec, yaxis);

	vec = new Vec2d();
	vec.normalize2(posInf);
	assertEqual(vec, yaxis);

	vec = new Vec2d();
	vec.normalize2(negInf);
	assertEqual(vec, yaxis);

	vec = new Vec2d();
	vec.normalize2(mixInf);
	assertEqual(vec, yaxis);

	vec = new Vec2d();
	vec.normalize2(mixInfNaN);
	assertEqual(vec, yaxis);

	vec = new Vec2d();
	vec.normalize2(oneNaN);
	assertEqual(vec, yaxis);
}

}
