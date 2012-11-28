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

import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Matrix4d;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Vector4d;
import com.jaamsim.render.DebugLine;
import com.sandwell.JavaSimulation.ColourInput;

public class TestRay {

	@Test
	public void TestRayMatrix() {
		Ray r = new Ray(new Vector4d(0, 4, 3), Vector4d.X_AXIS);

		Matrix4d rayMat = Matrix4d.RaySpace(r);

		Vector4d test = new Vector4d(4, 0, 0);
		Vector4d testRaySpace = new Vector4d();
		rayMat.mult(test, testRaySpace);

		assertTrue(MathUtils.near(testRaySpace.z(), 4));

		double distToRay = Math.sqrt(testRaySpace.x()*testRaySpace.x() + testRaySpace.y()*testRaySpace.y());
		assertTrue(MathUtils.near(distToRay, 5));
	}

	@Test
	public void TestLineCollision() {
		ArrayList<Vector4d> lineVerts = new ArrayList<Vector4d>(4);
		lineVerts.add(new Vector4d(1, 1, 1));
		lineVerts.add(new Vector4d(-1, -1, 1));

		lineVerts.add(new Vector4d(1, 2, 0));
		lineVerts.add(new Vector4d(8, 2, 0));

		DebugLine dl = new DebugLine(lineVerts, ColourInput.BLACK, ColourInput.BLACK, 1, 1);

		Ray r0 = new Ray(Vector4d.ORIGIN, Vector4d.Z_AXIS);

		double r0Dist = dl.getCollisionDist(r0);
		assertTrue(MathUtils.near(r0Dist, 1)); // Should collide at (0, 0, 1)

		Ray r1 = new Ray(new Vector4d(6, -3, 0), Vector4d.Y_AXIS);

		double r1Dist = dl.getCollisionDist(r1);
		assertTrue(MathUtils.near(r1Dist, 5)); // Should collide at (0, 0, 1)

	}
}
