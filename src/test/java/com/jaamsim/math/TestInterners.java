/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
