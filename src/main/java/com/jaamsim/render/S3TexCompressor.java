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
package com.jaamsim.render;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class S3TexCompressor {

	// These are useful scratch arrays
	int[] rs = new int[4];
	int[] gs = new int[4];
	int[] bs = new int[4];

	int[] pixels = new int[16];

	public ByteBuffer compress(IntBuffer inBuffer, int width, int height) {

		int blocksWide = ((width + 3) >> 2);
		int blocksHigh = ((height+3) >> 2);
		int numBlocks = blocksWide * blocksHigh;

		ByteBuffer ret = ByteBuffer.allocate(numBlocks * 8);

		for (int by = 0; by < blocksHigh; ++by) {
			for (int bx = 0; bx < blocksWide; ++bx) {
				// Build up a block
				int x = bx*4;
				int y = by*4;

				// Handle being near the right or top edge
				int maxPX = 3;
				int maxPY = 3;
				if (by == blocksHigh - 1 && (height & 3) != 0) {
					maxPY = (height&3)-1;
				}
				if (bx == blocksWide - 1 && (width & 3) != 0) {
					maxPX = (width&3)-1;
				}
				for (int py = 0; py < 4; ++py) {

					int rpy = py;
					if (py > maxPY) { rpy = maxPY; }

					for (int px = 0; px < 4; ++px) {
						int rpx = px;
						if (px > maxPX) { rpx = maxPX; }
						pixels[rpy*4+rpx] = inBuffer.get((y+rpy)*width+x+rpx);
					}
				}

				compressBlock(pixels, ret);
			}
		}

		assert(ret.position() == ret.capacity());
		ret.flip();
		return ret;
	}

	private void compressBlock(int[] pixels, ByteBuffer out) {
		assert(pixels.length == 16);

		// Find the extreme colours
		int minMag = 765;
		int maxMag = 0;

		int minR = 0, minG = 0, minB = 0;
		int maxR = 0, maxG = 0, maxB = 0;

		for (int i = 0; i < 16; ++i) {
			int pix = pixels[i];
			int r =         pix & 0xff;
			int g = (pix >>  8) & 0xff;
			int b = (pix >> 16) & 0xff;

			int mag = r + g + b;
			if (mag < minMag) {
				minMag = mag;
				minR = r; minG = g; minB = b;
			}
			if (mag > maxMag) {
				maxMag = mag;
				maxR = r; maxG = g; maxB = b;
			}
		}

		int c0 = 0;
		c0 += (maxB >> 3) << 11;
		c0 += (maxG >> 2) << 5;
		c0 += (maxR >> 3);

		int c1 = 0;
		c1 += (minB >> 3) << 11;
		c1 += (minG >> 2) << 5;
		c1 += (minR >> 3);

		if (c1 > c0) {
			// Swap the order
			int temp = minR;
			minR = maxR;
			maxR = temp;

			temp = minG;
			minG = maxG;
			maxG = temp;

			temp = minB;
			minB = maxB;
			maxB = temp;

			temp = c0;
			c0 = c1;
			c1 = temp;
		}

		out.put((byte)(c0 & 0xff)); out.put((byte)((c0>>8) & 0xff));
		out.put((byte)(c1 & 0xff)); out.put((byte)((c1>>8) & 0xff));

		rs[0] = maxR; gs[0] = maxG; bs[0] = maxB;
		rs[1] = minR; gs[1] = minG; bs[1] = minB;

		if (c0 == c1) {
			rs[2] = (maxR+minR)/2; gs[2] = (maxG+minG)/2; bs[2] = (maxB+minB)/2;
			rs[3] = 0; gs[3] = 0; bs[3] = 0;
		} else {
			rs[2] = (2*maxR+minR)/3; gs[2] = (2*maxG+minG)/3; bs[2] = (2*maxB+minB)/3;
			rs[3] = (maxR+2*minR)/3; gs[3] = (maxG+2*minG)/3; bs[3] = (maxB+2*minB)/3;
		}

		int numVals = 0;
		int outByte = 0;
		int mult = 1;

		for (int i = 0; i < 16; ++i) {

			int pix = pixels[i];
			int r =         pix & 0xff;
			int g = (pix >>  8) & 0xff;
			int b = (pix >> 16) & 0xff;

			int bestDiff = 756;
			int bestInd = 0;
			for (int j = 0; j < 4; ++j) {
				int diff = Math.abs(r-rs[j]) + Math.abs(g-gs[j]) + Math.abs(b-bs[j]);
				if (diff < bestDiff) {
					bestInd = j;
					bestDiff = diff;
				}
			}

			outByte += bestInd * mult;
			mult = mult << 2;

			if (++numVals == 4) {
				mult = 1;
				numVals = 0;
				out.put((byte)outByte);
				outByte = 0;
			}
		}
	}
}
