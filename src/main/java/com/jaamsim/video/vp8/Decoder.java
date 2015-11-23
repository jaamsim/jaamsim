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

import java.nio.ByteBuffer;


/**
 * Basic VP8 decoder, holds all information needed to decode a stream of frames
 *
 */
@SuppressWarnings("unused")
public class Decoder {

	private final static int CURRENT_FRAME = 0;
	private final static int LAST_FRAME    = 1;
	private final static int GOLDEN_FRAME  = 2;
	private final static int ALTREF_FRAME  = 3;

	// Header information
	private boolean isKeyFrame;
	private int bitstreamVersion;
	private boolean isShown;

	private BoolDecoder p0Dec;

	private int frameWidth;
	private int frameHeight;

	private int widthScale;
	private int heightScale;

	private int mbRows;
	private int mbCols;

	//private boolean frameSizeUpdated;

	// TODO add segment header info
	private boolean simpleFilter;
	private int filterLevel;
	private int sharpness;
	private boolean filterDeltaEnabled;
	private final int refFilterDelta[] = new int[4];
	private final int modeFilterDelta[] = new int[4];

	private int numTokenPartitions;
	private BoolDecoder tokenDecoders[];

	// Dequant info
	private int y1ACDeqIndex;
	//private boolean hasDequantDeltas;
	private int y1DCDelta;
	private int y2ACDelta;
	private int y2DCDelta;
	private int uvACDelta;
	private int uvDCDelta;

	// Reference info
	private boolean refreshLast;
	private boolean refreshGolden;
	private boolean refreshAltRef;
	private int copyGolden;
	private int copyAltRef;
	private final boolean[] signBiases = new boolean[4];
	private boolean refreshEntropy;

	private TokenProbs tokenProbs;
	private TokenProbs savedProbs;
	private boolean saveProbsValid;

	private boolean skipCoeffEnabled;
	private int skipCoeffProb;

	private final int yModeProbs[] = new int[4];
	private final int uvModeProbs[] = new int[3];

	private static class QuantFactor {
		int y1AC;
		int y1DC;
		int y2AC;
		int y2DC;
		int uvAC;
		int uvDC;
	}

	private QuantFactor qf;

	private static class MBInfo {
		int yMode;
		int uvMode;
		int segID;
		int refFrame;
		boolean skipCoeff;
		int splitMVPart;
		short mvX;
		short mvY;
		int eobMask; // TODO figure out what this does...

		int[] subBlockModes = new int[16];
	}

	private MBInfo[] mbInfos;
	private MBInfo dummyMBInfo;


	private static class TokenEnt {
		int[] v = new int[9];
		TokenEnt() {
			for (int i = 0; i < 9; ++i) { v[i] = 0; }
		}
		void clear(boolean clearY2) {
			for (int i = 0; i < 8; ++i) { v[i] = 0; }
			if (clearY2) { v[8] = 0; }
		}
	}

	static class Tokens {
		short[] v = new short[16];
	}

	static class MBTokens {
		Tokens t[] = new Tokens[25];
		boolean hasValues = false;
		MBTokens() {
			for (int i = 0; i < t.length; ++i) {
				t[i] = new Tokens();
			}
		}
	}

	private MBTokens mbTokens[];
	private final short[] temp = new short[16];

	private static class DequantFactors {
		int index = -1;
		short factors[][] = new short[3][];

		//init
		{
			for (int i = 0; i < 3; ++i) {
				factors[i] = new short[2];
			}
		}

	}

	public YUVImage currentFrame;
	public YUVImage lastFrame;
	public YUVImage goldenFrame;
	public YUVImage altRefFrame;

	public YUVImage predRef;
	public YUVImage finalRef;
	public boolean testPred = false;

	private final DequantFactors deqFactors[] = new DequantFactors[4];

	//init
	{
		for (int i = 0; i < 4; ++i) {
			deqFactors[i] = new DequantFactors();
		}

		dummyMBInfo = new MBInfo();
		dummyMBInfo.yMode = Defs.B_PRED;
		for (int i = 0; i < 16; ++i) {
			dummyMBInfo.subBlockModes[i] = Defs.B_DC_PRED;
		}
	}

	public Decoder() {

	}

