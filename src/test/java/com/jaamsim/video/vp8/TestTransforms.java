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
package com.jaamsim.video.vp8;

import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

public class TestTransforms {

	@Test
	public void testWHT() {
		for (int i = 0; i < 2000; ++i) {
			testSingleWHT();
		}
	}

	private void testSingleWHT() {
		short [] vals = new short[16];
		Random rand = new Random();

		for (int i = 0; i < 16; ++i) {
			vals[i] = (short)(rand.nextInt(2000) - 1000);
			//vals[i] = (short)10;
		}

		short[] temp = new short[16];
		short[] out = new short[16];
		short[] inter = new short[16];

		Transform.WHT(vals, inter, temp);

		Transform.deWHT(inter, out, temp);

		for (int i = 0; i < 16; ++i) {

			assertTrue(Math.abs(out[i] - vals[i]) <= 2);
		}

	}

	@Test
	public void testDCT() {
		for (int i = 0; i < 2000; ++i) {
			testSingleDCT();
		}
	}

	private void testSingleDCT() {
		short [] vals = new short[16];
		Random rand = new Random();

		for (int i = 0; i < 16; ++i) {
			vals[i] = (short)(rand.nextInt(2000) - 1000);
			//vals[i] = (short)10;
		}

		short[] temp = new short[16];
		short[] out = new short[16];
		short[] inter = new short[16];

		Transform.DCT(vals, inter, temp);

		Transform.deDCT(inter, out, temp);

		for (int i = 0; i < 16; ++i) {
			assertTrue(Math.abs(out[i] - vals[i]) <= 2);
		}

		short dc = Transform.DCTVal0(vals);
		assertTrue(Math.abs(dc - inter[0]) <= 2);

	}

}
