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
