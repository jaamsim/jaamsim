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

import org.junit.Assert;
import org.junit.Test;

public class TestMat4d {
private static final double EPS = 1e-12d;
//Test constructors and basic utility helpers
private static final double[] d_seq = new double[16];
private static final double[] d_nseq = new double[16];
private static final double[] d_seqtranspose = new double[16];
private static final double[] d_z = new double[16];
private static final double[] d_seqmult = new double[16];

public static final Mat4d ident;
public static final Mat4d ident3;
public static final Mat4d zero;
public static final Mat4d seq;

public static final Mat4d seqTranspose;

//Used to test matrix multiplication
public static final Mat4d seqOne;
public static final Mat4d seqmult3;
public static final Mat4d seqmult4;
public static final Mat4d seqmult3z;

static {
	for (int i = 0; i < 16; i++) {
		d_seq[i] = i;
		d_nseq[i] = -i;
		d_z[i] = 0.0d;
		d_seqmult[i] = i + 1;
	}

	ident = new Mat4d();

	ident3 = new Mat4d();
	ident3.d33 = 0.0d;

	zero = new Mat4d(d_z);

	for (int row = 0; row < 4; row++) {
		d_seqtranspose[row * 4 + 0] = 0 * 4 + row;
		d_seqtranspose[row * 4 + 1] = 1 * 4 + row;
		d_seqtranspose[row * 4 + 2] = 2 * 4 + row;
		d_seqtranspose[row * 4 + 3] = 3 * 4 + row;
	}

	seq = new Mat4d(d_seq);
	seqTranspose = new Mat4d(d_seqtranspose);

	seqOne = new Mat4d(d_seqmult);
	seqmult3 = new Mat4d( 20.0d,  23.0d,  26.0d,   3.0d,
	                      68.0d,  83.0d,  98.0d,   7.0d,
	                     116.0d, 143.0d, 170.0d,  11.0d,
	                      12.0d,  13.0d,  14.0d,  15.0d);

	seqmult3z = new Mat4d( 20.0d,  23.0d,  26.0d,   0.0d,
	                       68.0d,  83.0d,  98.0d,   0.0d,
	                      116.0d, 143.0d, 170.0d,   0.0d,
	                        0.0d,   0.0d,   0.0d,   1.0d);

	seqmult4 = new Mat4d( 56.0d,  62.0d,  68.0d,  74.0d,
	                     152.0d, 174.0d, 196.0d, 218.0d,
	                     248.0d, 286.0d, 324.0d, 362.0d,
	                     344.0d, 398.0d, 452.0d, 506.0d);
}

public static void assertEqual(Mat4d m1, Mat4d m2) {
	assertTrue(m1.d00 == m2.d00);
	assertTrue(m1.d01 == m2.d01);
	assertTrue(m1.d02 == m2.d02);
	assertTrue(m1.d03 == m2.d03);

	assertTrue(m1.d10 == m2.d10);
	assertTrue(m1.d11 == m2.d11);
	assertTrue(m1.d12 == m2.d12);
	assertTrue(m1.d13 == m2.d13);

	assertTrue(m1.d20 == m2.d20);
	assertTrue(m1.d21 == m2.d21);
	assertTrue(m1.d22 == m2.d22);
	assertTrue(m1.d23 == m2.d23);

	assertTrue(m1.d30 == m2.d30);
	assertTrue(m1.d31 == m2.d31);
	assertTrue(m1.d32 == m2.d32);
	assertTrue(m1.d33 == m2.d33);
}


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

public static void assertEqualArray(Mat4d m, double... val) {
	// Row 0
	assertTrue(m.d00 == val[ 0]);
	assertTrue(m.d01 == val[ 1]);
	assertTrue(m.d02 == val[ 2]);
	assertTrue(m.d03 == val[ 3]);

	// Row 1
	assertTrue(m.d10 == val[ 4]);
	assertTrue(m.d11 == val[ 5]);
	assertTrue(m.d12 == val[ 6]);
	assertTrue(m.d13 == val[ 7]);

	// Row 2
	assertTrue(m.d20 == val[ 8]);
	assertTrue(m.d21 == val[ 9]);
	assertTrue(m.d22 == val[10]);
	assertTrue(m.d23 == val[11]);

	// Row 3
	assertTrue(m.d30 == val[12]);
	assertTrue(m.d31 == val[13]);
	assertTrue(m.d32 == val[14]);
	assertTrue(m.d33 == val[15]);
}

public static void printMat(Mat4d mat) {
	System.out.format("%s %s %s %s%n", mat.d00, mat.d01, mat.d02, mat.d03);
	System.out.format("%s %s %s %s%n", mat.d10, mat.d11, mat.d12, mat.d13);
	System.out.format("%s %s %s %s%n", mat.d20, mat.d21, mat.d22, mat.d23);
	System.out.format("%s %s %s %s%n", mat.d30, mat.d31, mat.d32, mat.d33);
}

@Test
public void testConstructors() {
	Mat4d mat;

	mat = new Mat4d();
	assertEqualArray(mat, 1.0d, 0.0d, 0.0d, 0.0d,
	                      0.0d, 1.0d, 0.0d, 0.0d,
	                      0.0d, 0.0d, 1.0d, 0.0d,
	                      0.0d, 0.0d, 0.0d, 1.0d);

	mat = new Mat4d(d_seq);
	assertEqualArray(mat, d_seq);

	mat = new Mat4d(new Mat4d(d_seq));
	assertEqualArray(mat, d_seq);
}

@Test
public void testKnownMatrices() {
	Mat4d mat;

	mat = new Mat4d();
	mat.zero();
	assertEqual(mat, zero);

	mat.identity();
	assertEqual(mat, ident);
}

@Test
public void testSet() {
	Mat4d mat;

	mat = new Mat4d();
	mat.zero();
	mat.set4(seqOne);
	assertEqual(mat, seqOne);

	mat.set4(zero);
	assertEqual(mat, zero);
}

@Test
public void testTranspose() {
	Mat4d mat;

	mat = new Mat4d(seq);
	mat.transpose4();
	assertEqual(mat, seqTranspose);
	mat.transpose4();
	assertEqual(mat, seq);

	mat = new Mat4d();
	mat.transpose4(seq);
	assertEqual(mat, seqTranspose);
	mat.transpose4(seqTranspose);
	assertEqual(mat, seq);
}

@Test
public void testMult() {
	Mat4d mat;

	mat = new Mat4d(seq);
	mat.mult3(ident);
	assertEqual(mat, seq);

	mat.mult3(ident, seq);
	assertEqual(mat, seq);

	mat.mult3(seq, ident);
	assertEqual(mat, seq);

	mat = new Mat4d(seq);
	mat.mult3(seq);
	assertEqual(mat, seqmult3);

	mat = new Mat4d();
	mat.mult3(seq, seq);
	assertEqual(mat, seqmult3z);

	mat = new Mat4d(seq);
	mat.mult4(ident);
	assertEqual(mat, seq);

	mat.mult4(ident, seq);
	assertEqual(mat, seq);

	mat.mult4(seq, ident);
	assertEqual(mat, seq);

	mat = new Mat4d(seq);
	mat.mult4(seq);
	assertEqual(mat, seqmult4);

	mat = new Mat4d();
	mat.mult4(seq, seq);
	assertEqual(mat, seqmult4);
}

@Test
public void testEuler() {
	// Rotation of PI radians around the x-axis
	Mat4d xRot = new Mat4d( 1.0d,  0.0d,  0.0d, 0.0d,
	                        0.0d, -1.0d,  0.0d, 0.0d,
	                        0.0d,  0.0d, -1.0d, 0.0d,
	                        0.0d,  0.0d,  0.0d, 1.0d);

	Mat4d yRot = new Mat4d(-1.0d,  0.0d,  0.0d, 0.0d,
	                        0.0d,  1.0d,  0.0d, 0.0d,
	                        0.0d,  0.0d, -1.0d, 0.0d,
	                        0.0d,  0.0d,  0.0d, 1.0d);

	Mat4d zRot = new Mat4d(-1.0d,  0.0d,  0.0d, 0.0d,
	                        0.0d, -1.0d,  0.0d, 0.0d,
	                        0.0d,  0.0d,  1.0d, 0.0d,
	                        0.0d,  0.0d,  0.0d, 1.0d);

	Mat4d mat;

	mat = new Mat4d();
	mat.zero();
	mat.setEuler3(new Vec3d());
	assertEqual(mat, ident3);

	mat = new Mat4d();
	mat.setEuler3(new Vec3d(Math.PI, 0.0d, 0.0d));
	assertNear(mat, xRot);

	mat = new Mat4d();
	mat.setEuler3(new Vec3d(0.0d, Math.PI, 0.0d));
	assertNear(mat, yRot);

	mat = new Mat4d();
	mat.setEuler3(new Vec3d(0.0d, 0.0d, Math.PI));
	assertNear(mat, zRot);

	mat = new Mat4d();
	mat.zero();
	mat.setEuler4(new Vec3d());
	assertEqual(mat, ident);

	mat = new Mat4d();
	mat.setEuler4(new Vec3d(Math.PI, 0.0d, 0.0d));
	assertNear(mat, xRot);

	mat = new Mat4d();
	mat.setEuler4(new Vec3d(0.0d, Math.PI, 0.0d));
	assertNear(mat, yRot);

	mat = new Mat4d();
	mat.setEuler4(new Vec3d(0.0d, 0.0d, Math.PI));
	assertNear(mat, zRot);
}

@Test
public void testTranslate() {
	Mat4d trans = new Mat4d(1.0d, 0.0d, 0.0d, 4.0d,
	                        0.0d, 1.0d, 0.0d, 5.0d,
	                        0.0d, 0.0d, 1.0d, 6.0d,
	                        0.0d, 0.0d, 0.0d, 1.0d);
	Mat4d mat;

	mat = new Mat4d();
	mat.setTranslate3(new Vec3d(4.0d, 5.0d, 6.0d));
	assertEqual(mat, trans);
}

@Test
public void testScale() {
	Mat4d scale2 = new Mat4d( 2.0d,  4.0d,  3.0d,  4.0d,
	                         10.0d, 12.0d,  7.0d,  8.0d,
	                          9.0d, 10.0d, 11.0d, 12.0d,
	                         13.0d, 14.0d, 15.0d, 16.0d);

	Mat4d scale3 = new Mat4d( 2.0d,  4.0d,  6.0d,  4.0d,
	                         10.0d, 12.0d, 14.0d,  8.0d,
	                         18.0d, 20.0d, 22.0d, 12.0d,
	                         13.0d, 14.0d, 15.0d, 16.0d);

	Mat4d scale4 = new Mat4d( 2.0d,  4.0d,  6.0d,  8.0d,
	                         10.0d, 12.0d, 14.0d, 16.0d,
	                         18.0d, 20.0d, 22.0d, 24.0d,
	                         26.0d, 28.0d, 30.0d, 32.0d);

	Mat4d mat;

	mat = new Mat4d(seqOne);
	mat.scale2(2.0d);
	assertEqual(mat, scale2);

	mat = new Mat4d(seqOne);
	mat.scale3(2.0d);
	assertEqual(mat, scale3);

	mat = new Mat4d(seqOne);
	mat.scale4(2.0d);
	assertEqual(mat, scale4);
}

@Test
public void testScaleRows() {
	Mat4d rowScale2 = new Mat4d( 2.0d,  4.0d,  6.0d,  8.0d,
	                            10.0d, 12.0d, 14.0d, 16.0d,
	                             9.0d, 10.0d, 11.0d, 12.0d,
	                            13.0d, 14.0d, 15.0d, 16.0d);

	Mat4d rowScale3 = new Mat4d( 2.0d,  4.0d,  6.0d,  8.0d,
	                            10.0d, 12.0d, 14.0d, 16.0d,
	                            18.0d, 20.0d, 22.0d, 24.0d,
	                            13.0d, 14.0d, 15.0d, 16.0d);

	Mat4d rowScale4 = new Mat4d( 2.0d,  4.0d,  6.0d,  8.0d,
	                            10.0d, 12.0d, 14.0d, 16.0d,
	                            18.0d, 20.0d, 22.0d, 24.0d,
	                            26.0d, 28.0d, 30.0d, 32.0d);

	Vec4d two = new Vec4d(2.0d, 2.0d, 2.0d, 2.0d);

	Mat4d mat;

	mat = new Mat4d(seqOne);
	mat.scaleRows2(two);
	assertEqual(mat, rowScale2);

	mat = new Mat4d(seqOne);
	mat.scaleRows3(two);
	assertEqual(mat, rowScale3);

	mat = new Mat4d(seqOne);
	mat.scaleRows4(two);
	assertEqual(mat, rowScale4);
}

@Test
public void testScaleCols() {
	Mat4d colScale2 = new Mat4d( 2.0d,  4.0d,  3.0d,  4.0d,
	                            10.0d, 12.0d,  7.0d,  8.0d,
	                            18.0d, 20.0d, 11.0d, 12.0d,
	                            26.0d, 28.0d, 15.0d, 16.0d);

	Mat4d colScale3 = new Mat4d( 2.0d,  4.0d,  6.0d,  4.0d,
	                            10.0d, 12.0d, 14.0d,  8.0d,
	                            18.0d, 20.0d, 22.0d, 12.0d,
	                            26.0d, 28.0d, 30.0d, 16.0d);

	Mat4d colScale4 = new Mat4d( 2.0d,  4.0d,  6.0d,  8.0d,
	                            10.0d, 12.0d, 14.0d, 16.0d,
	                            18.0d, 20.0d, 22.0d, 24.0d,
	                            26.0d, 28.0d, 30.0d, 32.0d);

	Vec4d two = new Vec4d(2.0d, 2.0d, 2.0d, 2.0d);

	Mat4d mat;

	mat = new Mat4d(seqOne);
	mat.scaleCols2(two);
	assertEqual(mat, colScale2);

	mat = new Mat4d(seqOne);
	mat.scaleCols3(two);
	assertEqual(mat, colScale3);

	mat = new Mat4d(seqOne);
	mat.scaleCols4(two);
	assertEqual(mat, colScale4);
}

@Test
public void testDeterminant() {
	Mat4d mat;
	double det;
	for (int i = 0; i < 10; ++i) {
		mat = new Mat4d();
		mat.setEuler4(new Vec3d(i * 0.63453d, 0.0d, 0.0d));
		det = mat.determinant();
		Assert.assertEquals(1.0d, det, EPS);

		mat = new Mat4d();
		mat.setEuler4(new Vec3d(0.0d, i * 0.63453d, 0.0d));
		det = mat.determinant();
		Assert.assertEquals(1.0d, det, EPS);

		mat = new Mat4d();
		mat.setEuler4(new Vec3d(0.0d, 0.0d, i * 0.63453d));
		det = mat.determinant();
		Assert.assertEquals(1.0d, det, EPS);
	}

	mat = new Mat4d();
	mat.setEuler4(new Vec3d( 0.63453d,  0.63453d, 0.63453d));
	mat.setTranslate3(new Vec3d(1.0d, 2.0d, 3.0d));
	det = mat.determinant();
	Assert.assertEquals(1.0d, det, EPS);
}

@Test
public void testInverse() {
	Quaternion tmp = new Quaternion();
	for (int i = 1; i < 10; ++i) {
		// Create 10 random matrices to invert
		Mat4d xRot = new Mat4d();
		tmp.setRotXAxis(i * .63453);
		xRot.setRot3(tmp);

		Mat4d yRot = new Mat4d();
		tmp.setRotYAxis(i * .63453);
		yRot.setRot3(tmp);

		Mat4d scale = new Mat4d();
		scale.scaleCols4(new Vec4d(i, i*2, i*3, 1.0d));
		Mat4d trans = new Mat4d();
		trans.setTranslate3(new Vec3d(i, i*2, i*3));

		Mat4d testMat = new Mat4d();
		testMat.mult4(xRot);
		testMat.mult4(yRot);
		testMat.mult4(scale);
		testMat.mult4(trans);

		Mat4d inv = testMat.inverse();

		Mat4d ident = new Mat4d();

		Mat4d res = new Mat4d();
		res.mult4(testMat, inv);
		assertNear(res, ident);

		res.mult4(inv, testMat);
		assertNear(res, ident);
	}
}

}
