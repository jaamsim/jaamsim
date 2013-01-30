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