	private int getBits(long data, int bitOffset, int bitLength) {
		int mask = ~((-1)<<(bitLength));
		return (int)((data >> bitOffset) & mask);
	}

	private void decodeFrameHeader(ByteBuffer frameData) throws VP8Exception {
		if (frameData.limit() < 10) {
			throw new VP8Exception(String.format("Frame too small: %d bytes", frameData.limit()));
		}

		long rawData = 0;
		rawData += Util.getUByte(frameData);
		rawData += (Util.getUByte(frameData) << 8);
		rawData += (Util.getUByte(frameData) << 16);

		isKeyFrame = (getBits(rawData, 0, 1) == 0);
		bitstreamVersion = getBits(rawData, 1, 3);
		if (bitstreamVersion > 3) {
			throw new VP8Exception(String.format("Invalid bitstream version: %d", bitstreamVersion));
		}
		isShown = (getBits(rawData, 4, 1) == 1);
		int part0Size = getBits(rawData, 5, 19);

		if (isKeyFrame) {
			keyFrameClearState();

			if (Util.getUByte(frameData) != 0x9d || Util.getUByte(frameData) != 0x01 || Util.getUByte(frameData) != 0x2a) {
				throw new VP8Exception("Magic code not found in keyframe");
			}

			rawData = 0;
			rawData += Util.getUByte(frameData);
			rawData += (Util.getUByte(frameData) << 8);
			rawData += (Util.getUByte(frameData) << 16);
			rawData += (Util.getUByte(frameData) << 24);

			goldenFrame = null;
			altRefFrame = null;
			lastFrame = null;

			// For now re-allocate the current buffer every frame (yeah, this is wasteful)
			frameWidth = getBits(rawData, 0, 14);
			widthScale = getBits(rawData, 14, 2);
			frameHeight = getBits(rawData, 16, 14);
			heightScale = getBits(rawData, 30, 2);


			mbCols = (frameWidth + 15) >> 4;
			mbRows = (frameHeight + 15) >> 4;

			// Make the buffer fit whole macro blocks
			currentFrame = new YUVImage(mbCols << 4, mbRows << 4);

			mbInfos = new MBInfo[mbCols * mbRows];
			mbTokens = new MBTokens[mbCols * mbRows];
			for (int i = 0; i < mbRows * mbCols; ++i) {
				mbInfos[i] = new MBInfo();
				mbTokens[i] = new MBTokens();
			}
		}

		if (part0Size > frameData.limit() - frameData.position()) {
			throw new VP8Exception(String.format("Incomplete frame expected: %d more bytes", part0Size));
		}

		ByteBuffer part0Buffer = frameData.slice();
		part0Buffer.limit(part0Size);

		p0Dec = new BoolDecoder(part0Buffer);

		frameData.position(frameData.position() + part0Size);
	}

	private void decodeLoopFilterHeader() {
		simpleFilter = p0Dec.getFlag();
		filterLevel = p0Dec.getLitUInt(6);
		sharpness = p0Dec.getLitUInt(3);
		filterDeltaEnabled = p0Dec.getFlag();

		if (filterDeltaEnabled && p0Dec.getFlag()) {
			for (int i = 0; i < 4; ++i) {
				refFilterDelta[i] = p0Dec.getOptInt(6);
			}
			for (int i = 0; i < 4; ++i) {
				modeFilterDelta[i] = p0Dec.getOptInt(6);
			}
		}
	}

	private void decodePartionInfo(ByteBuffer frameData) throws VP8Exception {
		numTokenPartitions = p0Dec.getLitUInt(2);
		if (numTokenPartitions != 0) throw new VP8Exception(String.format("Invalid number of partitions: %d", numTokenPartitions));

		tokenDecoders = new BoolDecoder[1];

		tokenDecoders[0] = new BoolDecoder(frameData);
	}

