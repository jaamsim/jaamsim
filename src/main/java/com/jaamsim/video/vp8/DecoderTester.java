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

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class DecoderTester {

	public static void main(String[] args) {
		try {
			Decoder dec = new Decoder();

			File f = new File(args[0]);
			FileInputStream inStream = new FileInputStream(f);
			FileChannel fc = inStream.getChannel();
			ByteBuffer frame = ByteBuffer.allocate((int)fc.size());
			fc.read(frame);
			frame.flip();

			dec.decodeFrame(frame);

			dec.currentFrame.show(1, true, "");

			inStream.close();

		} catch (Throwable t) {
			t.printStackTrace();
			throw new RuntimeException(t);
		}
	}
}
