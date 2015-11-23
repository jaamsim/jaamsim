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