	private void decodeDequantHeader() {
		y1ACDeqIndex = p0Dec.getLitUInt(7);

		y1DCDelta = p0Dec.getOptInt(4);
		y2DCDelta = p0Dec.getOptInt(4);
		y2ACDelta = p0Dec.getOptInt(4);
		uvDCDelta = p0Dec.getOptInt(4);
		uvACDelta = p0Dec.getOptInt(4);

		qf = new QuantFactor();
		qf.y1DC = Defs.DC_Q_LOOKUP[y1ACDeqIndex + y1DCDelta];
		qf.y1AC = Defs.AC_Q_LOOKUP[y1ACDeqIndex];

		qf.y2DC = Defs.DC_Q_LOOKUP[y1ACDeqIndex + y2DCDelta] * 2;
		qf.y2AC = Defs.AC_Q_LOOKUP[y1ACDeqIndex + y2ACDelta] * 155 / 100;

		qf.uvDC = Defs.DC_Q_LOOKUP[y1ACDeqIndex + uvDCDelta];
		qf.uvAC = Defs.AC_Q_LOOKUP[y1ACDeqIndex + uvACDelta];

		if (qf.y2AC < 8) { qf.y2AC = 8; }
}

	private void decodeReferenceHeader() {
		if (isKeyFrame) {

			refreshGolden = true;
			refreshAltRef = true;
			copyGolden = 0;
			copyAltRef = 0;
			signBiases[GOLDEN_FRAME] = false;
			signBiases[ALTREF_FRAME] = false;
			refreshEntropy = p0Dec.getFlag();
			refreshLast = true;

		} else {

			refreshGolden = p0Dec.getFlag();
			refreshAltRef = p0Dec.getFlag();
			copyGolden = 0;
			if (!refreshGolden)
				copyGolden = p0Dec.getLitUInt(2);

			copyAltRef = 0;
			if (!refreshAltRef)
				copyAltRef = p0Dec.getLitUInt(2);

			signBiases[GOLDEN_FRAME] = p0Dec.getFlag();
			signBiases[ALTREF_FRAME] = p0Dec.getFlag();
			refreshEntropy = p0Dec.getFlag();
			refreshLast = p0Dec.getFlag();
		}
	}

	private void decodeEntropyHeader() {

		tokenProbs.updateProbs(p0Dec);

		skipCoeffEnabled = p0Dec.getFlag();
		if (skipCoeffEnabled)
			skipCoeffProb = p0Dec.getLitUInt(8);

		// TODO: parse interframe probability updates here
		assert(isKeyFrame);
	}

	private int getAboveBMode(MBInfo curr, MBInfo above, int i) {
		if (i < 4) {
			if (above.yMode == Defs.DC_PRED) return Defs.B_DC_PRED;
			if (above.yMode ==  Defs.H_PRED) return Defs.B_H_PRED;
			if (above.yMode ==  Defs.V_PRED) return Defs.B_V_PRED;
			if (above.yMode == Defs.TM_PRED) return Defs.B_TM_PRED;
			if (above.yMode == Defs.B_PRED) return above.subBlockModes[i+12];
			assert(false);
		}
		return curr.subBlockModes[i-4];
	}

	private int getLeftBMode(MBInfo curr, MBInfo left, int i) {
		if ((i & 3) == 0) {
			if (left.yMode == Defs.DC_PRED) return Defs.B_DC_PRED;
			if (left.yMode ==  Defs.H_PRED) return Defs.B_H_PRED;
			if (left.yMode ==  Defs.V_PRED) return Defs.B_V_PRED;
			if (left.yMode == Defs.TM_PRED) return Defs.B_TM_PRED;
			if (left.yMode == Defs.B_PRED) return left.subBlockModes[i+3];
			assert(false);
		}
		return curr.subBlockModes[i-1];
	}

	private void decodeMBPred(MBInfo currMB, MBInfo above, MBInfo left) {
		// TODO: read segment here when applicable

		if (skipCoeffEnabled) {
			currMB.skipCoeff = p0Dec.decodeBit(skipCoeffProb) == 1;
		}

		// TODO handle inter prediction here
		currMB.yMode = p0Dec.getTreeVal(Defs.KF_Y_MODE_TREE, Defs.KF_Y_MODE_PROBS);
		//expectIntraMode(currMB.yMode);

		if (currMB.yMode == Defs.B_PRED) {
			for (int i = 0; i < 16; ++i) {
				int aMode = getAboveBMode(currMB, above, i);
				int lMode = getLeftBMode(currMB, left, i);

				//Verifier.demand(String.format("BMODE %d %d", aMode, lMode));

				currMB.subBlockModes[i] = p0Dec.getTreeVal(Defs.B_MODE_TREE, Defs.B_MODE_PROBS[aMode][lMode]);
				//expectSubblockMode(currMB.subBlockModes[i]);
			}
		}

		currMB.uvMode = p0Dec.getTreeVal(Defs.UV_MODE_TREE, Defs.KF_UV_MODE_PROBS);
		//expectIntraMode(currMB.uvMode);
	}

