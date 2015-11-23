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
