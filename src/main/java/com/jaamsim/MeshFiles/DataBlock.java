/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2015 JaamSim Software Inc.
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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import com.jaamsim.math.Mat4d;

/**
 * DataBlock is the base unit of data for the renderer binary format IO system
 * Blocks can be written to disk and can contain binary data, other blocks or a mixture of the two
 * API includes utility for serializing primitive types and strings.
 * @author matt.chudleigh
 *
 */
public class DataBlock {

	public static class Error extends RuntimeException {
		public Error(String errStr) {
			super(errStr);
		}
	}

	private final String name;
	private final byte[] data;
	private int dataSize = 0;
	private int readPos = 0;
	private final ArrayList<DataBlock> children;

	/** Create a new DataBlock with room for 'bufferSize' bytes of binary data
	 *  this method is intended for data that is being dynamically generated, eg by an exporter
	 */
	public DataBlock(String name, int bufferSize) {
		this.name = name;
		data = new byte[bufferSize];
		children = new ArrayList<>();
	}

	/**
	 * Create a DataBlock explicitly, this method is meant to be used by a file reader
	 * @param name
	 * @param data
	 * @param children
	 */
	public DataBlock(String name, byte[] data, ArrayList<DataBlock> children) {
		this.name = name;
		this.data = data;
		this.children = children;
		dataSize = data.length;
	}

	public int getDataSize() {
		return dataSize;
	}

	public void setReadPosition(int pos) {
		if (pos > dataSize) {
			throw new Error("Read set past end of block");
		}
		readPos = pos;
	}

	public int getReadPosition() {
		return readPos;
	}

	public boolean atEnd() {
		return readPos == dataSize;
	}

	public byte[] getData() {
		return data;
	}

	public ArrayList<DataBlock> getChildren() {
		return children;
	}

	public void addChildBlock(DataBlock child) {
		children.add(child);
	}

	public String getName() {
		return name;
	}

	private void checkWriteSize(int newSize) {
		if (dataSize + newSize > data.length) {
			throw new Error("DataBlock write too large");
		}
	}

	private void checkReadSize(int newSize) {
		if (readPos + newSize > dataSize) {
			throw new Error("DataBlock read too large");
		}
	}

	// Add all of a byte array to the data
	public void writeData(byte[] d) {
		checkWriteSize(d.length);

		System.arraycopy(d, 0, data, dataSize, d.length);
		dataSize += d.length;
	}

	public void writeByte(byte b) {
		checkWriteSize(1);
		data[dataSize++] = b;
	}

	public void writeDouble(double d) {
		writeLong(Double.doubleToLongBits(d));
	}
	public void writeLong(long l) {
		checkWriteSize(8);

		BlockUtils.longToBytes(l, data, dataSize);
		dataSize += 8;
	}

	public void writeFloat(float f) {
		writeInt(Float.floatToIntBits(f));
	}

	public void writeInt(int i) {
		checkWriteSize(4);

		BlockUtils.intToBytes(i, data, dataSize);
		dataSize += 4;
	}

	public void writeString(String s) {
		try {
			byte[] utf8 = s.getBytes("UTF-8");
			checkWriteSize(s.length() + 1); // Room for the string and null terminator

			System.arraycopy(utf8, 0, data, dataSize, utf8.length);
			dataSize += utf8.length;
			data[dataSize++] = 0; // Add the null terminator

		} catch (UnsupportedEncodingException e) {
			throw new Error(e.getMessage());
		}
	}

	public void writeMat4d(Mat4d mat) {
		checkWriteSize(8*16);

		writeDouble(mat.d00);
		writeDouble(mat.d01);
		writeDouble(mat.d02);
		writeDouble(mat.d03);

		writeDouble(mat.d10);
		writeDouble(mat.d11);
		writeDouble(mat.d12);
		writeDouble(mat.d13);

		writeDouble(mat.d20);
		writeDouble(mat.d21);
		writeDouble(mat.d22);
		writeDouble(mat.d23);

		writeDouble(mat.d30);
		writeDouble(mat.d31);
		writeDouble(mat.d32);
		writeDouble(mat.d33);

	}

	public byte readByte() {
		checkReadSize(1);
		return data[readPos++];
	}

	public int readInt() {
		checkReadSize(4);

		int ret = BlockUtils.intFromBytes(data, readPos);
		readPos += 4;
		return ret;
	}

	public float readFloat() {
		int bits = readInt();
		return Float.intBitsToFloat(bits);
	}

	public long readLong() {
		checkReadSize(8);

		long ret = BlockUtils.longFromBytes(data, readPos);
		readPos += 8;
		return ret;
	}

	public double readDouble() {
		long bits = readLong();
		return Double.longBitsToDouble(bits);
	}

	public String readString() {
		// Find the next null terminator
		int startPos = readPos;
		while (readPos++ < dataSize) {
			if (data[readPos] == 0)
				break;
		}
		if (readPos == dataSize) {
			throw new Error("Read string past end of block");
		}
		int size = readPos - startPos;
		readPos++; // Skip the null byte
		byte[] bytes = new byte[size];
		System.arraycopy(data, startPos, bytes, 0, size);

		try {
			return new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new Error(e.getMessage());
		}
	}

	public Mat4d readMat4d() {
		Mat4d ret = new Mat4d();

		ret.d00 = readDouble();
		ret.d01 = readDouble();
		ret.d02 = readDouble();
		ret.d03 = readDouble();

		ret.d10 = readDouble();
		ret.d11 = readDouble();
		ret.d12 = readDouble();
		ret.d13 = readDouble();

		ret.d20 = readDouble();
		ret.d21 = readDouble();
		ret.d22 = readDouble();
		ret.d23 = readDouble();

		ret.d30 = readDouble();
		ret.d31 = readDouble();
		ret.d32 = readDouble();
		ret.d33 = readDouble();

		return ret;
	}
	/**
	 * Returns the first child with a matching name. Utility
	 * @param name
	 */
	public DataBlock findChildByName(String name) {
		for (DataBlock b : children) {
			if (b.name.equals(name)) {
				return b;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return name;
	}
}
