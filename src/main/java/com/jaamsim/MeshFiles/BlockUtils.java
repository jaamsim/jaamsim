/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.MeshFiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;

public class BlockUtils {

	static final byte[] header;
	static final byte[] footer;

	static {
		header = new byte[4];
		header[0] = 'B';
		header[1] = 'L';
		header[2] = 'C';
		header[3] = 'K';

		footer = new byte[4];
		footer[0] = 'B';
		footer[1] = 'E';
		footer[2] = 'N';
		footer[3] = 'D';
	}


	public static void intToBytes(int i, byte[] bytes, int offset) {
		bytes[offset + 0] = (byte)((i & 0xFF000000) >> 24);
		bytes[offset + 1] = (byte)((i & 0x00FF0000) >> 16);
		bytes[offset + 2] = (byte)((i & 0x0000FF00) >>  8);
		bytes[offset + 3] = (byte)((i & 0x000000FF));
	}

	public static void longToBytes(long l, byte[] bytes, int offset) {
		bytes[offset + 0] = (byte)((l & 0xFF00000000000000L) >> 56);
		bytes[offset + 1] = (byte)((l & 0x00FF000000000000L) >> 48);
		bytes[offset + 2] = (byte)((l & 0x0000FF0000000000L) >> 40);
		bytes[offset + 3] = (byte)((l & 0x000000FF00000000L) >> 32);
		bytes[offset + 4] = (byte)((l & 0x00000000FF000000L) >> 24);
		bytes[offset + 5] = (byte)((l & 0x0000000000FF0000L) >> 16);
		bytes[offset + 6] = (byte)((l & 0x000000000000FF00L) >>  8);
		bytes[offset + 7] = (byte)((l & 0x00000000000000FFL));
	}

	public static int intFromBytes(byte[] bytes, int offset) {
		int ret = 0;
		ret |= (bytes[offset + 0] & 0xFF) << 24;
		ret |= (bytes[offset + 1] & 0xFF) << 16;
		ret |= (bytes[offset + 2] & 0xFF) << 8;
		ret |= bytes[offset + 3] & 0xFF;
		return ret;
	}

	public static long longFromBytes(byte[] bytes, int offset) {
		long ret = 0;
		ret |= (bytes[offset + 0] & 0xFFL) << 56;
		ret |= (bytes[offset + 1] & 0xFFL) << 48;
		ret |= (bytes[offset + 2] & 0xFFL) << 40;
		ret |= (bytes[offset + 3] & 0xFFL) << 32;
		ret |= (bytes[offset + 4] & 0xFFL) << 24;
		ret |= (bytes[offset + 5] & 0xFFL) << 16;
		ret |= (bytes[offset + 6] & 0xFFL) << 8;
		ret |= (bytes[offset + 7] & 0xFFL);
		return ret;
	}

	public static class CRCOutputStream extends OutputStream {
		private final CRC32 crc = new CRC32();
		private final OutputStream wrapped;
		public CRCOutputStream(OutputStream wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public void write(int b) throws IOException {
			crc.update(b);
			wrapped.write(b);
		}
		@Override
		public void write(byte[] bs) throws IOException{
			crc.update(bs);
			wrapped.write(bs);
		}

		@Override
		public void write(byte[] bs, int off, int len) throws IOException {
			crc.update(bs, off, len);
			wrapped.write(bs, off, len);
		}

		public long getCRC() {
			return crc.getValue();
		}
	}

	public static class CRCInputStream extends InputStream {

		private final CRC32 crc = new CRC32();
		private final InputStream wrapped;
		private long bytesRead = 0;
		private boolean doCRC;
		public CRCInputStream(InputStream wrapped, boolean doCRC) {
			this.wrapped = wrapped;
			this.doCRC = doCRC;
		}

		@Override
		public int read() throws IOException {
			int ret = wrapped.read();
			bytesRead++;
			if (doCRC)
				crc.update(ret);
			return ret;
		}

		@Override
		public int read(byte[] b) throws IOException {
			int read = wrapped.read(b);
			if (doCRC)
				crc.update(b, 0, read);
			bytesRead += read;
			return read;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int read = wrapped.read(b, off, len);
			if (doCRC)
				crc.update(b, off, read);
			bytesRead += read;
			return read;
		}

		public long getCRC() {
			return crc.getValue();
		}

		public long getBytesRead() {
			return bytesRead;
		}

	}
}
