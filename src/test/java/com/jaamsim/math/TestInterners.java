/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import java.util.Random;

import org.junit.Test;

public class TestInterners {

	@Test
	public void testVec2dInterner() {
		Random rand = new Random();

		Vec2d[] vecs = new Vec2d[128];
		Vec2d[] interned = new Vec2d[128];
		for (int i = 0; i < 128; ++i) {
			vecs[i] = new Vec2d(rand.nextDouble(), rand.nextDouble());
		}

		Vec2dInterner interner = new Vec2dInterner();

		for (int i = 0; i < 128; ++i) {
			interned[i] = interner.intern(vecs[i]);
			assertTrue(interned[i].equals2(vecs[i]));
		}

		// check index
		for (int i = 0; i < 128; ++i) {
			int ind = interner.getIndexForValue(vecs[i]);
			assertTrue(i < 128);
			assertTrue(interner.getValueForIndex(ind).equals2(vecs[i]));
		}
	}

	@Test
	public void testVec3dInterner() {
		Random rand = new Random();

		Vec3d[] vecs = new Vec3d[128];
		Vec3d[] interned = new Vec3d[128];
		for (int i = 0; i < 128; ++i) {
			vecs[i] = new Vec3d(rand.nextDouble(), rand.nextDouble(), rand.nextDouble());
		}

		Vec3dInterner interner = new Vec3dInterner();

		for (int i = 0; i < 128; ++i) {
			interned[i] = interner.intern(vecs[i]);
			assertTrue(interned[i].equals2(vecs[i]));
		}

		// check index
		for (int i = 0; i < 128; ++i) {
			int ind = interner.getIndexForValue(vecs[i]);
			assertTrue(i < 128);
			assertTrue(interner.getValueForIndex(ind).equals2(vecs[i]));
		}
	}

	@Test
	public void testVec4dInterner() {
		Random rand = new Random();

		Vec4d[] vecs = new Vec4d[128];
		Vec4d[] interned = new Vec4d[128];
		for (int i = 0; i < 128; ++i) {
			vecs[i] = new Vec4d(rand.nextDouble(), rand.nextDouble(), rand.nextDouble(), rand.nextDouble());
		}

		Vec4dInterner interner = new Vec4dInterner();

		for (int i = 0; i < 128; ++i) {
			interned[i] = interner.intern(vecs[i]);
			assertTrue(interned[i].equals2(vecs[i]));
		}

		// check index
		for (int i = 0; i < 128; ++i) {
			int ind = interner.getIndexForValue(vecs[i]);
			assertTrue(i < 128);
			assertTrue(interner.getValueForIndex(ind).equals2(vecs[i]));
		}
	}

}