	public void decodeFrame(ByteBuffer frameData) throws VP8Exception {

		decodeFrameHeader(frameData);

		if (isKeyFrame) {
			// read colour and clamping info
			boolean color = p0Dec.getFlag();
			if (color) throw new VP8Exception("Unsupported colour space");
			p0Dec.getFlag(); // ignore clamp
		}

		if (p0Dec.getFlag()) {
			throw new VP8Exception("Segmentation not supported");
		}

		decodeLoopFilterHeader();
		decodePartionInfo(frameData);
		decodeDequantHeader();
		decodeReferenceHeader();

		if (isKeyFrame) {
			tokenProbs = new TokenProbs();
		}

		if (!refreshEntropy) {
			// Save the current entropy values
			savedProbs = new TokenProbs(tokenProbs);
		} else {
			savedProbs = null;
		}

		if (isKeyFrame) {
			yModeProbs[0] = 112;
			yModeProbs[1] = 86;
			yModeProbs[2] = 140;
			yModeProbs[3] = 37;

			uvModeProbs[0] = 162;
			uvModeProbs[1] = 101;
			uvModeProbs[2] = 204;
		}

		decodeEntropyHeader();

		for (int y = 0; y < mbRows; ++y) {
			for (int x = 0; x < mbCols; ++x) {
				MBInfo aboveMB = (y == 0) ? dummyMBInfo : mbInfos[(y-1)*mbCols + x];
				MBInfo leftMB  = (x == 0) ? dummyMBInfo : mbInfos[y*mbCols + x - 1];
				MBInfo currMB = mbInfos[y*mbCols + x];
				decodeMBPred(currMB, aboveMB, leftMB);
			}
		}

		TokenEnt[] aboveEnts = new TokenEnt[mbCols];
		for (int i = 0; i < mbCols; ++i) {
			aboveEnts[i] = new TokenEnt();
		}

		//Verifier.demand(String.format("Y1DC: %d", qf.y1DC));
		//Verifier.demand(String.format("Y1AC: %d", qf.y1AC));

		//Verifier.demand(String.format("UVDC: %d", qf.uvDC));
		//Verifier.demand(String.format("UVAC: %d", qf.uvAC));

		//Verifier.demand(String.format("Y2DC: %d", qf.y2DC));
		//Verifier.demand(String.format("Y2AC: %d", qf.y2AC));

		// Now decode DCT tokens
		for (int y = 0; y < mbRows; ++y) {
			TokenEnt leftEnt = new TokenEnt();
			for (int x = 0; x < mbCols; ++x) {
				MBInfo mbi = mbInfos[y*mbCols + x];
				MBTokens mbt = mbTokens[y*mbCols + x];

				decodeMBTokens(tokenDecoders[0], mbi, mbt, aboveEnts[x], leftEnt);

//				Verifier.demand("COEFFS:");
//				for (int i = 0; i < 25; ++i) {
//					for (int j = 0; j < 16; ++j) {
//						Verifier.demand(String.format("%d", mbt.t[i].v[j]));
//					}
//				}

			}
		}

		// Everything has been read, start reconstructing it
		for (int yMB = 0; yMB < mbRows; ++yMB) {
			for (int xMB = 0; xMB < mbCols; ++xMB) {
				predictIntra(xMB, yMB);

				addResidue(xMB, yMB);
			}
		}
	}

