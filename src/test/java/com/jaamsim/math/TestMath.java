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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.jaamsim.math.Constants;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Matrix4d;
import com.jaamsim.math.Vector4d;

public class TestMath {


private Vector4d va;
private Vector4d vb;
private Matrix4d ma;
private Matrix4d mb;

@Before
public void setup()
{
	va = new Vector4d(1, 2, 3, 4);
	vb = new Vector4d(2, 4, 6, 8);
	double[] matVals = new double[] {  1,  2,  3,  4,
	                                   5,  6,  7,  8,
	                                   9, 10, 11, 12,
	                                  13, 14, 15, 16};
	ma = new Matrix4d(matVals);

	double[] mbVals = new double[] { 1, 0, 0, 0,
	                                 0, 2, 0, 0,
	                                 0, 0, 3, 0,
	                                 0, 0, 0, 4};

	mb = new Matrix4d(mbVals);

}

@Test
public void testVectAdd() {

	Vector4d expected4 = new Vector4d(3, 6, 9, 12);
	Vector4d expected3 = new Vector4d(3, 6, 9, 1);

	Vector4d res4 = new Vector4d();
	Vector4d res3 = new Vector4d();

	va.add4(vb, res4);
	va.add3(vb, res3);

	assertTrue(res4.equals(expected4));
	assertTrue(res3.equals(expected3));
}

@Test
public void testVectSub() {
	Vector4d expected4 = new Vector4d(-1, -2, -3, -4);
	Vector4d expected3 = new Vector4d(-1, -2, -3, 1);

	Vector4d res4 = new Vector4d();
	Vector4d res3 = new Vector4d();

	va.sub4(vb, res4);
	va.sub3(vb, res3);

	assertTrue(res4.equals(expected4));
	assertTrue(res3.equals(expected3));

}

@Test
public void addInPlace() {
	Vector4d res = new Vector4d(1, 1, 1, 1);
	Vector4d expected = new Vector4d(2, 3, 4, 5);
	res.addLocal4(va);

	assertTrue(res.equals(expected));

	res.set(1, 1, 1, 1);
	expected.set(-1, -3, -5, -7);
	res.subLocal4(vb);
	assertTrue(res.equals(expected));

}

@Test
public void testMagnitude() {
	Vector4d v = new Vector4d(3, 4, 0);
	double mag3 = v.mag3();
	double mag4 = v.mag4();
	assertEquals(mag3, 5, Constants.EPSILON);

	assertEquals(mag4, Math.sqrt(26), Constants.EPSILON);

}

@Test
public void testVectClone() {
	Vector4d cl = new Vector4d(va);
	Vector4d expected = new Vector4d(1, 2, 3, 4);
	assertTrue(cl.equals(expected));
}

@Test
public void testMatIdent() {
	Matrix4d ident = new Matrix4d();
	Matrix4d res = new Matrix4d();

	ident.mult(ma, res);
	assertTrue(ma.equals(res));

	ma.mult(ident, res);
	assertTrue(ma.equals(res));

}

public void testMatTranspose() {
	Matrix4d res = new Matrix4d();
	double[] d = new double[] { 1, 5,  9, 13,
	                            2, 6, 10, 14,
	                            3, 7, 11, 15,
	                            4, 8, 12, 16};
	Matrix4d expected = new Matrix4d(d);

	ma.transpose(res);
	assertTrue(res.equals(expected));
}

@Test
public void testSimpleMult() {

	double[] expectedLVals = new double[] { 1,  5,  9, 13, // Careful, this format is transposed
	                                        4, 12, 20, 28, // as Matrixes are column major
	                                        9, 21, 33, 45,
	                                       16, 32, 48, 64};
	Matrix4d expectedL = new Matrix4d(expectedLVals);
	expectedL.transposeLocal();

	Matrix4d res = new Matrix4d();
	mb.mult(ma, res);
	assertTrue(res.equals(expectedL));

	double[] expectedRVals = new double[] { 1, 10, 27, 52, // Careful, this format is transposed
	                                        2, 12, 30, 56, // as Matrixes are column major
	                                        3, 14, 33, 60,
	                                        4, 16, 36, 64};
	Matrix4d expectedR = new Matrix4d(expectedRVals);
	expectedR.transposeLocal();

	ma.mult(mb, res);
	assertTrue(res.equals(expectedR));

}

@Test
public void testMatrixScale() {
	double[] expectedVals = new double[] {  3,  6,  9, 12, // Careful, this format is transposed
	                                       15, 18, 21, 24, // as Matrixes are column major
	                                       27, 30, 33, 36,
	                                       39, 42, 45, 48};
	Matrix4d expected = new Matrix4d(expectedVals);
	Matrix4d res = new Matrix4d();

	ma.scale(3.0, res);
	assertTrue(res.equals(expected));

}

@Test
public void testMatrixScratchPad() {
	Vector4d v = new Vector4d(1, 2, 3, 4);
	Vector4d expected = new Vector4d(1, 4, 9, 16);

	// Test self assignment;
	mb.mult(v,  v);
	assertTrue(v.equals(expected));

	Matrix4d ident = new Matrix4d();

	Matrix4d exp = new Matrix4d(ma);
	ident.mult(ma, ma);

	assertTrue(exp.equals(ma));
}

@Test
public void testDeterminant() {
	for (int i = 0; i < 10; ++i) {
		// Create 10 rotation matrices around each axis and check the determinat is 1 for all of them
		Matrix4d xRot = Matrix4d.RotationMatrix(i * .63453, Vector4d.X_AXIS);
		double xDet = xRot.determinant();
		assertTrue(MathUtils.near(xDet, 1));

		// Create 10 rotation matrices around each axis and check the determinat is 1 for all of them
		Matrix4d yRot = Matrix4d.RotationMatrix(i * .63453, Vector4d.Y_AXIS);
		double yDet = yRot.determinant();
		assertTrue(MathUtils.near(yDet, 1));

		// Create 10 rotation matrices around each axis and check the determinat is 1 for all of them
		Matrix4d zRot = Matrix4d.RotationMatrix(i * .63453, Vector4d.Z_AXIS);
		double zDet = zRot.determinant();
		assertTrue(MathUtils.near(zDet, 1));

	}

	for (int i = 0; i < 10; ++i) {
		Matrix4d scale = Matrix4d.ScaleMatrix(new Vector4d(i, i*2, i*3));
		double det = scale.determinant();
		assertTrue(MathUtils.near(det, i*i*i*6));

	}
}

@Test
public void testInverse() {

	for (int i = 1; i < 10; ++i) {
		// Create 10 rotation matrices around each axis and check the determinat is 1 for all of them
		Matrix4d xRot = Matrix4d.RotationMatrix(i * .63453, Vector4d.X_AXIS);
		Matrix4d yRot = Matrix4d.RotationMatrix(i * .63453, Vector4d.Y_AXIS);
		Matrix4d scale = Matrix4d.ScaleMatrix(new Vector4d(i, i*2, i*3));
		Matrix4d trans = Matrix4d.TranslationMatrix(new Vector4d(i, i*2, i*3));

		Matrix4d testMat = new Matrix4d();
		testMat.mult(xRot, testMat);
		testMat.mult(yRot, testMat);
		testMat.mult(scale, testMat);
		testMat.mult(trans, testMat);

		Matrix4d inv = testMat.inverse();

		Matrix4d ident = new Matrix4d();
		inv.mult(testMat, ident);
		assertTrue(ident.equals(Matrix4d.IDENT));

		testMat.mult(inv, ident);
		assertTrue(ident.equals(Matrix4d.IDENT));
	}
}


} //class
