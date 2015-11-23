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