	private void addResidue(int xMB, int yMB) {
		MBInfo mbi = mbInfos[yMB*mbCols + xMB];
		MBTokens tokens = mbTokens[yMB*mbCols + xMB];

		short[] out = new short[16];

		if (mbi.yMode != Defs.B_PRED) {
			// Fixup DC
			fixupDC(tokens);

			// Add luma
			// Node, B_PRED adds luma residue as it predicts
			for (int i = 0; i < 16; ++i) {
				int x = xMB*16 + (i&3)*4;
				int y = yMB*16 + (i>>2)*4;

				Transform.deDCT(tokens.t[i].v, out, temp);
				Util.addResidueToPlane(x, y, out, mbCols*16, currentFrame.yPlane);
				testPred(x, y, 0, true);
			}
		}

		// Add chroma
		for (int i = 0; i < 4; ++i) {
			int x = xMB*8 + (i&1)*4;
			int y = yMB*8 + (i>>1)*4;

			Transform.deDCT(tokens.t[i+16].v, out, temp);
			Util.addResidueToPlane(x, y, out, mbCols*8, currentFrame.uPlane);

			Transform.deDCT(tokens.t[i+20].v, out, temp);
			Util.addResidueToPlane(x, y, out, mbCols*8, currentFrame.vPlane);
		}
		testPred(xMB*8, yMB*8, 1, true);
		testPred(xMB*8, yMB*8, 2, true);
	}

	private void fixupDC(MBTokens tokens) {
		short[] out = new short[16];
		Transform.deWHT(tokens.t[24].v, out, temp);

		for (int i = 0; i < 16; ++i) {
			tokens.t[i].v[0] = out[i];
		}
	}

	private void predictIntra(int xMB, int yMB) {
		MBInfo mbi = mbInfos[yMB*mbCols + xMB];
		short[] residue = new short[16];

		// Start with Y
		switch (mbi.yMode) {
		case Defs.DC_PRED:
			Pred.predictDC(xMB*16, yMB*16, true, currentFrame.width, currentFrame.yPlane);
			break;
		case Defs.V_PRED:
			Pred.predictV(xMB*16, yMB*16, true, currentFrame.width, currentFrame.yPlane);
			break;
		case Defs.H_PRED:
			Pred.predictH(xMB*16, yMB*16, true, currentFrame.width, currentFrame.yPlane);
			break;
		case Defs.TM_PRED:
			Pred.predictTM(xMB*16, yMB*16, true, currentFrame.width, currentFrame.yPlane);
			break;
		case Defs.B_PRED:
			// Need to predict then add residue to the frame one sub block at a time
			MBTokens mbt = mbTokens[yMB*mbCols + xMB];
			for (int i = 0; i < 16; ++i) {

				int x = xMB*16 + (i&3)*4;
				int y = yMB*16 + (i>>2)*4;

				Pred.predictBSubBlock(xMB*16, yMB*16, x, y, mbi.subBlockModes[i], mbCols*16, currentFrame.yPlane);

				Transform.deDCT(mbt.t[i].v, residue, temp);

				Util.addResidueToPlane(x, y, residue, mbCols*16, currentFrame.yPlane);
				testPred(x, y, 0, true);
			}
			break;
		default:
			assert(false);
		}

		// Check that this block is what we expected
		if (mbi.yMode != Defs.B_PRED) {
			testPred(xMB*16, yMB*16, 0, false);
		}

		// chroma
		switch (mbi.uvMode) {
		case Defs.DC_PRED:
			Pred.predictDC(xMB*8, yMB*8, false, currentFrame.width >> 1, currentFrame.uPlane);
			Pred.predictDC(xMB*8, yMB*8, false, currentFrame.width >> 1, currentFrame.vPlane);
			break;
		case Defs.V_PRED:
			Pred.predictV(xMB*8, yMB*8, false, currentFrame.width >> 1, currentFrame.uPlane);
			Pred.predictV(xMB*8, yMB*8, false, currentFrame.width >> 1, currentFrame.vPlane);
			break;
		case Defs.H_PRED:
			Pred.predictH(xMB*8, yMB*8, false, currentFrame.width >> 1, currentFrame.uPlane);
			Pred.predictH(xMB*8, yMB*8, false, currentFrame.width >> 1, currentFrame.vPlane);
			break;
		case Defs.TM_PRED:
			Pred.predictTM(xMB*8, yMB*8, false, currentFrame.width >> 1, currentFrame.uPlane);
			Pred.predictTM(xMB*8, yMB*8, false, currentFrame.width >> 1, currentFrame.vPlane);
			break;
		default:
			assert(false);
		}
		testPred(xMB*8, yMB*8, 1, false);
		testPred(xMB*8, yMB*8, 2, false);

	}

