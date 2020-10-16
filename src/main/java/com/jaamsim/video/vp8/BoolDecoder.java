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

//import org.chudo.vp8.Verifier;

public class BoolDecoder {

	private final ByteBuffer input;

	private int range;
	private int value;
	private int bitCount;

	public BoolDecoder(ByteBuffer in) {
		input = in;

		assert(input.limit() >= 2);

		value = (Util.getUByte(input) << 8) + Util.getUByte(input);
		range = 255;
		bitCount = 0;
	}

	public int decodeBit(int prob) {
		int split = 1 + (((range-1) * prob) >> 8);

		int bigSplit = split << 8;

		int retVal = 0;
		if (value >= bigSplit) {
			retVal = 1;
			range -= split;
			value -= bigSplit;
		} else {
			range = split;
		}

		while (range < 128) {
			value = value << 1;
			range = range << 1;

			// We have shifted out a whole byte, replace it
			if (++bitCount == 8) {
				bitCount = 0;

				assert((value & 0xFF) == 0);
				value += Util.getUByte(input);
			}
		}

		//Verifier.demand(String.format("%d %d %d", retVal, prob, range));

		return retVal;
	}

	public int getLitBit() {
		return decodeBit(128);
	}

	public boolean getFlag() {
		return (decodeBit(128) == 1);
	}

	public int getLitUInt(int bits) {
		int ret = 0;
		for (int i = bits-1; i >= 0; --i) {
			ret |= decodeBit(128) << i;
		}
		return ret;
	}

	public int getLitInt(int bits) {
		int val = getLitUInt(bits);
		return getFlag() ? -val : val;
	}

	// optionally read an int if the first bit is 1, otherwise return 0
	public int getOptInt(int bits) {
		return getFlag() ? getLitInt(bits) : 0;
	}

	/**
	 * Return a tree coded value, use the same tree coding scheme as in the VP8 RFC
	 * @param treeCode
	 */
	public int getTreeVal(int[] treeCode, int[] probs) {
		return getTreeVal(treeCode, probs, 0);
	}

	public int getTreeVal(int[] treeCode, int[] probs, int startPos) {
		int pos = startPos;

		do {
			int prob = probs[pos >> 1];
			pos = treeCode[pos + decodeBit(prob)];
		} while (pos > 0);
		return -pos;
	}

	public int getIntWithProbs(int[] probs, int numBits) {
		int ret = 0;
		for (int i = 0; i < numBits; ++i) {
			ret += ret + decodeBit(probs[i]);
		}
		return ret;
	}

}
