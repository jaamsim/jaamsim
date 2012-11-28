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

import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;

public class TestVec {

public static final Vec4d zero = new Vec4d();
public static final Vec4d one = new Vec4d(1.0d, 1.0d, 1.0d, 1.0d);
public static final Vec4d negone = new Vec4d(-1.0d, -1.0d, -1.0d, -1.0d);
public static final Vec4d seq = new Vec4d(1.0d, 2.0d, 3.0d, 4.0d);
public static final Vec4d nseq = new Vec4d(-1.0d, -2.0d, -3.0d, -4.0d);

//Single-axis Vectors
public static final Vec4d xaxis = new Vec4d(1.0d, 0.0d, 0.0d, 0.0d);
public static final Vec4d yaxis = new Vec4d(0.0d, 1.0d, 0.0d, 0.0d);
public static final Vec4d zaxis = new Vec4d(0.0d, 0.0d, 1.0d, 0.0d);
public static final Vec4d waxis = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);

public static final Vec4d negxaxis = new Vec4d(-1.0d, 0.0d, 0.0d, 0.0d);
public static final Vec4d negyaxis = new Vec4d(0.0d, -1.0d, 0.0d, 0.0d);
public static final Vec4d negzaxis = new Vec4d(0.0d, 0.0d, -1.0d, 0.0d);
public static final Vec4d negwaxis = new Vec4d(0.0d, 0.0d, 0.0d, -1.0d);

// Used to test partially zeroed sequence
public static final Vec4d zseq2 = new Vec4d(0.0d, 0.0d, 3.0d, 4.0d);
public static final Vec4d zseq3 = new Vec4d(0.0d, 0.0d, 0.0d, 4.0d);
public static final Vec4d zseq4 = new Vec4d(0.0d, 0.0d, 0.0d, 0.0d);

// Used to test partially added sequences
public static final Vec4d seqadd2 = new Vec4d(2.0d, 4.0d, 3.0d, 4.0d);
public static final Vec4d seqadd3 = new Vec4d(2.0d, 4.0d, 6.0d, 4.0d);
public static final Vec4d seqadd4 = new Vec4d(2.0d, 4.0d, 6.0d, 8.0d);

//Used to test partially subtracted values
public static final Vec4d seqsub2 = new Vec4d(0.0d, 1.0d, 3.0d, 4.0d);
public static final Vec4d seqsub3 = new Vec4d(0.0d, 1.0d, 2.0d, 4.0d);
public static final Vec4d seqsub4 = new Vec4d(0.0d, 1.0d, 2.0d, 3.0d);

//Used to test partially multiplied values
public static final Vec4d seqmul2 = new Vec4d(1.0d, 4.0d, 3.0d, 4.0d);
public static final Vec4d seqmul3 = new Vec4d(1.0d, 4.0d, 9.0d, 4.0d);
public static final Vec4d seqmul4 = new Vec4d(1.0d, 4.0d, 9.0d, 16.0d);

//Used to test max/min values
public static final Vec4d seqmin2 = new Vec4d(-1.0d, -2.0d,  3.0d,  4.0d);
public static final Vec4d seqmin3 = new Vec4d(-1.0d, -2.0d, -3.0d,  4.0d);
public static final Vec4d seqmin4 = new Vec4d(-1.0d, -2.0d, -3.0d, -4.0d);
public static final Vec4d seqmax2 = new Vec4d(1.0d, 2.0d, -3.0d, -4.0d);
public static final Vec4d seqmax3 = new Vec4d(1.0d, 2.0d,  3.0d, -4.0d);
public static final Vec4d seqmax4 = new Vec4d(1.0d, 2.0d,  3.0d,  4.0d);

//Used to test scaling
public static final Vec4d scaleseq2 = new Vec4d(2.0d, 4.0d, 3.0d, 4.0d);
public static final Vec4d scaleseq3 = new Vec4d(2.0d, 4.0d, 6.0d, 4.0d);
public static final Vec4d scaleseq4 = new Vec4d(2.0d, 4.0d, 6.0d, 8.0d);