	public void testPred(int x, int y, int planeInd, boolean withResidue) {
		if (!testPred) {
			return;
		}

		int blockWidth = 0;
		int stride = 0;
		YUVImage refImage = withResidue ? finalRef : predRef;

		byte[] refPlane = null;
		byte[] plane = null;

		switch (planeInd) {
		case 0:
			refPlane = refImage.yPlane;
			plane = currentFrame.yPlane;
			blockWidth = 16;
			stride = predRef.width;
			break;
		case 1:
			refPlane = refImage.uPlane;
			plane = currentFrame.uPlane;
			blockWidth = 8;
			stride = predRef.width >> 1;
			break;
		case 2:
			refPlane = refImage.vPlane;
			plane = currentFrame.vPlane;
			blockWidth = 8;
			stride = predRef.width >> 1;
			break;
		case 3:
			refPlane = refImage.yPlane;
			plane = currentFrame.yPlane;
			blockWidth = 4;
			stride = predRef.width;
			break;
		default: assert(false);
		}

		StringBuilder refBytes = new StringBuilder();
		StringBuilder ourBytes = new StringBuilder();
		boolean failure = false;
		int firstX = 0;
		int firstY = 0;

		for (int j = 0; j < blockWidth; ++j) {
			for (int i = 0; i < blockWidth; ++i) {
				int rx = x + i;
				int ry = y + j;
				int refVal = Util.getUByte(refPlane, ry*stride+rx);
				int val = Util.getUByte(plane, ry*stride+rx);
				refBytes.append(String.format(" %3d,", refVal));
				ourBytes.append(String.format(" %3d,", val));
				if (val != refVal) {
					if (!failure) {
						firstX = i;
						firstY = j;
					}
					failure = true;
				}
			}
			refBytes.append("\n");
			ourBytes.append("\n");
		}

		if (failure) {
			System.out.printf("Failure at Block (%d, %d) first (%d, %d) plane: %d\n", x, y, firstX, firstY, planeInd);
			System.out.printf("Expected:\n%s", refBytes.toString());
			System.out.printf("Got:\n%s", ourBytes.toString());
			assert(false);
		}
	}

	private void decodeMBTokens(BoolDecoder dec, MBInfo mbi, MBTokens mbt, TokenEnt above, TokenEnt left) {
		boolean hasY2 = mbi.yMode != Defs.B_PRED; // TODO SPLITMV
		if (mbi.skipCoeff) {
			left.clear(hasY2);
			above.clear(hasY2);
			return;
		}

		if (mbi.yMode != Defs.B_PRED) {
			// Decode Y2
			decodeTokens(dec, 1, 24, mbt, above, left, 0, qf.y2DC, qf.y2AC);
		}
		// Decode the Y blocks
		for (int i = 0; i < 16; ++i) {
			decodeTokens(dec, (hasY2?0:3), i, mbt, above, left, (hasY2?1:0), qf.y1DC, qf.y1AC);
		}
		for (int i = 0; i < 8; ++i) {
			decodeTokens(dec, 2, i+16, mbt, above, left, 0, qf.uvDC, qf.uvAC);
		}
	}

