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

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class Util {

	// Note, this is purely an exchange class, large arrays should not be stored of these
	// ints are used to avoid signing problems with bytes
	public static class YUVColor {
		public int y, u, v;
	}

	public static class RGBColor {
		public int r, g, b;

		public RGBColor() {}

		public RGBColor(int rgb) {
			r = (rgb >> 16) & 0xff;
			g = (rgb >>  8) & 0xff;
			b = (rgb      ) & 0xff;
		}
	}

	public static int unsign(byte b) {
		return (b&0xff);
	}

	public static int getUByte(ByteBuffer b) {
		return unsign(b.get());
	}

	public static int getUByte(ByteBuffer b, int ind) {
		return unsign(b.get(ind));
	}

	public static int getUByte(byte[] b, int ind) {
		return unsign(b[ind]);
	}

	public static int clamp255(int i) {
		if (i < 0) return 0;
		if (i > 255) return 255;
		return i;
	}
	public static int clamp255d(double d) {
		return clamp255((int)d);
	}

	public static int avg3(int a, int b, int c) {
		return (a + b + b + c +2) >> 2;
	}
	public static int avg2(int a, int b) {
		return (a + b + 1) >> 1;
	}

	public static int yuvToRGBInt(YUVColor yuv) {
		return yuvToRGBInt(yuv.y, yuv.u, yuv.v);
	}

	public static int yuvToRGBInt(int y, int u, int v) {
		int ret = 0;

		int r = clamp255d(1.1644*(y-16)                        + (1.5960 * (v - 128)));
		int g = clamp255d(1.1644*(y-16) - (0.3918 * (u - 128)) - (0.8130 * (v - 128)));
		int b = clamp255d(1.1644*(y-16) + (2.0172 * (u - 128)));

		ret += r << 16;
		ret += g << 8;
		ret += b;

		return ret;
	}

	public static YUVColor rgbToYUV(RGBColor rgb) {
		return rgbToYUV(rgb.r, rgb.g, rgb.b);
	}

	public static int rgbToY(int rgb) {
		int r = (rgb >> 16) & 0xff;
		int g = (rgb >>  8) & 0xff;
		int b = (rgb      ) & 0xff;

		return (int)( 16 + 0.2568*r + 0.5052*g + 0.0979*b);
	}

	public static int rgbToU(int rgb) {
		int r = (rgb >> 16) & 0xff;
		int g = (rgb >>  8) & 0xff;
		int b = (rgb      ) & 0xff;

		return (int)(128 - 0.1482*r - 0.2910*g + 0.4392*b);
	}

	public static int rgbToV(int rgb) {
		int r = (rgb >> 16) & 0xff;
		int g = (rgb >>  8) & 0xff;
		int b = (rgb      ) & 0xff;

		return (int)(128 + 0.4392*r - 0.3678*g - 0.0714*b);
	}


	public static YUVColor rgbToYUV(int r, int g, int b) {
		r = clamp255(r);
		g = clamp255(g);
		b = clamp255(b);

		YUVColor ret = new YUVColor();
		ret.y = (int)( 16 + 0.2568*r + 0.5052*g + 0.0979*b);
		ret.u = (int)(128 - 0.1482*r - 0.2910*g + 0.4392*b);
		ret.v = (int)(128 + 0.4392*r - 0.3678*g - 0.0714*b);

		return ret;
	}

	public static void addResidueToPlane(int x, int y, short[] res, int stride, byte[] plane) {
		for (int j = 0; j < 4; ++j) {
			for (int i = 0; i < 4; ++i) {
				int ind = j*4+i;
				int val = Util.getUByte(plane, (y+j)*stride+x+i);

				val += res[ind];
				plane[(y+j)*stride+x+i] = (byte)Util.clamp255(val);
			}
		}
	}

	/**
	 * Get the rgb average of 4 pixels in a BufferedImage, handles edge cases
	 * @param img
	 * @param x
	 * @param y
	 * @return
	 */
	public static RGBColor avgPixel2(BufferedImage img, int x, int y) {
		assert(x < img.getWidth());
		assert(y < img.getHeight());

		int numPixels = 1;
		int r = 0, g = 0, b = 0;
		RGBColor rgb;
		rgb = new RGBColor(img.getRGB(x, y));
		r += rgb.r; g += rgb.g; b += rgb.b;

		if (x+1 < img.getWidth()) {
			rgb = new RGBColor(img.getRGB(x+1, y));
			r += rgb.r; g += rgb.g; b += rgb.b;
			numPixels++;
		}
		if (y+1 < img.getHeight()) {
			rgb = new RGBColor(img.getRGB(x, y+1));
			r += rgb.r; g += rgb.g; b += rgb.b;
			numPixels++;
		}

		if ((x+1 < img.getWidth()) &&(y+1 < img.getHeight())) {
			rgb = new RGBColor(img.getRGB(x+1, y+1));
			r += rgb.r; g += rgb.g; b += rgb.b;
			numPixels++;
		}

		rgb = new RGBColor();
		rgb.r = r / numPixels;
		rgb.g = g / numPixels;
		rgb.b = b / numPixels;

		return rgb;
	}
}
