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

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class YUVImage {

	final byte[] yPlane;
	final byte[] uPlane;
	final byte[] vPlane;

	final int width;
	final int height;


	public static YUVImage fromFile(String filename) {
		FileInputStream in;

		try {
			in = new FileInputStream(filename);
			int w = in.read() + (in.read() << 8) + (in.read() << 16) + (in.read() << 24);
			int h = in.read() + (in.read() << 8) + (in.read() << 16) + (in.read() << 24);

			YUVImage ret = new YUVImage(w, h);

			int read = in.read(ret.yPlane);
			if (read != w*h) {
				System.out.printf("Only read %d bytes for y\n", read);
				in.close();
				return null;
			}
			read = in.read(ret.uPlane);
			if (read != w*h/4) {
				System.out.printf("Only read %d bytes for u\n", read);
				in.close();
				return null;
			}
			read = in.read(ret.vPlane);
			if (read != w*h/4) {
				System.out.printf("Only read %d bytes for v\n", read);
				in.close();
				return null;
			}

			in.close();

			return ret;

		} catch (FileNotFoundException ex) {
			System.out.println("Could not open file");
			System.out.println(ex.getMessage());
			return null;
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
			return null;
		}
	}


	public YUVImage(int width, int height) {
		this.width = width;
		this.height = height;

		yPlane = new byte[width * height];

		int cWidth = (width + 1) >> 1;
		int cHeight = (height + 1) >> 1;

		uPlane = new byte[cWidth * cHeight];
		vPlane = new byte[cWidth * cHeight];

	}

	/**
	 * Debug, show this image in a JFrame
	 * @param scale
	 * @param withChroma
	 * @param title
	 */
	public void show(int scale, boolean withChroma, String title) {
		BufferedImage image = new BufferedImage(width*scale, height*scale, BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				int chX = x >> 1;
				int chY = y >> 1;
				int chW = width >> 1;
				int index = y*width + x;
				int chIndex = chY*chW + chX;
				int val = 0;
				if (withChroma) {
					val = Util.yuvToRGBInt(Util.getUByte(yPlane, index), Util.getUByte(uPlane, chIndex), Util.getUByte(vPlane, chIndex));
				} else {
					val = Util.yuvToRGBInt(Util.getUByte(yPlane, index), 128, 128);
				}

				for (int j = 0; j < scale; ++j) {
					for (int i = 0; i < scale; ++i) {
						image.setRGB(x*scale+i, y*scale+j, val);
					}
				}
			}
		}

		JFrame frame = new JFrame(title);

		ImageIcon icon = new ImageIcon(image);

		JLabel label = new JLabel(icon, JLabel.CENTER);
		label.setSize(width*scale, height*scale);
		frame.add(label);
		frame.pack();

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setVisible(true);

	}

	/**
	 * Note, for performance reasons, this will ONLY work on BufferedImage's that use the TYPE_INT_ARGB or TYPE_INT_RGB data types.
	 * @param img
	 */
	public void fillFromBuffered(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();

		int blockW = (w + 15) & ~0x0f;
		int blockH = (h + 15) & ~0x0f;

		assert(blockW <= width);
		assert(blockH <= height);

		// Going to do two passes on this image, one for Y, one for UV as they use different
		// resolutions
		DataBufferInt ints = (DataBufferInt)img.getData().getDataBuffer();
		int[] rgbs = ints.getData();


		for (int y = 0; y < h; ++y) {

			for (int x = 0; x < w; ++x) {
				int luma = Util.rgbToY(rgbs[y*w+x]);
				yPlane[y*width+x] = (byte)luma;

				if ((y&1) == 0 && (x&1) == 0) {
					// Add color
					int u = Util.rgbToU(rgbs[y*w+x]);
					int v = Util.rgbToV(rgbs[y*w+x]);
					uPlane[y*width/4+x/2] = (byte)u;
					vPlane[y*width/4+x/2] = (byte)v;

				}
			}
		}
	}

}
