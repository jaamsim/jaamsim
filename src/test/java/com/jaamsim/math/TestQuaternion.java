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

public class TestQuaternion {

private static final Vec4d X_AXIS = new Vec4d(1, 0, 0, 1.0d);
private static final Vec4d Y_AXIS = new Vec4d(0, 1, 0, 1.0d);
private static final Vec4d Z_AXIS = new Vec4d(0, 0, 1, 1.0d);
private static final Vec4d NEG_X_AXIS = new Vec4d(-1,  0,  0, 1.0d);
private static final Vec4d NEG_Y_AXIS = new Vec4d( 0, -1,  0, 1.0d);
private static final Vec4d NEG_Z_AXIS = new Vec4d( 0,  0, -1, 1.0d);

private Quaternion x_pi;
private Quaternion x_halfPi;
private Quaternion y_pi;
private Quaternion y_halfPi;
private Quaternion z_pi;
private Quaternion z_halfPi;
private Quaternion ident;

@Before
public void setup() {
	x_pi = new Quaternion();
	x_pi.setRotXAxis(Math.PI);

	x_halfPi = new Quaternion();
	x_halfPi.setRotXAxis(Math.PI / 2.0d);

	y_pi = new Quaternion();
	y_pi.setRotYAxis(Math.PI);

	y_halfPi = new Quaternion();
	y_halfPi.setRotYAxis(Math.PI / 2.0d);

	z_pi = new Quaternion();
	z_pi.setRotZAxis(Math.PI);

	z_halfPi = new Quaternion();
	z_halfPi.setRotZAxis(Math.PI / 2.0d);

	ident = new Quaternion();
}


@Test
public void testSimpleQuatRotation() {
	Mat4d tempRot = new Mat4d();
	Vec4d res = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);

	tempRot.setRot3(z_halfPi);
	res.mult3(tempRot, X_AXIS);
	assertTrue(res.near4(Y_AXIS));

	tempRot.setRot3(z_pi);
	res.mult3(tempRot, X_AXIS);
	assertTrue(res.near4(NEG_X_AXIS));

	tempRot.setRot3(y_halfPi);
	res.mult3(tempRot, X_AXIS);
	assertTrue(res.near4(NEG_Z_AXIS));

	tempRot.setRot3(y_pi);
	res.mult3(tempRot, X_AXIS);
	assertTrue(res.near4(NEG_X_AXIS));

	tempRot.setRot3(x_halfPi);
	res.mult3(tempRot, Y_AXIS);
	assertTrue(res.near4(Z_AXIS));

	tempRot.setRot3(x_pi);
	res.mult3(tempRot, Y_AXIS);
	assertTrue(res.near4(NEG_Y_AXIS));
}

@Test
public void testSlerp() {
	Mat4d tempRot = new Mat4d();

	Quaternion x_quarterPi = new Quaternion();
	ident.slerp(x_halfPi, 0.5, x_quarterPi);
	Quaternion x_eighthPi = new Quaternion();
	ident.slerp(x_halfPi, 0.25, x_eighthPi);

	Vec4d res = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
	Vec4d expected = new Vec4d(0, 1, 1, 1);
	expected.normalize3();

	tempRot.setRot3(x_quarterPi);
	res.mult3(tempRot, Y_AXIS);
	assertTrue(res.near3(expected));

	tempRot.setRot3(x_eighthPi);
	res.mult3(tempRot, Y_AXIS);
	expected.set3(0,  Math.cos(Math.PI/8), Math.sin(Math.PI/8));
	assertTrue(res.near3(expected));
}

@Test
public void testConstructor() {

	Quaternion x_eighthPi = new Quaternion();
	ident.slerp(x_halfPi, 0.25, x_eighthPi);

	Quaternion x_eighthPi2 = new Quaternion();
	x_eighthPi2.setRotXAxis(Math.PI/8);

	assertTrue(x_eighthPi.near(x_eighthPi2));

	Quaternion x_negEighthPi = new Quaternion();
	x_negEighthPi.conjugate(x_eighthPi);

	Quaternion x_negEighthPi2 = new Quaternion();
	x_negEighthPi2.setRotXAxis(-Math.PI/8);

	assertTrue(x_negEighthPi.near(x_negEighthPi2));
}

@Test
public void testSelfAssignment() {
	Quaternion q1 = new Quaternion(x_halfPi);
	Quaternion q2 = new Quaternion(z_pi);

	Quaternion q3 = new Quaternion();
	q3.mult(q1, q2);
	q1.mult(q1, q2);

	assertTrue(q1.equals(q3));

	q3.mult(q1, q2);
	q2.mult(q1, q2);

	assertTrue(q2.equals(q3));
}

} // class
