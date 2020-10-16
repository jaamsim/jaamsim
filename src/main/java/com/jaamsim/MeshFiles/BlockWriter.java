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
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.CRC32;

public class BlockWriter {

	private static long getBlockDataSize(DataBlock b) {
		long size = 0;
		for (DataBlock child : b.getChildren()) {
			size += getBlockTotalSize(child);
		}
		size += b.getDataSize();
		return size;
	}

	private static long getBlockTotalSize(DataBlock b) {
		try {
			byte[] nameBytes = b.getName().getBytes("UTF-8");
			return 29 + nameBytes.length + getBlockDataSize(b);
		} catch (UnsupportedEncodingException e) {
			throw new DataBlock.Error(e.getMessage());
		}
	}

	/**
	 * Write the block and sub blocks to the output stream
	 * Can throw a DataBlock.Error
	 * @param out
	 * @param b
	 */
	public static void writeBlock(OutputStream out, DataBlock b) {
		try {
			// Write out the block header
			byte[] nameBytes = b.getName().getBytes("UTF-8");
			if (nameBytes.length > 128) {
				throw new DataBlock.Error("Block name too large");
			}

			long payloadSize = getBlockDataSize(b);
			byte[] sizeBytes = new byte[8];
			BlockUtils.longToBytes(payloadSize, sizeBytes, 0);
			byte[] numChildrenBytes = new byte[4];
			BlockUtils.intToBytes(b.getChildren().size(), numChildrenBytes, 0);

			CRC32 headerCRC = new CRC32();
			headerCRC.update(nameBytes);
			headerCRC.update(0);
			headerCRC.update(numChildrenBytes);
			headerCRC.update(sizeBytes);
			byte[] crcBytes = new byte[4];
			BlockUtils.intToBytes((int)headerCRC.getValue(), crcBytes, 0);

			// header is ready, so start writing
			out.write(BlockUtils.header);
			out.write(crcBytes);
			out.write(nameBytes);
			out.write(0);
			out.write(numChildrenBytes);
			out.write(sizeBytes);

			// Now wrap the output stream to get a total payload CRC, while still being recursive
			BlockUtils.CRCOutputStream crcStream = new BlockUtils.CRCOutputStream(out);
			for(DataBlock child : b.getChildren()) {
				writeBlock(crcStream, child);
			}
			// Write the data section
			crcStream.write(b.getData(), 0, b.getDataSize());

			// re-use the CRC bytes array
			BlockUtils.intToBytes((int)crcStream.getCRC(), crcBytes, 0);
			out.write(crcBytes);
			out.write(BlockUtils.footer);

		} catch (IOException e) {
			throw new DataBlock.Error(e.getMessage());
		}
	}
}