//Assorted garbage matrices to test fallbacks.
public static final Vec4d posInf = new Vec4d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
public static final Vec4d negInf = new Vec4d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
public static final Vec4d mixInf = new Vec4d(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
public static final Vec4d allNaN = new Vec4d(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
public static final Vec4d mixInfNaN = new Vec4d(Double.NaN, Double.POSITIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY);
public static final Vec4d oneNaN = new Vec4d(Double.NaN, 0.0d, 0.0d, 0.0d);

//Tests the degenerate normalize cases
public static final Vec4d deg2posInf = new Vec4d(0.0d, 1.0d, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
public static final Vec4d deg3posInf = new Vec4d(0.0d, 0.0d, 1.0d, Double.POSITIVE_INFINITY);

public static final Vec4d deg2negInf = new Vec4d(0.0d, 1.0d, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
public static final Vec4d deg3negInf = new Vec4d(0.0d, 0.0d, 1.0d, Double.NEGATIVE_INFINITY);

public static final Vec4d deg2mixInf = new Vec4d(0.0d, 1.0d, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
public static final Vec4d deg3mixInf = new Vec4d(0.0d, 0.0d, 1.0d, Double.NEGATIVE_INFINITY);

public static final Vec4d deg2allNaN = new Vec4d(0.0d, 1.0d, Double.NaN, Double.NaN);
public static final Vec4d deg3allNaN = new Vec4d(0.0d, 0.0d, 1.0d, Double.NaN);

public static final Vec4d deg2mixInfNaN = new Vec4d(0.0d, 1.0d, Double.NaN, Double.POSITIVE_INFINITY);
public static final Vec4d deg3mixInfNaN = new Vec4d(0.0d, 0.0d, 1.0d, Double.POSITIVE_INFINITY);

//Tests for the mult cases
public static final Vec4d mult2 = new Vec4d(3.0d, 11.0d, 0.0d, 0.0d);
public static final Vec4d mult2tpose = new Vec4d(6.0d, 8.0d, 0.0d, 0.0d);
public static final Vec4d mult3 = new Vec4d(6.0d, 18.0d, 30.0d, 0.0d);
public static final Vec4d mult3tpose = new Vec4d(15.0d, 18.0d, 21.0d, 0.0d);
public static final Vec4d mult4 = new Vec4d(10.0d, 26.0d, 42.0d, 58.0d);
public static final Vec4d mult4tpose = new Vec4d(28.0d, 32.0d, 36.0d, 40.0d);

public static void assertEqual(Vec2d v, Vec4d v4) {
	assertTrue(v.x == v4.x);
	assertTrue(v.y == v4.y);
}

public static void assertEqual(Vec3d v, Vec4d v4) {
	assertTrue(v.x == v4.x);
	assertTrue(v.y == v4.y);
	assertTrue(v.z == v4.z);
}

public static void assertEqual(Vec4d v, Vec4d v4) {
	assertTrue(v.x == v4.x);
	assertTrue(v.y == v4.y);
	assertTrue(v.z == v4.z);
	assertTrue(v.w == v4.w);
}

public static void assertEqualNaN(Vec2d v, Vec4d v4) {
	assertTrue(v.x == v4.x || Double.isNaN(v.x) == Double.isNaN(v4.x));
	assertTrue(v.y == v4.y || Double.isNaN(v.y) == Double.isNaN(v4.y));
}

public static void assertEqualNaN(Vec3d v, Vec4d v4) {
	assertTrue(v.x == v4.x || Double.isNaN(v.x) == Double.isNaN(v4.x));
	assertTrue(v.y == v4.y || Double.isNaN(v.y) == Double.isNaN(v4.y));
	assertTrue(v.z == v4.z || Double.isNaN(v.z) == Double.isNaN(v4.z));
}

public static void assertEqualNaN(Vec4d v, Vec4d v4) {
	assertTrue(v.x == v4.x || Double.isNaN(v.x) == Double.isNaN(v4.x));
	assertTrue(v.y == v4.y || Double.isNaN(v.y) == Double.isNaN(v4.y));
	assertTrue(v.z == v4.z || Double.isNaN(v.z) == Double.isNaN(v4.z));
	assertTrue(v.w == v4.w || Double.isNaN(v.w) == Double.isNaN(v4.w));
}

private static final double EPS = 1e-12d;
public static void assertNear(double d1, double d2) {
	assertTrue(Math.abs(d1 - d2) < EPS);
}

public static void assertNear(Vec4d v, Vec4d v4) {
	assertNear(v.x, v4.x);
	assertNear(v.y, v4.y);
	assertNear(v.z, v4.z);
	assertNear(v.w, v4.w);
}

public void printVec(Vec4d vec) {
	System.out.format("%.20f %.20f %.20f %.20f%n", vec.x, vec.y, vec.z, vec.w);
}

@Test
public void testVecConstructors() {
	// Vec2d constructors
	Vec2d vec2 = new Vec2d();
	assertEqual(vec2, zero);

	vec2 = new Vec2d(seq);
	assertEqual(vec2, seq);

	vec2 = new Vec2d(1.0d, 2.0d);
	assertEqual(vec2, seq);

	// Vec3d constructors
	Vec3d vec3 = new Vec3d();
	assertEqual(vec3, zero);

	vec3 = new Vec3d(seq);
	assertEqual(vec3, seq);

	vec3 = new Vec3d(1.0d, 2.0d, 3.0d);
	assertEqual(vec3, seq);

	// Vec4d constructors
	Vec4d vec4 = new Vec4d();
	assertEqual(vec4, zero);

	vec4 = new Vec4d(seq);
	assertEqual(vec4, seq);

	vec4 = new Vec4d(1.0d, 2.0d, 3.0d, 4.0d);
	assertEqual(vec4, seq);
}

@Test
public void testVec2Set() {
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
public void testVec3Set() {
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
public void testVec4Set() {
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
public void testVec2Add() {
	Vec2d vec;

	// Test add into vec
	vec = new Vec2d(seq);
	vec.add2(seq);
	assertEqual(vec, seqadd2);

	// Test set vec to sum
	vec = new Vec2d(seq);
	vec.add2(seq, seq);
	assertEqual(vec, seqadd2);

	// Test construct summed vec
	vec = Vec2d.getAdd2(seq, seq);
	assertEqual(vec, seqadd2);
}

@Test
public void testVec3Add() {
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

	// Test construct summed vec
	vec = Vec3d.getAdd3(seq, seq);
	assertEqual(vec, seqadd3);
}

@Test
public void testVec4Add() {
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

	// Test construct summed vec
	vec = Vec4d.getAdd4(seq, seq);
	assertEqual(vec, seqadd4);
}

@Test
public void testVec2Sub() {
	Vec2d vec;

	// Test sub into vec
	vec = new Vec2d(seq);
	vec.sub2(one);
	assertEqual(vec, seqsub2);

	// Test set vec to sub
	vec = new Vec2d(seq);
	vec.sub2(seq, one);
	assertEqual(vec, seqsub2);

	// Test construct subtracted vec
	vec = Vec2d.getSub2(seq, one);
	assertEqual(vec, seqsub2);
}

@Test
public void testVec3Sub() {
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

	// Test construct subtracted vec
	vec = Vec3d.getSub3(seq, one);
	assertEqual(vec, seqsub3);
}

@Test
public void testVec4Sub() {
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

	// Test construct subtracted vec
	vec = Vec4d.getSub4(seq, one);
	assertEqual(vec, seqsub4);
}

@Test
public void testVec2Mul() {
	Vec2d vec;

	// Test mul into vec
	vec = new Vec2d(seq);
	vec.mul2(seq);
	assertEqual(vec, seqmul2);

	// Test set vec to mul
	vec = new Vec2d(seq);
	vec.mul2(seq, seq);
	assertEqual(vec, seqmul2);

	// Test construct multiplied vec
	vec = Vec2d.getMul2(seq, seq);
	assertEqual(vec, seqmul2);
}

@Test
public void testVec3Mul() {
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

	// Test construct multiplied vec
	vec = Vec3d.getMul3(seq, seq);
	assertEqual(vec, seqmul3);
}

@Test
public void testVec4Mul() {
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

	// Test construct multiplied vec
	vec = Vec4d.getMul4(seq, seq);
	assertEqual(vec, seqmul4);
}

@Test
public void testVec2Min() {
	Vec2d vec;

	// Test mul into vec
	vec = new Vec2d(seq);
	vec.min2(nseq);
	assertEqual(vec, seqmin2);

	// Test set vec to mul
	vec = new Vec2d(seq);
	vec.min2(seq, nseq);
	assertEqual(vec, seqmin2);

	// Test construct multiplied vec
	vec = Vec2d.getMin2(seq, nseq);
	assertEqual(vec, seqmin2);
}

@Test
public void testVec3Min() {
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

	// Test construct multiplied vec
	vec = Vec3d.getMin3(seq, nseq);
	assertEqual(vec, seqmin3);
}

@Test
public void testVec4Min() {
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

	// Test construct multiplied vec
	vec = Vec4d.getMin4(seq, nseq);
	assertEqual(vec, seqmin4);
}

@Test
public void testVec2Max() {
	Vec2d vec;

	// Test mul into vec
	vec = new Vec2d(nseq);
	vec.max2(seq);
	assertEqual(vec, seqmax2);

	// Test set vec to mul
	vec = new Vec2d(nseq);
	vec.max2(seq, nseq);
	assertEqual(vec, seqmax2);

	// Test construct multiplied vec
	vec = Vec2d.getMax2(seq, nseq);
	assertEqual(vec, seqmax2);
}

@Test
public void testVec3Max() {
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

	// Test construct multiplied vec
	vec = Vec3d.getMax3(seq, nseq);
	assertEqual(vec, seqmax3);
}

@Test
public void testVec4Max() {
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

	// Test construct multiplied vec
	vec = Vec4d.getMax4(seq, nseq);
	assertEqual(vec, seqmax4);
}

@Test
public void testVec2Dot() {
	Vec2d vec = new Vec2d(3.0d, 4.0d);

	assertTrue(vec.dot2(one) == 7.0d);
	assertTrue(vec.dot2(seq) == 11.0d);

	assertTrue(vec.mag2() == 5.0d);
	assertTrue(vec.magSquare2() == 25.0d);
}

@Test
public void testVec3Dot() {
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
public void testVec4Dot() {
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
public void testVec2Norm() {
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
public void testVec3Norm() {
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
public void testVec4Norm() {
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
public void testVec2NormDegen() {
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

@Test
public void testVec3NormDegen() {
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

@Test
public void testVec4NormDegen() {
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
	vec.normalize2(zero);
	assertEqual(vec, yaxis);

	vec = new Vec4d();
	vec.normalize2(posInf);
	assertEqual(vec, yaxis);

	vec = new Vec4d();
	vec.normalize2(negInf);
	assertEqual(vec, yaxis);

	vec = new Vec4d();
	vec.normalize2(mixInf);
	assertEqual(vec, yaxis);

	vec = new Vec4d();
	vec.normalize2(mixInfNaN);
	assertEqual(vec, yaxis);

	vec = new Vec4d();
	vec.normalize2(oneNaN);
	assertEqual(vec, yaxis);

	vec = new Vec4d();
	vec.normalize3(zero);
	assertEqual(vec, zaxis);

	vec = new Vec4d();
	vec.normalize3(posInf);
	assertEqual(vec, zaxis);

	vec = new Vec4d();
	vec.normalize3(negInf);
	assertEqual(vec, zaxis);

	vec = new Vec4d();
	vec.normalize3(mixInf);
	assertEqual(vec, zaxis);

	vec = new Vec4d();
	vec.normalize3(mixInfNaN);
	assertEqual(vec, zaxis);

	vec = new Vec4d();
	vec.normalize3(oneNaN);
	assertEqual(vec, zaxis);

	vec = new Vec4d();
	vec.normalize4(zero);
	assertEqual(vec, waxis);

	vec = new Vec4d();
	vec.normalize4(posInf);
	assertEqual(vec, waxis);

	vec = new Vec4d();
	vec.normalize4(negInf);
	assertEqual(vec, waxis);

	vec = new Vec4d();
	vec.normalize4(mixInf);
	assertEqual(vec, waxis);

	vec = new Vec4d();
	vec.normalize4(mixInfNaN);
	assertEqual(vec, waxis);

	vec = new Vec4d();
	vec.normalize4(oneNaN);
	assertEqual(vec, waxis);
}

@Test
public void testVec2Scale() {
	Vec2d vec;

	vec = new Vec2d(seq);
	vec.scale2(2.0d);
	assertEqual(vec, scaleseq2);

	vec = new Vec2d(seq);
	vec.scale2(2.0d, seq);
	assertEqual(vec, scaleseq2);

	vec = Vec2d.getScale2(2.0d, seq);
	assertEqual(vec, scaleseq2);
}

@Test
public void testVec3Scale() {
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

	vec = Vec3d.getScale3(2.0d, seq);
	assertEqual(vec, scaleseq3);
}

@Test
public void testVec4Scale() {
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

	vec = Vec4d.getScale4(2.0d, seq);
	assertEqual(vec, scaleseq4);
}

@Test
public void testVec3Cross3() {
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

	vec = Vec3d.getCross3(xaxis, yaxis);
	assertEqual(vec, zaxis);
}

@Test
public void testVec4Cross3() {
	Vec4d vec;

	vec = new Vec4d(xaxis);
	vec.cross3(yaxis);
	assertEqual(vec, zaxis);

	vec = new Vec4d(yaxis);
	vec.cross3(xaxis);
	assertEqual(vec, negzaxis);

	vec = new Vec4d();
	vec.cross3(xaxis, yaxis);
	assertEqual(vec, zaxis);

	vec = new Vec4d();
	vec.cross3(yaxis, xaxis);
	assertEqual(vec, negzaxis);
}

@Test
public void testVec2Mult() {
	Vec2d vec = new Vec2d();

	vec.mult2(TestMat.seqmult, one);
	assertEqual(vec, mult2);

	vec.mult2(one, TestMat.seqmult);
	assertEqual(vec, mult2tpose);
}

@Test
public void testVec3Mult() {
	Vec3d vec = new Vec3d();

	vec.mult2(TestMat.seqmult, one);
	assertEqual(vec, mult2);

	vec.mult2(one, TestMat.seqmult);
	assertEqual(vec, mult2tpose);

	vec.mult3(TestMat.seqmult, one);
	assertEqual(vec, mult3);

	vec.mult3(one, TestMat.seqmult);
	assertEqual(vec, mult3tpose);
}

@Test
public void testVec4Mult() {
	Vec4d vec = new Vec4d();

	vec.mult2(TestMat.seqmult, one);
	assertEqual(vec, mult2);

	vec.mult2(one, TestMat.seqmult);
	assertEqual(vec, mult2tpose);

	vec.mult3(TestMat.seqmult, one);
	assertEqual(vec, mult3);

	vec.mult3(one, TestMat.seqmult);
	assertEqual(vec, mult3tpose);

	vec.mult4(TestMat.seqmult, one);
	assertEqual(vec, mult4);

	vec.mult4(one, TestMat.seqmult);
	assertEqual(vec, mult4tpose);
}
}
