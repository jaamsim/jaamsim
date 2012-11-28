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

import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.math.Vector4d;


public class TestMat {

// Test constructors and basic utility helpers
public static double[] d_seq = new double[16];
public static double[] d_nseq = new double[16];
public static double[] d_seqtranspose = new double[16];
public static double[] d_z = new double[16];
public static double[] d_seqmult = new double[16];

public static final Mat4d ident = new Mat4d();
public static final Mat4d zero;
public static final Mat4d seq;
public static final Mat4d seqmult;

// Used to test matrix multiplication
public static final Mat4d seqmul3;
public static final Mat4d seqmul4;

//Used to test the matrix rotation
private static Quaternion z_halfPi = Quaternion.Rotation(Math.PI / 2.0, Vector4d.Z_AXIS);

// Used to test translation setters
public static Mat4d zeroTrans;

static {
	for (int i = 0; i < 16; i++) {
		d_seq[i] = i;
		d_nseq[i] = -i;
		d_z[i] = 0.0d;
		d_seqmult[i] = i + 1;
	}


	zero = new Mat4d(d_z);

	for (int row = 0; row < 4; row++) {
		d_seqtranspose[row * 4 + 0] = 0 * 4 + row;
		d_seqtranspose[row * 4 + 1] = 1 * 4 + row;
		d_seqtranspose[row * 4 + 2] = 2 * 4 + row;
		d_seqtranspose[row * 4 + 3] = 3 * 4 + row;
	}

	seq = new Mat4d(d_seq);
	seqmult = new Mat4d(d_seqmult);

	seqmul3 = new Mat4d( 20.0d,  23.0d,  26.0d,   3.0d,
	                     68.0d,  83.0d,  98.0d,   7.0d,
	                    116.0d, 143.0d, 170.0d,  11.0d,
	                     12.0d,  13.0d,  14.0d,  15.0d);

	seqmul4 = new Mat4d( 56.0d,  62.0d,  68.0d,  74.0d,
	                    152.0d, 174.0d, 196.0d, 218.0d,
	                    248.0d, 286.0d, 324.0d, 362.0d,
	                    344.0d, 398.0d, 452.0d, 506.0d);

	zeroTrans = new Mat4d(0.0d, 0.0d, 0.0d, 7.0d,
	                      0.0d, 0.0d, 0.0d, 8.0d,
	                      0.0d, 0.0d, 0.0d, 9.0d,
	                      0.0d, 0.0d, 0.0d, 0.0d);

}

public static void printMat(Mat4d mat) {
	System.out.format("%f %f %f %f%n", mat.d00, mat.d01, mat.d02, mat.d03);
	System.out.format("%f %f %f %f%n", mat.d10, mat.d11, mat.d12, mat.d13);
	System.out.format("%f %f %f %f%n", mat.d20, mat.d21, mat.d22, mat.d23);
	System.out.format("%f %f %f %f%n", mat.d30, mat.d31, mat.d32, mat.d33);
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

@Test
public void testMatConstructors() {
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
public void testMatUtil() {
	Mat4d mat;

	mat = new Mat4d();
	mat.zero();
	assertEqual(mat, zero);

	mat.identity();
	assertEqual(mat, ident);

	mat = new Mat4d(d_seq);
	mat.transpose4();
	assertEqualArray(mat, d_seqtranspose);
	mat.transpose4(mat);
	assertEqualArray(mat, d_seq);

	mat = new Mat4d();
	mat.transpose4(seq);
	assertEqualArray(mat, d_seqtranspose);
}

@Test
public void testMul3() {
	Mat4d mat;

	mat = new Mat4d(seq);
	mat.mul3(ident);
	assertEqual(mat, seq);

	mat.mul3(ident, seq);
	assertEqual(mat, seq);
}

@Test
public void testMul4() {
	Mat4d mat;

	mat = new Mat4d(seq);
	mat.mul3(seq);
	assertEqual(mat, seqmul3);

	mat = new Mat4d(seq);
	mat.mul4(ident);
	assertEqual(mat, seq);

	mat = new Mat4d(seq);
	mat.mul4(seq, seq);
	assertEqual(mat, seqmul4);

	mat = new Mat4d();
	mat.mul4(ident, seq);
	assertEqual(mat, seq);
}

@Test
public void testRotQuat() {
	Mat4d mat = new Mat4d();
	mat.setRot4(z_halfPi);

	Vec4d vec = new Vec4d();
	vec.mult4(mat, TestVec.xaxis);
	TestVec.assertNear(vec, TestVec.yaxis);
}

@Test
public void testSetTrans() {
	Mat4d mat;

	mat = new Mat4d(zero);
	mat.setTrans3(new Vec3d(7.0d, 8.0d, 9.0d));
	assertEqual(mat, zeroTrans);
}
}
