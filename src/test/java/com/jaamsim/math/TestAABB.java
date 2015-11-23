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

import org.junit.Test;

public class TestAABB {

	@Test
	public void  BasicCollision() {
		AABB aabb = new AABB(new Vec4d(1, 2, 3, 1.0d),
		                     new Vec4d(-1, -2, -3, 1.0d));

		assertTrue(aabb.collides(new Vec4d(0.0d, 0.0d, 0.0d, 1.0d)));

		assertTrue(aabb.collides(new Vec4d(0.5, 0.5, 0.5, 1.0d)));

		assertTrue(aabb.collides(new Vec4d(0.5, -0.5, 0.5, 1.0d)));
		assertTrue(aabb.collides(new Vec4d(0.5, 0.5, -0.5, 1.0d)));
		assertTrue(aabb.collides(new Vec4d(0.5, 0.5, 2.5, 1.0d)));

		assertTrue(!aabb.collides(new Vec4d(0.5, 0.5, 3.5, 1.0d)));


	}

	@Test
	public void  AABBCollision() {
		AABB aabb1 = new AABB(new Vec4d(1, 2, 3, 1.0d),
		                      new Vec4d(-1, -2, -3, 1.0d));

		AABB aabb2 = new AABB(new Vec4d(3, 3, 3, 1.0d),
		                      new Vec4d(2, 2, 2, 1.0d));

		AABB aabb3 = new AABB(new Vec4d(2.5, 3, 3, 1.0d),
		                      new Vec4d(2, 2, 2, 1.0d));

		assertTrue(!aabb1.collides(aabb2));

		assertTrue(aabb2.collides(aabb3));
	}

	@Test
	public void  AABBRayCollision() {
		AABB aabb = new AABB(new Vec4d(1, 2, 3, 1.0d),
		                      new Vec4d(-1, -2, -3, 1.0d));

		Ray r1 = new Ray(new Vec4d(-5, 0, 0, 1.0d),
		                new Vec4d(1, 0, 0, 1.0d));

		assertTrue(aabb.collisionDist(r1) >= 0);
		assertTrue(MathUtils.near(aabb.collisionDist(r1), 4));

		Ray r2 = new Ray(new Vec4d(3, 3, 3, 1.0d),
                new Vec4d(1, 1, 1, 1.0d));

		// Should not hit
		assertTrue(aabb.collisionDist(r2) < 0);

		Ray r3 = new Ray(new Vec4d(3, 3, 3, 1.0d),
                new Vec4d(-1, -1, -1, 1.0d));
		assertTrue(aabb.collisionDist(r3) >= 0);

	}


}
