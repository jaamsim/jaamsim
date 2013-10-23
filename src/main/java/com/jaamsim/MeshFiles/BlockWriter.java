/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
	 * @param d
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
