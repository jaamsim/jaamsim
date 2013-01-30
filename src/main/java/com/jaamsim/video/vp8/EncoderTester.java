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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

public class EncoderTester {

	public static void main(String[] args) {
		try {
			BufferedImage img = ImageIO.read(new File(args[0]));

			BufferedImage intImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = intImg.createGraphics();
			g2.drawImage(img, null, null);

			Encoder enc = new Encoder();

//			long startTime = System.nanoTime();
			ByteBuffer frame = enc.encodeFrame(intImg, true);
//			long endTime = System.nanoTime();

//			long time = (endTime - startTime) / 1000000;
//			System.out.print(String.format("Encode time %d ms\n", time));

			FileOutputStream out = new FileOutputStream(args[1]+"Key.vp8");
			out.getChannel().write(frame);
			out.close();

			frame = enc.encodeFrame(intImg, false);

			out = new FileOutputStream(args[1]+"Inter.vp8");
			out.getChannel().write(frame);
			out.close();

			System.out.println("Success!");

		} catch (IOException ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
}
