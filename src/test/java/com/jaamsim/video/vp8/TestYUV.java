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

import com.jaamsim.video.vp8.Util.YUVColor;

public class TestYUV {

	private static final int NUM_COLORS = 10000;

	@Test
	public void testYUV() {

		Random rand = new Random();

		// Create a random RGB, transform to YUV and back, check consistency
		for (int i = 0; i < NUM_COLORS; ++i) {

			int r = rand.nextInt(256);
			int g = rand.nextInt(256);
			int b = rand.nextInt(256);

			YUVColor yuv = Util.rgbToYUV(r, g, b);
			int rgbRebuilt = Util.yuvToRGBInt(yuv);
			int rRebuilt = (rgbRebuilt >> 16) & 0xFF;
			int gRebuilt = (rgbRebuilt >> 8) & 0xFF;
			int bRebuilt = (rgbRebuilt >> 0) & 0xFF;

			// 2% accuracy is probably fine for now as there is some potential for clamping...
			assertTrue(Math.abs(r - rRebuilt) <= 4);
			assertTrue(Math.abs(g - gRebuilt) <= 4);
			assertTrue(Math.abs(b - bRebuilt) <= 4);
		}
	}
}
