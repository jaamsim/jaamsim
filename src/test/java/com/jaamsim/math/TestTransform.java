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

import com.jaamsim.math.Matrix4d;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;

public class TestTransform {

@Test
public void testIdentity() {
	Transform t = new Transform();
	Vector4d in = new Vector4d(1, 2, 3);
	Vector4d expected = new Vector4d(in);

	Vector4d res = new Vector4d();
	t.apply(in, res);
	assertTrue(res.equals(expected));
}

@Test
public void testTranslation() {
	Transform t = new Transform();
	Vector4d trans = new Vector4d(3, 4, 5);
	t.setTrans(trans);
	Vector4d in = new Vector4d(1, 2, 3);

	Vector4d expected = new Vector4d(4, 6, 8);

	Vector4d res = new Vector4d();
	t.apply(in, res);
	assertTrue(res.equals(expected));
}

@Test
public void testRotation() {
	Transform t = new Transform();
	Quaternion rot = Quaternion.Rotation(Math.PI/2, Vector4d.X_AXIS);
	t.setRot(rot);
	Vector4d in = new Vector4d(1, 2, 3);

	Vector4d expected = new Vector4d(1, -3, 2);

	Vector4d res = new Vector4d();
	t.apply(in, res);
	assertTrue(res.equals(expected));
}

@Test
public void testScale() {
	Transform t = new Transform();
	t.setScale(3);
	Vector4d in = new Vector4d(1, 2, 3);

	Vector4d expected = new Vector4d(3, 6, 9);

	Vector4d res = new Vector4d();
	t.apply(in, res);
	assertTrue(res.equals(expected));
}

@Test
public void testInverse()
{
	Vector4d trans = new Vector4d(1, 2, 3);
	Quaternion rot = Quaternion.Rotation(Math.PI/4, Vector4d.Z_AXIS);
	Transform t = new Transform(trans, rot, 3);

	Transform invT = new Transform();
	t.inverse(invT);

	Transform ident = new Transform();

	Transform test = new Transform();
	t.merge(invT, test);

	assertTrue(test.equals(ident));

	Matrix4d mat = new Matrix4d();
	Matrix4d matInv = new Matrix4d();
	t.getMatrix(mat);
	invT.getMatrix(matInv);
	Matrix4d identMat = new Matrix4d();

	Matrix4d resMat = new Matrix4d();

	mat.mult(matInv, resMat);
	assertTrue(resMat.equals(identMat));

	// Test inverse both ways
	matInv.mult(mat, resMat);
	assertTrue(resMat.equals(identMat));
}

/**
 * Make sure the transforms can write to themselves cleanly
 */
@Test
public void testSelfAssignment() {
	Vector4d trans1 = new Vector4d(1, 2, 3);
	Quaternion rot1 = Quaternion.Rotation(Math.PI/4, Vector4d.Z_AXIS);
	Transform t1 = new Transform(trans1, rot1, 3);

	Vector4d trans2 = new Vector4d(3, 2, 1);
	Quaternion rot2 = Quaternion.Rotation(Math.PI/3, Vector4d.X_AXIS);
	Transform t2 = new Transform(trans2, rot2, 6);

	Transform t3 = new Transform();
	t1.merge(t2, t3);
	t1.merge(t2, t1);
	assertTrue(t1.equals(t3));

	t1.merge(t2, t3);
	t1.merge(t2, t2);
	assertTrue(t2.equals(t3));

	Transform t4 = new Transform();
	t2.inverse(t4);
	t2.inverse(t2);
	assertTrue(t2.equals(t4));
}

}
