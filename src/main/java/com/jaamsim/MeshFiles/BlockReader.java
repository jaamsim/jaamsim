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
import java.net.URI;
import java.util.ArrayList;
import java.util.zip.CRC32;

public class BlockReader {

	private static final boolean CHECK_PAYLOAD_CRC = false;

	public static final MeshData parse(URI asset) throws Exception {
		InputStream inStream = asset.toURL().openStream();
		DataBlock block = readBlock(inStream);
		return new MeshData(false, block, asset.toURL());
	}

	public static DataBlock readBlock(InputStream in) {
		try {
			byte[] readBuffer = new byte[128];

			// Read the header
			readLoop(in, readBuffer, 0, 4);
			for (int i = 0; i < 4; ++i) {
				if (readBuffer[i] != BlockUtils.header[i])
					throw new DataBlock.Error("Missing block header");
			}

			// Read the header CRC
			readLoop(in, readBuffer, 0, 4);
			int headerValue = BlockUtils.intFromBytes(readBuffer, 0);

			CRC32 headerCRC = new CRC32();

			// Read until a null byte, or max 128
			int stringSize = 0;
			while (stringSize < 128) {
				byte b = (byte)in.read();
				headerCRC.update(b);
				readBuffer[stringSize] = b;
				if (b == 0)
					break;
				++stringSize;
			}
			if (stringSize == 128) {
				throw new DataBlock.Error("No null terminator for block name");
			}

			String blockName = new String(readBuffer, 0, stringSize, "UTF-8");

			// Read the number of children
			readLoop(in, readBuffer, 0, 4);

			int numChildren = BlockUtils.intFromBytes(readBuffer, 0);
			headerCRC.update(readBuffer, 0, 4);

			// Read the block size
			readLoop(in, readBuffer, 0, 8);

			long payloadSize = BlockUtils.longFromBytes(readBuffer, 0);
			headerCRC.update(readBuffer, 0, 8);

			// check the header Adds up
			if ((int)headerCRC.getValue() != headerValue) {
				throw new DataBlock.Error("Header CRC mismatch");
			}

			ArrayList<DataBlock> children = new ArrayList<>();

			BlockUtils.CRCInputStream wrappedIn = new BlockUtils.CRCInputStream(in, CHECK_PAYLOAD_CRC);

			for (int i = 0; i < numChildren; ++i) {
				children.add(readBlock(wrappedIn));
			}
			// Now read the remainder of the payload
			long remainingBytes = payloadSize - wrappedIn.getBytesRead();
			if (remainingBytes > Integer.MAX_VALUE) throw new DataBlock.Error("Block is too big and broke java");

			byte[] data = new byte[(int)remainingBytes];
			readLoop(in, data, 0, (int)remainingBytes);

			// Check the CRC and footer
			readLoop(in, readBuffer, 0, 4);

			if (CHECK_PAYLOAD_CRC) {
				int payloadValue = BlockUtils.intFromBytes(readBuffer, 0);
				if (payloadValue != (int)wrappedIn.getCRC())
					throw new DataBlock.Error("Block payload CRC mismatch");
			}

			// Finally read the footer
			readLoop(in, readBuffer, 0, 4);
			for (int i = 0; i < 4; ++i) {
				if (readBuffer[i] != BlockUtils.footer[i])
					throw new DataBlock.Error("Missing block header");
			}

			// Everything checks out here, return the block
			return new DataBlock(blockName, data, children);

		} catch (Exception e) {
			throw new DataBlock.Error(e.getMessage());
		}
	}

	/**
	 * Wrapper around InputStream.read() that keeps reading until the requested amount is found, or EOF
	 * Throws on error
	 */
	private static void readLoop(InputStream in, byte[] buffer, int offset, int size) throws IOException {
		int total = 0;
		int bytesRead;
		while (total < size) {
			bytesRead = in.read(buffer, total, size - total);

			if (bytesRead == -1)
				throw new DataBlock.Error("Unexpected End of stream");

			total += bytesRead;
		}
	}
}
