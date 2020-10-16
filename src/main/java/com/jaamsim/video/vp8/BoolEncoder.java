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

import java.nio.ByteBuffer;

public final class BoolEncoder {

	//private final ArrayList<Byte> data;
	private final byte[] data;
	int pos = 0;

	private int range;
	private int bottom;
	private int count;

	private boolean canEncode = true;

	BoolEncoder() {
		range = 255;
		bottom = 0;
		data = new byte[1 << 25]; // 32 MiB buffer for now
		count = 0;
	}

	public final void encodeBoolean(boolean b, int prob) {
		//assert(prob >= 1 && prob <= 255);
		assert(canEncode);

		int split = 1 + (((range-1) * prob) >> 8);

		if (b) {
			range -= split;
			bottom += split;
		} else {
			range = split;
		}

		while (range < 128) {
			range <<= 1;
			bottom <<= 1;

			if (bottom >= 65536) {
				addOneToOutput();
				bottom -= 65536;
			}

			if (++count == 8) {
				// Write out a byte
				data[pos++] = (byte)((bottom & 0xFF00) >> 8);
				count = 0;
				bottom = bottom & 0xFF;
			}
		}
		//EncLogger.log(String.format("%d %d %d", b ? 1 : 0, prob, range));
	}

	private final void addOneToOutput() {
		// The value overflowed and we need to explicitly add 1 to the stored value
		for (int i = pos-1; i >= 0; i--) {
			int val = Util.unsign(data[i]);
			if (val != 0xff) {
				data[i] = (byte)(val + 1);
				break;
			}
			data[i] = 0;
		}
	}

	public final void encodeFlag(boolean b) {
		encodeBoolean(b, 128);
	}

	public void encodeLitUInt(int v, int numBits) {
		assert(v >= 0);

		int mask = 1 << (numBits - 1);
		for (int i = 0; i < numBits; ++i) {
			encodeFlag((v & mask) != 0);
			mask = mask >> 1;
		}
	}

	public final void encodeLitInt(int v, int numBits) {
		boolean neg = v < 0;
		if (neg) { v = -v; }

		encodeLitUInt(v, numBits);

		encodeFlag(neg);
	}

	public final void encodeLitWithProbs(int v, int numBits, int[] probs) {
		assert(v >= 0);

		int mask = 1 << (numBits - 1);
		for (int i = 0; i < numBits; ++i) {
			encodeBoolean((v & mask) != 0, probs[i]);
			mask = mask >> 1;
		}
	}

	public final void encodeTree(int[] bits, int[] probs, int[] tree, int startBit, int startTree) {

		// This is a similar tree structure used by the decoder
		int pos = startTree;
		for (int i = startBit; i < bits.length; ++i) {
			boolean val = bits[i] == 1;
			int prob = probs[pos >> 1];
			pos = tree[pos+(val?1:0)];

			encodeBoolean(val, prob);
		}
	}

	/**
	 * Returns the data so far, note after this is called, the encoder can still be added to
	 */
	public ByteBuffer getData() {
		ByteBuffer ret = ByteBuffer.allocate(pos + 2);

		int bot = bottom << (8 - count);

		if (bot >= 65536) {
			addOneToOutput();
			bot -= 65536;
		}

		ret.put(data, 0, pos);

		ret.put((byte)((bot & 0xff00) >> 8));
		ret.put((byte)(bot & 0xff));

		ret.flip();

		canEncode = false;

		return ret;
	}
}
