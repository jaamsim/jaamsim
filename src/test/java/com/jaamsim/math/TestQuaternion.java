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

import org.junit.Before;
import org.junit.Test;

import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Vector4d;

public class TestQuaternion {

private Quaternion x_pi;
private Quaternion x_halfPi;
private Quaternion y_pi;
private Quaternion y_halfPi;
private Quaternion z_pi;
private Quaternion z_halfPi;
private Quaternion ident;

@Before
public void setup() {
	x_pi = Quaternion.Rotation(Math.PI, Vector4d.X_AXIS);
	x_halfPi = Quaternion.Rotation(Math.PI / 2.0, Vector4d.X_AXIS);

	y_pi = Quaternion.Rotation(Math.PI, Vector4d.Y_AXIS);
	y_halfPi = Quaternion.Rotation(Math.PI / 2.0, Vector4d.Y_AXIS);

	z_pi = Quaternion.Rotation(Math.PI, Vector4d.Z_AXIS);
	z_halfPi = Quaternion.Rotation(Math.PI / 2.0, Vector4d.Z_AXIS);

	ident = new Quaternion();
}


@Test
public void testSimpleQuatRotation() {

	Vector4d res = new Vector4d();
	z_halfPi.rotateVector(Vector4d.X_AXIS, res);
	assertTrue(res.equals(Vector4d.Y_AXIS));

	z_pi.rotateVector(Vector4d.X_AXIS, res);
	assertTrue(res.equals(Vector4d.NEG_X_AXIS));

	y_halfPi.rotateVector(Vector4d.X_AXIS, res);
	assertTrue(res.equals(Vector4d.NEG_Z_AXIS));

	y_pi.rotateVector(Vector4d.X_AXIS, res);
	assertTrue(res.equals(Vector4d.NEG_X_AXIS));

	x_halfPi.rotateVector(Vector4d.Y_AXIS, res);
	assertTrue(res.equals(Vector4d.Z_AXIS));
	x_pi.rotateVector(Vector4d.Y_AXIS, res);
	assertTrue(res.equals(Vector4d.NEG_Y_AXIS));

}

@Test
public void testSlerp() {

	Quaternion x_quarterPi = new Quaternion();
	ident.slerp(x_halfPi, 0.5, x_quarterPi);
	Quaternion x_eighthPi = new Quaternion();
	ident.slerp(x_halfPi, 0.25, x_eighthPi);

	Vector4d res = new Vector4d();
	Vector4d expected = new Vector4d(0, 1, 1, 1);
	expected.normalizeLocal3();

	x_quarterPi.rotateVector(Vector4d.Y_AXIS, res);
	assertTrue(res.equals3(expected));

	x_eighthPi.rotateVector(Vector4d.Y_AXIS, res);
	expected.set(0,  Math.cos(Math.PI/8), Math.sin(Math.PI/8));
	assertTrue(res.equals3(expected));
}

@Test
public void testConstructor() {

	Quaternion x_eighthPi = new Quaternion();
	ident.slerp(x_halfPi, 0.25, x_eighthPi);

	Quaternion x_eighthPi2 = Quaternion.Rotation(Math.PI/8, Vector4d.X_AXIS);

	assertTrue(x_eighthPi.equals(x_eighthPi2));

	Quaternion x_negEighthPi = new Quaternion();
	x_negEighthPi.conjugate(x_eighthPi);

	Quaternion x_negEighthPi2 = Quaternion.Rotation(-Math.PI/8, Vector4d.X_AXIS);
	assertTrue(x_negEighthPi.equals(x_negEighthPi2));
}

@Test
public void testSelfAssignment() {
	Quaternion q1 = new Quaternion(x_halfPi);
	Quaternion q2 = new Quaternion(z_pi);

	Quaternion q3 = new Quaternion();
	q1.mult(q2, q3);
	q1.mult(q2, q1);

	assertTrue(q1.equals(q3));

	q1.mult(q2, q3);
	q1.mult(q2, q2);

	assertTrue(q2.equals(q3));
}

} // class
