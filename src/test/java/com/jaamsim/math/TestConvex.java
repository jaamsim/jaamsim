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

import java.util.ArrayList;

import org.junit.Test;

public class TestConvex {

	@Test
	public void TestConvexCube() {
		// Create a list of points with 3 nested cubes

		ArrayList<Vec4d> totalPoints = new ArrayList<Vec4d>();
		//totalPoints.addAll(getPointsForCube(1));
		//totalPoints.addAll(getPointsForCube(2));
		totalPoints.addAll(getPointsForCube(3));

		ConvexHull hull = ConvexHull.TryBuildHull(totalPoints, 1);

		assertTrue(hull.getVertices().size() == 8);

		assertTrue(hull.getFaces().size() == 12);

		assertTrue(MathUtils.near(hull.getRadius(), Math.sqrt(27)));

		assertTrue(hull.collides(new Vec4d(0, 0, 0, 1.0d), Transform.ident));
		assertTrue(hull.collides(new Vec4d(1, 1, 1, 1.0d), Transform.ident));

		assertTrue(hull.collides(new Vec4d(2, 2, 2, 1.0d), Transform.ident));

		assertTrue(!hull.collides(new Vec4d(4, 2, 2, 1.0d), Transform.ident));
		assertTrue(!hull.collides(new Vec4d(-4, 2, -2, 1.0d), Transform.ident));


		Transform trans = new Transform(new Vec4d(5, 6, 7, 1.0d));
		assertTrue(hull.collides(new Vec4d(5, 6, 7, 1.0d), trans));
	}


	@Test
	public void TestConvexCubeToRay() {
		ArrayList<Vec4d> totalPoints = new ArrayList<Vec4d>();
		totalPoints.addAll(getPointsForCube(1));
		totalPoints.addAll(getPointsForCube(2));
		totalPoints.addAll(getPointsForCube(3));

		ConvexHull hull = ConvexHull.TryBuildHull(totalPoints, 1);

		Ray r = new Ray(new Vec4d(5, 0, 0, 1.0d), new Vec4d(-1, 0, 0, 1.0d));
		double colDist = hull.collisionDistance(r, Transform.ident);
		assertTrue(colDist >= 0.0);
		assertTrue(MathUtils.near(colDist, 2.0));

		AABB aabb = hull.getAABB(new Mat4d());

		double aabbDist = aabb.collisionDist(r);
		assertTrue(aabbDist >= 0.0);
		assertTrue(MathUtils.near(aabbDist, 2.0));
	}

	private ArrayList<Vec4d> getPointsForCube(double r) {
		ArrayList<Vec4d> ret = new ArrayList<Vec4d>();

		ret.add(new Vec4d( r,  r,  r, 1.0d));
		ret.add(new Vec4d(-r,  r,  r, 1.0d));
		ret.add(new Vec4d( r, -r,  r, 1.0d));
		ret.add(new Vec4d(-r, -r,  r, 1.0d));

		ret.add(new Vec4d( r,  r, -r, 1.0d));
		ret.add(new Vec4d(-r,  r, -r, 1.0d));
		ret.add(new Vec4d( r, -r, -r, 1.0d));
		ret.add(new Vec4d(-r, -r, -r, 1.0d));

		return ret;
	}
}
