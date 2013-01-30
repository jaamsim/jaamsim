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
package com.jaamsim.video;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import com.jaamsim.video.vp8.Encoder;

public class AviTester {

	public static void main(String args[]) {

		try {
			BufferedImage img = ImageIO.read(new File(args[0]));

			BufferedImage intImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = intImg.createGraphics();
			g2.drawImage(img, null, null);

			Encoder enc = new Encoder();

			AviWriter w = new AviWriter(args[1], img.getWidth(), img.getHeight(), 30);
			for (int i = 0; i < 30; ++i) {
				ByteBuffer frame = enc.encodeFrame(intImg, false);
				w.addFrame(frame, i == 0);
				//System.out.printf("Frame: %d, size: %d\n", i, frame.limit());
			}
			w.close();
			System.out.println("Done");

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
