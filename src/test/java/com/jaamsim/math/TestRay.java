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
 */package com.jaamsim.math;

 import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import com.jaamsim.render.DebugLine;
import com.sandwell.JavaSimulation.ColourInput;

public class TestRay {
	private static final Vec4d ORIGIN = new Vec4d(0, 0, 0, 1.0d);
	private static final Vec4d X_AXIS = new Vec4d(1, 0, 0, 1.0d);
	private static final Vec4d Y_AXIS = new Vec4d(0, 1, 0, 1.0d);
	private static final Vec4d Z_AXIS = new Vec4d(0, 0, 1, 1.0d);

	@Test
	public void TestRayMatrix() {
		Ray r = new Ray(new Vec4d(0, 4, 3, 1.0d), X_AXIS);

		Mat4d rayMat = MathUtils.RaySpace(r);

		Vec4d test = new Vec4d(4, 0, 0, 1.0d);
		Vec4d testRaySpace = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		testRaySpace.mult4(rayMat, test);

		assertTrue(MathUtils.near(testRaySpace.z, 4));

		double distToRay = Math.sqrt(testRaySpace.x*testRaySpace.x + testRaySpace.y*testRaySpace.y);
		assertTrue(MathUtils.near(distToRay, 5));
	}

	@Test
	public void TestLineCollision() {
		ArrayList<Vec4d> lineVerts = new ArrayList<Vec4d>(4);
		lineVerts.add(new Vec4d(1, 1, 1, 1.0d));
		lineVerts.add(new Vec4d(-1, -1, 1, 1.0d));

		lineVerts.add(new Vec4d(1, 2, 0, 1.0d));
		lineVerts.add(new Vec4d(8, 2, 0, 1.0d));

		DebugLine dl = new DebugLine(lineVerts, ColourInput.BLACK, ColourInput.BLACK, 1, null,1);

		Ray r0 = new Ray(ORIGIN, Z_AXIS);

		double r0Dist = dl.getCollisionDist(r0, true);
		assertTrue(MathUtils.near(r0Dist, 1)); // Should collide at (0, 0, 1)

		Ray r1 = new Ray(new Vec4d(6, -3, 0, 1.0d), Y_AXIS);

		double r1Dist = dl.getCollisionDist(r1, true);
		assertTrue(MathUtils.near(r1Dist, 5)); // Should collide at (0, 0, 1)

	}
}
