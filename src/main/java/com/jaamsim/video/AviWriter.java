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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import com.jaamsim.basicsim.Log;
import com.jaamsim.ui.LogBox;

public class AviWriter {

	private static class FrameEntry {
		int size;
		int pos;
		boolean keyFrame;
	}

	private FileChannel fc;
	private RandomAccessFile raf;

	private ArrayList<FrameEntry> index;

	private int width, height;
	private int numFrames;

	private int moviSizePos;

	private int chunkPos;

	public AviWriter(String filename, int width, int height, int numFrames) {

		try {
			this.width = width;
			this.height = height;
			this.numFrames = numFrames;

			raf = new RandomAccessFile(filename, "rw");
			raf.setLength(0);
			fc = raf.getChannel();

			writeHeader();

			index = new ArrayList<>(numFrames);

		} catch (IOException ex) {
			fc = null;
			// TODO log this error
			Log.logException(ex);
		}
	}

	public boolean isOpen() {
		return fc != null;
	}

	public void close() {
		try {
			// The size of the file before the index is written
			int dataSize = (int)fc.position();

			writeIndex();

			// The size of the file with the index
			int fileSize = (int)fc.position();

			ByteBuffer sizeBuff = ByteBuffer.allocate(4);
			sizeBuff.order(ByteOrder.LITTLE_ENDIAN);

			// Fixup some of the missing sizes

			fc.position(4);
			sizeBuff.putInt(fileSize - 8);
			sizeBuff.flip();
			fc.write(sizeBuff);

			sizeBuff.clear();

			fc.position(moviSizePos);
			sizeBuff.putInt(dataSize - moviSizePos - 4);
			sizeBuff.flip();
			fc.write(sizeBuff);

			fc.close();
			fc = null;
		} catch (IOException ex) {
			// Ignore for now...
			Log.logException(ex);
		}
	}

	private void writeIndex() {
		ByteBuffer buff = ByteBuffer.allocate(8 + 16 * index.size());
		buff.order(ByteOrder.LITTLE_ENDIAN);

		writeFourCC(buff, "idx1");
		buff.putInt(16*index.size()); // struct size

		for (FrameEntry f : index) {
			writeFourCC(buff, "00dc");
			buff.putInt(f.keyFrame ? 0x10 : 0); // Key frame flag
			buff.putInt(f.pos);
			buff.putInt(f.size);
		}

		buff.flip();
		try {
			fc.write(buff);
		} catch (IOException ex) {
			Log.logException(ex);
			throw new RuntimeException(ex);
		}
	}

	private static void writeFourCC(ByteBuffer buff, String fourCC) {
		byte[] b = new byte[4];
		for (int i = 0; i < 4; ++i)
			b[i] = (byte)fourCC.charAt(i);

		buff.put(b);
	}

	private void writeHeader() throws IOException {
		ByteBuffer header = ByteBuffer.allocate(256);
		header.order(ByteOrder.LITTLE_ENDIAN);

		writeFourCC(header, "RIFF");
		header.putInt(0); // We will fill this size in when we are done

		writeFourCC(header, "AVI ");
		writeFourCC(header, "LIST");

		header.putInt(192); // Total header size
		int hdrlPos = header.position();


		writeFourCC(header, "hdrl");
		writeFourCC(header, "avih");

		header.putInt(0x38); // Size of the MainAVIHeader
		header.putInt(33000000); // 33 ms per frame
		header.putInt(0); // Max bytes per second
		header.putInt(0); // reserved
		header.putInt(0x10); // flags (has index)
		header.putInt(numFrames);
		header.putInt(0); // Initial frames
		header.putInt(1); // num streams
		header.putInt(width * height * 3); // buffer size
		header.putInt(width);
		header.putInt(height);

		header.putInt(0); // reserved
		header.putInt(0);
		header.putInt(0);
		header.putInt(0);

		writeFourCC(header, "LIST");

		header.putInt(116); // Size of the rest of the header
		int strlPos = header.position();


		writeFourCC(header, "strl");
		writeFourCC(header, "strh");
		header.putInt(0x38); // Size of the AVIStreamHeader

		writeFourCC(header, "vids");
		writeFourCC(header, "VP80");

		header.putInt(0); // flags
		header.putInt(0); // priority
		header.putInt(0); // initial frames

		header.putInt(1); // scale
		header.putInt(30); // rate

		header.putInt(0); // start

		header.putInt(numFrames/33);

		header.putInt(width * height * 3);

		header.putInt(-1); // default quality
		header.putInt(0); // sample size

		// These 3 make up the RECT struct
		header.putInt(0);
		header.putShort((short)width);
		header.putShort((short)height);

		writeFourCC(header, "strf");
		header.putInt(0x28); // chunk size
		header.putInt(0x28); // struct size

		header.putInt(width);
		header.putInt(height);
		header.putShort((short)1); // planes
		header.putShort((short)24); // bpp

		writeFourCC(header, "VP80");
		header.putInt(width * height * 3); // buffer size

		header.putInt(0); // other info
		header.putInt(0);
		header.putInt(0);
		header.putInt(0);

		assert(header.position() - hdrlPos == 192);
		assert(header.position() - strlPos == 116);

		writeFourCC(header, "LIST");

		moviSizePos = header.position();

		header.putInt(0); // TODO, work out this size
		writeFourCC(header, "movi");

		chunkPos = header.position();

		header.flip();

		fc.write(header);

		// Header is written and we are ready for data
	}

	public void addFrame(ByteBuffer frameData, boolean keyFrame) {
		ByteBuffer header = ByteBuffer.allocate(8);
		header.order(ByteOrder.LITTLE_ENDIAN);

		int extraBytes = 0;
		if ((frameData.limit() & 3) != 0) {
			// We will align to 4 byte boundaries
			extraBytes = 4 - (frameData.limit() & 3);
		}

		FrameEntry f = new FrameEntry();
		f.pos = chunkPos;
		f.size = frameData.limit() + 8 + extraBytes;
		f.keyFrame = keyFrame;
		chunkPos += f.size;
		index.add(f);

		writeFourCC(header, "00dc");
		header.putInt(frameData.limit() + extraBytes);
		header.flip();
		try {
			fc.write(header);
			fc.write(frameData);

			header.clear();
			for (int i = 0; i < extraBytes; ++i) {
				header.put((byte)0);
			}
			header.flip();
			if (extraBytes != 0) {
				fc.write(header);
			}


		} catch (IOException ex) {
			Log.logException(ex);
			throw new RuntimeException(ex);
		}

	}
}
