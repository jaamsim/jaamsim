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
package com.jaamsim.MeshFiles;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;

public class TestDataBlocks {

	@Test
	public void testAddByteToBlock() throws Throwable {
		DataBlock block = new DataBlock("Blockity", 128);

		byte[] bytes = new byte[128];

		for (int i = 0; i < 128; ++i) {
			bytes[i] = (byte)(i * 2);
		}

		for (int i = 0; i < 128; ++i) {
			block.writeByte(bytes[i]);
		}

		block.setReadPosition(0);

		for (int i = 0; i < 128; ++i) {
			byte val = block.readByte();
			assertTrue(val == bytes[i]);
		}
	}

	@Test
	public void testAddDoubleToBlock() throws Throwable {
		DataBlock block = new DataBlock("Blockity", 128);

		double[] doubles = new double[16];

		for (int i = 0; i < 16; ++i) {
			doubles[i] = i * 42.4;
		}

		for (int i = 0; i < 16; ++i) {
			block.writeDouble(doubles[i]);
		}

		block.setReadPosition(0);

		for (int i = 0; i < 16; ++i) {
			double val = block.readDouble();
			assertTrue(val == doubles[i]);
		}
	}

	@Test
	public void testAddFloatToBlock() throws Throwable {
		DataBlock block = new DataBlock("Blockity", 128);

		float[] floats = new float[16];

		for (int i = 0; i < 16; ++i) {
			floats[i] = i * 42.4f;
		}

		for (int i = 0; i < 16; ++i) {
			block.writeFloat(floats[i]);
		}

		block.setReadPosition(0);

		for (int i = 0; i < 16; ++i) {
			float val = block.readFloat();
			assertTrue(val == floats[i]);
		}
	}

	@Test
	public void testAddIntToBlock() throws Throwable {
		DataBlock block = new DataBlock("Blockity", 128);

		int[] ints = new int[32];

		for (int i = 0; i < 32; ++i) {
			ints[i] = i * 42;
		}

		for (int i = 0; i < 32; ++i) {
			block.writeInt(ints[i]);
		}

		block.setReadPosition(0);

		for (int i = 0; i < 32; ++i) {
			int val = block.readInt();
			assertTrue(val == ints[i]);
		}

	}

	@Test
	public void testAddLongToBlock() throws Throwable {
		DataBlock block = new DataBlock("Blockity", 256);

		long[] longs= new long[32];

		for (int i = 0; i < 32; ++i) {
			longs[i] = i * 42L;
		}

		for (int i = 0; i < 32; ++i) {
			block.writeLong(longs[i]);
		}

		block.setReadPosition(0);

		for (int i = 0; i < 32; ++i) {
			long val = block.readLong();
			assertTrue(val == longs[i]);
		}
	}

	@Test
	public void testAddStringsToBlock() throws Throwable {
		DataBlock block = new DataBlock("Blockity", 256);

		String[] strings = new String[5];

		strings[0] = new String("Foo");
		strings[1] = new String("Bar?");
		strings[2] = new String("Giggity!");
		strings[3] = new String("I am a string!!!");
		strings[4] = new String("Arglbargle");

		for (int i = 0; i < 5; ++i) {
			block.writeString(strings[i]);
		}

		block.setReadPosition(0);

		for (int i = 0; i < 5; ++i) {
			String val = block.readString();
			assertTrue(val.equals(strings[i]));
		}
	}

	@Test
	public void testSimpleWriteAndRead() throws Throwable {
		String blockName = "Blockity";
		DataBlock block = new DataBlock(blockName, 32);
		for (int i = 0; i < 8; ++i) {
			block.writeInt(i * 42);
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BlockWriter.writeBlock(out, block);

		byte[] binaryBlock = out.toByteArray();

		ByteArrayInputStream in = new ByteArrayInputStream(binaryBlock);
		DataBlock readBlock = BlockReader.readBlock(in);
		assertTrue(readBlock.getName().equals(blockName));
		for (int i = 0; i < 8; ++i) {
			int val = readBlock.readInt();
			assertTrue(val == i * 42);
		}
	}

	@Test
	public void testComplexWriteAndRead() throws Throwable {
		String blockName = "Blockity";
		String child1Name = "Kiddy";
		String child2Name = "Kiddy yet again";
		String grandChildName = "Wee One";

		DataBlock block = new DataBlock(blockName, 32);
		for (int i = 0; i < 8; ++i) {
			block.writeInt(i * 42);
		}

		DataBlock child1 = new DataBlock(child1Name, 256);
		for (int i = 0; i < 32; ++i) {
			child1.writeDouble(i * 3500);
		}

		DataBlock child2 = new DataBlock(child2Name, 256);
		child2.writeString("Fee");
		child2.writeString("Fi");
		child2.writeString("Fo");
		child2.writeString("Fum");

		DataBlock grandChild = new DataBlock(grandChildName, 128);
		for (int i = 0; i < 4; ++i) {
			grandChild.writeLong(i * 16000l);
			grandChild.writeDouble(i * 16000);
		}

		child1.addChildBlock(grandChild);

		block.addChildBlock(child1);
		block.addChildBlock(child2);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BlockWriter.writeBlock(out, block);

		byte[] binaryBlock = out.toByteArray();

		ByteArrayInputStream in = new ByteArrayInputStream(binaryBlock);
		DataBlock readBlock = BlockReader.readBlock(in);
		assertTrue(readBlock.getName().equals(blockName));

		for (int i = 0; i < 8; ++i) {
			int val = readBlock.readInt();
			assertTrue(val == i * 42);
		}

		assertTrue(readBlock.getChildren().size() == 2);

		DataBlock readChild1 = readBlock.getChildren().get(0);
		DataBlock readChild2 = readBlock.getChildren().get(1);

		assertTrue(readChild1.getChildren().size() == 1);
		DataBlock readGrandchild = readChild1.getChildren().get(0);

		assertTrue(readChild1.getName().equals(child1Name));
		assertTrue(readChild2.getName().equals(child2Name));
		assertTrue(readGrandchild.getName().equals(grandChildName));

		for (int i = 0; i < 32; ++i) {
			assertTrue(readChild1.readDouble() == i * 3500);
		}

		assertTrue(readChild2.readString().equals("Fee"));
		assertTrue(readChild2.readString().equals("Fi"));
		assertTrue(readChild2.readString().equals("Fo"));
		assertTrue(readChild2.readString().equals("Fum"));

		for (int i = 0; i < 4; ++i) {
			assertTrue(grandChild.readLong() == i * 16000);
			assertTrue(grandChild.readDouble() == i * 16000);
		}
	}
}