	private void decodeTokens(BoolDecoder dec, int type, int blockInd, MBTokens mbt, TokenEnt above, TokenEnt left, int startCoeff,
	                          int dcDQ, int acDQ) {

		int c = above.v[Defs.BLOCK_TO_ABOVE_ENT[blockInd]] + left.v[Defs.BLOCK_TO_LEFT_ENT[blockInd]];
		boolean lastTokZero = false;
		boolean hasVal = false;

		for (int i = startCoeff; i < 16; ++i) {
			int b = Defs.BANDS[i];
			int[] probs = tokenProbs.getProbs(type, b, c);

			int dct = dec.getTreeVal(Defs.TOKEN_TREE, probs, lastTokZero ? 2 : 0);

			if (dct == Defs.DCT_EOB) {
				 break;
			}

			int val = 0;
			hasVal = hasVal || dct > Defs.DCT_0;
			lastTokZero = (dct == Defs.DCT_0);
			if (dct == Defs.DCT_0) { c = 0; }
			else if (dct == Defs.DCT_1) { c = 1; }
			else { c = 2; }
			switch (dct) {
			case Defs.DCT_0:
				val = 0;
				break;
			case Defs.DCT_1:
				//Verifier.demand("DCT_1");
				val = 1;
				break;
			case Defs.DCT_2:
				//Verifier.demand("DCT_2");
				val = 2;
				break;
			case Defs.DCT_3:
				//Verifier.demand("DCT_3");
				val = 3;
				break;
			case Defs.DCT_4:
				//Verifier.demand("DCT_4");
				val = 4;
				break;
			case Defs.DCT_CAT1:
				//Verifier.demand("DCT_CAT1");
				val = 5 + dec.getIntWithProbs(Defs.CAT1_PROBS, 1);
				break;
			case Defs.DCT_CAT2:
				//Verifier.demand("DCT_CAT2");
				val = 7 + dec.getIntWithProbs(Defs.CAT2_PROBS, 2);
				break;
			case Defs.DCT_CAT3:
				//Verifier.demand("DCT_CAT3");
				val = 11 + dec.getIntWithProbs(Defs.CAT3_PROBS, 3);
				break;
			case Defs.DCT_CAT4:
				//Verifier.demand("DCT_CAT4");
				val = 19 + dec.getIntWithProbs(Defs.CAT4_PROBS, 4);
				break;
			case Defs.DCT_CAT5:
				//Verifier.demand("DCT_CAT5");
				val = 35 + dec.getIntWithProbs(Defs.CAT5_PROBS, 5);
				break;
			case Defs.DCT_CAT6:
				//Verifier.demand("DCT_CAT6");
				val = 67 + dec.getIntWithProbs(Defs.CAT6_PROBS, 11);
				break;
			}

			if (val != 0) {
				//Verifier.demand(String.format("VAL: %d", val));
				if (dec.getFlag()) { val = -val; }
			}

			int dqf = (i == 0) ? dcDQ : acDQ;
			int coeffInd = Defs.ZIGZAG[i];
			mbt.t[blockInd].v[coeffInd] = (short)(val * dqf);
		}

		int entVal = hasVal ? 1 : 0;
		above.v[Defs.BLOCK_TO_ABOVE_ENT[blockInd]] = entVal;
		left.v[Defs.BLOCK_TO_LEFT_ENT[blockInd]] = entVal;
		mbt.hasValues = hasVal;

	}

	private void keyFrameClearState() {
		for (int i = 0; i < 4; ++i) {
			refFilterDelta[i] = 0;
			modeFilterDelta[i] = 0;
		}
	}

	/*
	private void expectIntraMode(int mode) {
		switch (mode) {
		case DC_PRED:
			Verifier.demand("DC_PRED"); return;
		case H_PRED:
			Verifier.demand("H_PRED"); return;
		case V_PRED:
			Verifier.demand("V_PRED"); return;
		case TM_PRED:
			Verifier.demand("TM_PRED"); return;
		case B_PRED:
			Verifier.demand("B_PRED"); return;
		}
		assert(false);
	} */
	/*
	private void expectSubblockMode(int mode) {
		switch (mode) {

		case B_DC_PRED:
			Verifier.demand("B_DC_PRED"); return;
		case B_V_PRED:
			Verifier.demand("B_V_PRED"); return;
		case B_H_PRED:
			Verifier.demand("B_H_PRED"); return;
		case B_TM_PRED:
			Verifier.demand("B_TM_PRED"); return;
		case B_LD_PRED:
			Verifier.demand("B_LD_PRED"); return;
		case B_RD_PRED:
			Verifier.demand("B_RD_PRED"); return;
		case B_VR_PRED:
			Verifier.demand("B_VR_PRED"); return;
		case B_VL_PRED:
			Verifier.demand("B_VL_PRED"); return;
		case B_HD_PRED:
			Verifier.demand("B_HD_PRED"); return;
		case B_HU_PRED:
			Verifier.demand("B_HU_PRED"); return;
		}
		assert(false);
	} */

}
