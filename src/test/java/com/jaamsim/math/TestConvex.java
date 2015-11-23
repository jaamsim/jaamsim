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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

public class TestConvex {

	@Test
	public void TestConvexCube() {
		// Create a list of points with 3 nested cubes

		ArrayList<Vec3d> totalPoints = new ArrayList<>();
		//totalPoints.addAll(getPointsForCube(1));
		//totalPoints.addAll(getPointsForCube(2));
		totalPoints.addAll(getPointsForCube(3));

		ConvexHull hull = ConvexHull.TryBuildHull(totalPoints, 1, 0, null);

		assertTrue(hull.getVertices().size() == 8);

		assertTrue(hull.getFaces().size() == 12);

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
		ArrayList<Vec3d> totalPoints = new ArrayList<>();
		totalPoints.addAll(getPointsForCube(1));
		totalPoints.addAll(getPointsForCube(2));
		totalPoints.addAll(getPointsForCube(3));

		ConvexHull hull = ConvexHull.TryBuildHull(totalPoints, 1, 0, null);

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
		ArrayList<Vec4d> ret = new ArrayList<>();

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
