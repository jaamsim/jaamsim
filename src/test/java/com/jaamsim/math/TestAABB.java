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

public class TestAABB {

	@Test
	public void  BasicCollision() {
		AABB aabb = new AABB(new Vector4d(1, 2, 3),
		                     new Vector4d(-1, -2, -3));

		assertTrue(aabb.collides(new Vector4d()));

		assertTrue(aabb.collides(new Vector4d(0.5, 0.5, 0.5)));

		assertTrue(aabb.collides(new Vector4d(0.5, -0.5, 0.5)));
		assertTrue(aabb.collides(new Vector4d(0.5, 0.5, -0.5)));
		assertTrue(aabb.collides(new Vector4d(0.5, 0.5, 2.5)));

		assertTrue(!aabb.collides(new Vector4d(0.5, 0.5, 3.5)));


	}

	@Test
	public void  AABBCollision() {
		AABB aabb1 = new AABB(new Vector4d(1, 2, 3),
		                      new Vector4d(-1, -2, -3));

		AABB aabb2 = new AABB(new Vector4d(3, 3, 3),
		                      new Vector4d(2, 2, 2));

		AABB aabb3 = new AABB(new Vector4d(2.5, 3, 3),
		                      new Vector4d(2, 2, 2));

		assertTrue(!aabb1.collides(aabb2));

		assertTrue(aabb2.collides(aabb3));
	}

	@Test
	public void  AABBRayCollision() {
		AABB aabb = new AABB(new Vector4d(1, 2, 3),
		                      new Vector4d(-1, -2, -3));

		Ray r1 = new Ray(new Vector4d(-5, 0, 0),
		                new Vector4d(1, 0, 0));

		assertTrue(aabb.collisionDist(r1) >= 0);
		assertTrue(MathUtils.near(aabb.collisionDist(r1), 4));

		Ray r2 = new Ray(new Vector4d(3, 3, 3),
                new Vector4d(1, 1, 1));

		// Should not hit
		assertTrue(aabb.collisionDist(r2) < 0);

		Ray r3 = new Ray(new Vector4d(3, 3, 3),
                new Vector4d(-1, -1, -1));
		assertTrue(aabb.collisionDist(r3) >= 0);

	}


}
