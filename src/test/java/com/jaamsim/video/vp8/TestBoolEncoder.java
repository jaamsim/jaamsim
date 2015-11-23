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

import java.nio.ByteBuffer;
import java.util.Random;

import org.junit.Test;

public class TestBoolEncoder {

	private final int NUM_BITS = 10000000;
	private final int NUM_INTS = 100000;
	@Test
	public void testEncoder() {

		Random rand = new Random();
		int[] probs = new int[NUM_BITS];
		boolean[] bits = new boolean[NUM_BITS];
		for (int i = 0; i < NUM_BITS; ++i) {
			probs[i] = rand.nextInt(256);
			if (probs[i] == 0) { probs[i] = 0; }
			bits[i] = rand.nextInt(256) >= probs[i];
		}

		BoolEncoder enc = new BoolEncoder();
		for (int i = 0; i < NUM_BITS; ++i) {
			enc.encodeBoolean(bits[i], probs[i]);
		}

		ByteBuffer stream = enc.getData();

		BoolDecoder dec = new BoolDecoder(stream);

		for (int i = 0; i < NUM_BITS; ++i) {
			boolean val = dec.decodeBit(probs[i]) == 1;

			assertTrue(val == bits[i]);
		}
	}

	@Test
	public void testLitUInt() {

		Random rand = new Random();
		int[] vals = new int[NUM_INTS];
		int[] sizes = new int[NUM_INTS];
		for (int i = 0; i < NUM_INTS; ++i) {
			sizes[i] = rand.nextInt(24) + 2;
			vals[i] = rand.nextInt(1 << (sizes[i] - 1));
		}

		BoolEncoder enc = new BoolEncoder();
		for (int i = 0; i < NUM_INTS; ++i) {
			enc.encodeLitUInt(vals[i], sizes[i]);
		}

		ByteBuffer stream = enc.getData();

		BoolDecoder dec = new BoolDecoder(stream);

		for (int i = 0; i < NUM_INTS; ++i) {
			int decVal = dec.getLitUInt(sizes[i]);

			assertTrue(decVal == vals[i]);
		}
	}

	@Test
	public void testLitInt() {

		Random rand = new Random();
		int[] vals = new int[NUM_INTS];
		int[] sizes = new int[NUM_INTS];
		for (int i = 0; i < NUM_INTS; ++i) {
			sizes[i] = rand.nextInt(24) + 2;
			vals[i] = rand.nextInt(1 << (sizes[i] - 1)) - (1 << (sizes[i] - 2));
		}

		BoolEncoder enc = new BoolEncoder();
		for (int i = 0; i < NUM_INTS; ++i) {
			enc.encodeLitInt(vals[i], sizes[i]);
		}

		ByteBuffer stream = enc.getData();

		BoolDecoder dec = new BoolDecoder(stream);

		for (int i = 0; i < NUM_INTS; ++i) {
			int decVal = dec.getLitInt(sizes[i]);

			assertTrue(decVal == vals[i]);
		}
	}

}
