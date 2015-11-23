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

import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.nio.ByteBuffer;

/**
 * A simple VP8 Encoder, use is to pass a buffered image to encodeFrame() and use the returned
 * ByteBuffer as needed. This class is mostly likely to be used in conjunction with a video container encoder
 * @author matt.chudleigh
 *
 */
public class Encoder {

	private BoolEncoder headerEnc;
	private BoolEncoder resEnc;

	private YUVImage predImage;
	private YUVImage encodingImage;
	private YUVImage lastImage;

	private int mbCols;
	private int mbRows;

	private int y1DC;
	private int y1AC;

	private int y2DC;
	private int y2AC;

	private int uvDC;
	private int uvAC;

	// Some inter frame probs
	private int probIntraPred;
	private int probLastFrame;

	private TokenProbs tokenProbs;

	private boolean keyFrame;

	private short[] temp = new short[16];
	private short[] coeffs = new short[16];
	private short[] predictRes = new short[16];
	private short[] encodedRes = new short[16];
	private short[] residue = new short[16];
	private short[] y2Coeffs = new short[16];

	private static class EntTracker {
		int[] v = new int[9];
	}

	EntTracker[] aboveEnts;
	EntTracker leftEnt;

	PrintWriter encLogger;

	// Debug
//	private long predNanos;
//	private long resNanos;
//	private long transNanos;
//	private long encNanos;
//	private long detransNanos;
//	private long addbackNanos;

	public Encoder() {
	}

	public ByteBuffer encodeFrame(BufferedImage img, boolean forceKeyFrame) {

		keyFrame = (lastImage == null || forceKeyFrame);

//		long start = System.nanoTime();

		mbCols = (img.getWidth()  + 15) >> 4;
		mbRows = (img.getHeight() + 15) >> 4;

		// The image to use for intra-frame prediction
		if ( predImage == null ||
		     predImage.width != mbCols * 16 ||
		     predImage.height != mbRows * 16) {
			predImage = new YUVImage(mbCols*16, mbRows*16);
			encodingImage = new YUVImage(mbCols*16, mbRows*16);
		}

		encodingImage.fillFromBuffered(img);

//		long convImage = System.nanoTime();
//		long convDur = (convImage - start) / 1000000;

		// Initialize the entropy tracker (these are used in residue encoding)
		aboveEnts = new EntTracker[mbCols];
		for (int i = 0; i < mbCols; ++i) {
			aboveEnts[i] = new EntTracker();
		}

		if (keyFrame) {
			tokenProbs = new TokenProbs();
		}

		// Encoder for header and residue
		headerEnc = new BoolEncoder();
		resEnc = new BoolEncoder();

		if (keyFrame) {
			// Color space and clamping
			headerEnc.encodeFlag(false);
			headerEnc.encodeFlag(false);
		}

		// No segmentation
		headerEnc.encodeFlag(false);

		// Simple filter
		headerEnc.encodeFlag(false);
		headerEnc.encodeLitUInt(0, 6); // Filter level
		headerEnc.encodeLitUInt(0, 3); // sharpness

		// LF adjust
		headerEnc.encodeFlag(false);

		headerEnc.encodeLitUInt(0, 2); // 1 partition

		// Quantifier indices
		headerEnc.encodeLitUInt(0, 7); // Highest fidelity

		// These will be properly read from the table one day
		y1DC = 4;
		y1AC = 4;

		uvDC = 4;
		uvAC = 4;

		y2DC = 8;
		y2AC = 8;

		// Do not over ride any quantifiers for now
		headerEnc.encodeFlag(false);
		headerEnc.encodeFlag(false);
		headerEnc.encodeFlag(false);
		headerEnc.encodeFlag(false);
		headerEnc.encodeFlag(false);

		if (!keyFrame) {
			headerEnc.encodeFlag(true); // refresh golden
			headerEnc.encodeFlag(true); // refresh alt ref

			headerEnc.encodeFlag(false); // sign bias golden
			headerEnc.encodeFlag(false); // sign bias alt ref
			headerEnc.encodeFlag(true); // refresh entropy
			headerEnc.encodeFlag(true); // refresh last
		} else {
			// Don't refresh entropy
			headerEnc.encodeFlag(true);
		}


		tokenProbs.writeOutUpdateTable(headerEnc);

		// Disable skipping macroblock coeffs
		headerEnc.encodeFlag(false);

		if (!keyFrame) {
			probIntraPred = 1; // Always inter predicted
			headerEnc.encodeLitUInt(probIntraPred, 8);

			probLastFrame = 255; // Always last frame
			headerEnc.encodeLitUInt(probLastFrame, 8);

			headerEnc.encodeLitUInt(128, 8); // Never golden or altref so this doesn't matter

			headerEnc.encodeFlag(false); // update intra 16x16 probs
			headerEnc.encodeFlag(false); // upate intra chroma probs

			// MV prob updates

			for (int i = 0; i < 2; ++i) {
				for (int j = 0; j < 19; ++j) {
					headerEnc.encodeBoolean(false, Defs.MV_ENTROPY_UPATE_PROBS[i][j]);
				}
			}
		}

		// Now start encoding the macroblocks
		for (int j = 0; j < mbRows; ++j) {
			for (int  i= 0; i < mbCols; ++i) {
				if (keyFrame) {
					encodeKeyMBHeader(i, j);
				} else {
					encodeInterMBHeader(i, j);
				}
			}
		}

//		long writeHeader = System.nanoTime();
//		long headerDur = (writeHeader - convImage) / 1000000;

		//EncLogger.log("Y1DC: 4");
		//EncLogger.log("Y1AC: 4");

		//EncLogger.log("UVDC: 4");
		//EncLogger.log("UVAC: 4");

		//EncLogger.log("Y2DC: 8");
		//EncLogger.log("Y2AC: 8");

//		predNanos = resNanos = transNanos = encNanos = detransNanos = addbackNanos = 0;


		for (int j = 0; j < mbRows; ++j) {

			leftEnt = new EntTracker();

			for (int  i= 0; i < mbCols; ++i) {
				if (keyFrame) {
					predAndEncodeKeyMB(i, j);
				} else {
					predAndEncodeInterMB(i, j);
				}
			}
		}

//		System.out.println(String.format("p: %d, r: %d, t: %d, e: %d, d: %d, a: %d",
//				predNanos / 1000000,
//				resNanos / 1000000,
//				transNanos / 1000000,
//				encNanos / 1000000,
//				detransNanos / 1000000,
//				addbackNanos / 1000000));

//		long generateRes = System.nanoTime();
//		long resDur = (generateRes  - writeHeader) / 1000000;


		ByteBuffer headerStream = headerEnc.getData();
		ByteBuffer residueStream = resEnc.getData();

		ByteBuffer ret = ByteBuffer.allocate(10 + headerStream.capacity() + residueStream.capacity());

		assert(headerStream.capacity() < (1 << 19));

		int headerTemp = 0;
		headerTemp += (keyFrame ? 0 : 1);
		headerTemp += 3 << 1; // No filter (version 3)
		headerTemp += 1 << 4; // show flag
		headerTemp += headerStream.capacity() << 5;

		ret.put((byte)(headerTemp & 0xff));
		ret.put((byte)((headerTemp >> 8) & 0xff));
		ret.put((byte)((headerTemp >> 16) & 0xff));

		if (keyFrame) {

			ret.put((byte)0x9d);
			ret.put((byte)0x01);
			ret.put((byte)0x2a);

			int width = img.getWidth();
			ret.put((byte)(width & 0xff));
			ret.put((byte)((width >> 8) & 0xff));

			int height = img.getHeight();
			ret.put((byte)(height & 0xff));
			ret.put((byte)((height >> 8) & 0xff));
		}

		ret.put(headerStream);
		ret.put(residueStream);

//		long writeout = System.nanoTime();
//		long writeoutDur = (writeout - generateRes) / 1000000;
//
//		System.out.println(String.format("Conv: %d,  Head: %d, Res: %d, Write: %d", convDur, headerDur, resDur, writeoutDur ));

		lastImage = predImage;

		ret.flip();
		return ret;
	}

	private void encodeKeyMBHeader(int col, int row) {
		// For now we encode all macro blocks as all DC
		// Y is B_PRED with all 16 being DC
		// chroma is full block DC

		// Y_MODE
		headerEnc.encodeBoolean(false, 145); // B_PRED
		// Now 16 sub blocks

		//EncLogger.log("B_PRED");
		for (int i = 0; i < 16; ++i)
		{
			//EncLogger.log("BMODE 0 0");
			headerEnc.encodeBoolean(false, 231); // B_DC_PRED
			//EncLogger.log("B_DC_PRED");
		}

		// Now Chroma
		headerEnc.encodeBoolean(false, 142); // DC_PRED
		//EncLogger.log("DC_PRED");
	}

	private void encodeInterMBHeader(int col, int row) {
		headerEnc.encodeBoolean(true, probIntraPred); // inter frame prediction
		headerEnc.encodeBoolean(false, probLastFrame); // predict from last frame

		// This is the MV mode tree encoding, with all 0 MVs this encodes to a single fixed
		// boolean, but could be quite complex with non-zero MVs

		// The probability to use to encode our tree here is slightly contextual, we only want 0 MVs, but
		// the context still changes, see the spec
		int prob = 0;
		if (col == 0 && row == 0) {
			prob = 7;
		} else if (col == 0 || row == 0) {
			prob = 135;
		} else {
			prob = 234;
		}

		headerEnc.encodeBoolean(false, prob);
	}

	private void predAndEncodeKeyMB(int col, int row) {
		// Use the intra prediction code to fill in the prediction buffer

		int x = col * 16;
		int y = row * 16;
		for (int j = 0; j < 4; ++j) {
			for (int  i= 0; i < 4; ++i) {
				int subX = x+i*4;
				int subY = y+j*4;

				Pred.predictBSubBlock(x, y, subX, subY, Defs.B_DC_PRED, predImage.width, predImage.yPlane);

				// Now work out the residue
				setResidue(subX, subY, encodingImage.yPlane, predImage.yPlane, predImage.width);

				Transform.DCT(residue, coeffs, temp);

				encodeResidue(3, j*4+i, 0, leftEnt, aboveEnts[col], y1DC, y1AC, coeffs);

				// Now add the equivalent residue back to the prediction buffer
				Transform.deDCT(encodedRes, predictRes, temp);

				Util.addResidueToPlane(subX, subY, predictRes, predImage.width, predImage.yPlane);
			}
		}

		// On to chroma
		int chX = x >> 1;
		int chY = y >> 1;
		int chromaStride = (predImage.width+1) >> 1;
		Pred.predictDC(chX, chY, false, chromaStride, predImage.uPlane);
		Pred.predictDC(chX, chY, false, chromaStride, predImage.vPlane);

		encodeChroma(col, row, 16, predImage.uPlane, encodingImage.uPlane, predImage.uPlane, chromaStride);
		encodeChroma(col, row, 20, predImage.vPlane, encodingImage.vPlane, predImage.vPlane, chromaStride);

	}

	private void encodeChroma(int col, int row, int blockOffset, byte[] predPlane, byte[] encodingPlane, byte[] writeBackPlane, int stride) {

		int chX = col << 3;
		int chY = row << 3;
		for (int j = 0; j < 2; ++j) {
			for (int  i= 0; i < 2; ++i) {
				int subX = chX+i*4;
				int subY = chY+j*4;

				setResidue(subX, subY, encodingPlane, predPlane, stride);
				Transform.DCT(residue, coeffs, temp);

				encodeResidue(2, j*2+i+blockOffset, 0, leftEnt, aboveEnts[col], uvDC, uvAC, coeffs);
				//savedCoeffs[nextCoeff++] = encodedRes;

				// Now add the equivalent residue back to the prediction buffer
				Transform.deDCT(encodedRes, predictRes, temp);
				Util.addResidueToPlane(subX, subY, predictRes, stride, writeBackPlane);
			}
		}
	}

	private void predAndEncodeInterMB(int col, int row) {
		// This is zero MV predicted macro block
		int x = col << 4;
		int y = row << 4;
		int chX = col << 3;
		int chY = row << 3;
		int yStride = lastImage.width;
		int chStride = (yStride + 1) >> 1;

		// Copy the last frame into the prediction image
		for (int j = 0; j < 16; ++j) {
			int ry = y + j;
			for (int i = 0; i < 16; ++i) {
				int rx = x + i;
				predImage.yPlane[ry*yStride + rx] = lastImage.yPlane[ry*yStride + rx];
			}
		}
		for (int j = 0; j < 8; ++j) {
			int ry = chY + j;
			for (int i = 0; i < 8; ++i) {
				int rx = chX + i;
				predImage.uPlane[ry*chStride + rx] = lastImage.uPlane[ry*chStride + rx];
				predImage.vPlane[ry*chStride + rx] = lastImage.vPlane[ry*chStride + rx];
			}
		}

		// Now encode the Y2 sub block
		setY2Coeffs(x, y, encodingImage.yPlane, predImage.yPlane, yStride);
		Transform.WHT(y2Coeffs, coeffs, temp);

		encodeResidue(1, 24, 0, leftEnt, aboveEnts[col], y2DC, y2AC, coeffs);

		// Transform the WHT terms back to pick up any possible rounding problems
		Transform.deWHT(encodedRes, y2Coeffs, temp);

		// Now encode the normal Y blocks
		for (int j = 0; j < 4; ++j) {
			for (int  i= 0; i < 4; ++i) {
				int subX = x+i*4;
				int subY = y+j*4;

				// Now work out the residue
				setResidue(subX, subY, encodingImage.yPlane, predImage.yPlane, predImage.width);

				Transform.DCT(residue, coeffs, temp);

				encodeResidue(0, j*4+i, 1, leftEnt, aboveEnts[col], y1DC, y1AC, coeffs);

				encodedRes[0] = y2Coeffs[j*4+i];

				// Now add the equivalent residue back to the prediction buffer
				Transform.deDCT(encodedRes, predictRes, temp);

				Util.addResidueToPlane(subX, subY, predictRes, predImage.width, predImage.yPlane);
			}
		}
		// And now chroma
		encodeChroma(col, row, 16, predImage.uPlane, encodingImage.uPlane, predImage.uPlane, chStride);
		encodeChroma(col, row, 20, predImage.vPlane, encodingImage.vPlane, predImage.vPlane, chStride);

	}

	// Sets the residue private value to the DC values of the 16 Y sub blocks (the input of the Y2 WHT)
	private void setY2Coeffs(int subX, int subY, byte[] encPlane, byte[] predPlane, int stride) {
		for (int j = 0; j < 4; ++j) {
			for (int i = 0; i < 4; ++i) {
				setResidue(subX+4*i, subY+4*j, encPlane, predPlane, stride);
				y2Coeffs[4*j+i] = Transform.DCTVal0(residue);
			}
		}
	}

	private void setResidue(int subX, int subY, byte[] encPlane, byte[] predPlane, int stride) {
		for (int j = 0; j < 4; ++j) {
			for (int  i= 0; i < 4; ++i) {
				int rx = subX + i;
				int ry = subY + j;
				int encVal = Util.getUByte(encPlane, ry*stride + rx);
				int predVal = Util.getUByte(predPlane, ry*stride + rx);
				residue[j*4+i] = (short)(encVal - predVal);
			}
		}
	}

	private void encodeResidue(int type, int blockInd, int firstCoeff, EntTracker left, EntTracker above, int dcQF, int acQF, short[] residue) {
		int c = above.v[Defs.BLOCK_TO_ABOVE_ENT[blockInd]] + left.v[Defs.BLOCK_TO_LEFT_ENT[blockInd]];

		boolean lastTokenZero = false;
		boolean hasVal = false;

		// Reset the encoded residue (we may not visit all values)
		for (int i = 0; i < 16; ++i) {
			encodedRes[i] = 0;
		}

		int lastCoeff = -1;
		for (int i = firstCoeff; i < 16; ++i) {
			int val = residue[Defs.ZIGZAG[i]] / (i == 0 ? dcQF : acQF);
			if (val != 0) {
				lastCoeff = i;
			}
		}

		for (int i = firstCoeff; i < 16; ++i) {
			int b = Defs.BANDS[i];
			int[] probs = tokenProbs.getProbs(type, b, c);

			if (i > lastCoeff) {
				// Encode an eob token
				resEnc.encodeBoolean(false,probs[0]);
				break;
			}
			// val is the value to be encoded
			int val = residue[Defs.ZIGZAG[i]] / (i == 0 ? dcQF : acQF);
			encodedRes[Defs.ZIGZAG[i]] = (short)(val * (i == 0 ? dcQF : acQF));

			encodeCoeff(val, probs, lastTokenZero);

			lastTokenZero = (val == 0);
			if (val == 0) { c = 0; }
			else if (val == 1 || val == -1) { c = 1; }
			else { c = 2; }

			if (val != 0) { hasVal = true; }
		}

		int entVal = hasVal ? 1 : 0;
		above.v[Defs.BLOCK_TO_ABOVE_ENT[blockInd]] = entVal;
		left.v[Defs.BLOCK_TO_LEFT_ENT[blockInd]] = entVal;
	}

	// Hand encode the token tree for now...
	private void encodeCoeff(int val, int[] probs, boolean lastTokenZero) {
		boolean isNeg = val < 0;
		if (isNeg) { val = -val; }

		if (val > 2048) val = 2048;

		if (!lastTokenZero) {
			// Bypass the EOB branch
			resEnc.encodeBoolean(true, probs[0]);
		}

		if (val == 0) {
			resEnc.encodeTree(Defs.DCT_0_VAL, probs, Defs.TOKEN_TREE, 1, 2);
			return;
		}
		if (val == 1) {
			resEnc.encodeTree(Defs.DCT_1_VAL, probs, Defs.TOKEN_TREE, 1, 2);
			//EncLogger.log("DCT_1");
			//EncLogger.log(String.format("VAL: %d", val));
			resEnc.encodeFlag(isNeg);
			return;
		}

		if (val == 2) {
			resEnc.encodeTree(Defs.DCT_2_VAL, probs, Defs.TOKEN_TREE, 1, 2);
			//EncLogger.log("DCT_2");
			//EncLogger.log(String.format("VAL: %d", val));
			resEnc.encodeFlag(isNeg);
			return;
		}

		if (val == 3) {
			resEnc.encodeTree(Defs.DCT_3_VAL, probs, Defs.TOKEN_TREE, 1, 2);
			//EncLogger.log("DCT_3");
			//EncLogger.log(String.format("VAL: %d", val));
			resEnc.encodeFlag(isNeg);
			return;
		}

		if (val == 4) {
			resEnc.encodeTree(Defs.DCT_4_VAL, probs, Defs.TOKEN_TREE, 1, 2);
			//EncLogger.log("DCT_4");
			//EncLogger.log(String.format("VAL: %d", val));
			resEnc.encodeFlag(isNeg);
			return;
		}

		// Into the variable types
		if (val <= 6) { // cat1
			resEnc.encodeTree(Defs.DCT_CAT1_VAL, probs, Defs.TOKEN_TREE, 1, 2);
			//EncLogger.log("DCT_CAT1");
			resEnc.encodeLitWithProbs(val - 5, 1, Defs.CAT1_PROBS);
			//EncLogger.log(String.format("VAL: %d", val));
			resEnc.encodeFlag(isNeg);
			return;
		}
		if (val <= 10) { // cat2
			resEnc.encodeTree(Defs.DCT_CAT2_VAL, probs, Defs.TOKEN_TREE, 1, 2);
			//EncLogger.log("DCT_CAT2");
			resEnc.encodeLitWithProbs(val - 7, 2, Defs.CAT2_PROBS);
			//EncLogger.log(String.format("VAL: %d", val));
			resEnc.encodeFlag(isNeg);
			return;
		}
		if (val <= 18) { // cat3
			resEnc.encodeTree(Defs.DCT_CAT3_VAL, probs, Defs.TOKEN_TREE, 1, 2);
			//EncLogger.log("DCT_CAT3");
			resEnc.encodeLitWithProbs(val - 11, 3, Defs.CAT3_PROBS);
			//EncLogger.log(String.format("VAL: %d", val));
			resEnc.encodeFlag(isNeg);
			return;
		}
		if (val <= 34) { // cat4
			resEnc.encodeTree(Defs.DCT_CAT4_VAL, probs, Defs.TOKEN_TREE, 1, 2);
			//EncLogger.log("DCT_CAT4");
			resEnc.encodeLitWithProbs(val - 19, 4, Defs.CAT4_PROBS);
			//EncLogger.log(String.format("VAL: %d", val));
			resEnc.encodeFlag(isNeg);
			return;
		}
		if (val <= 66) { // cat5
			resEnc.encodeTree(Defs.DCT_CAT5_VAL, probs, Defs.TOKEN_TREE, 1, 2);
			//EncLogger.log("DCT_CAT5");
			resEnc.encodeLitWithProbs(val - 35, 5, Defs.CAT5_PROBS);
			//EncLogger.log(String.format("VAL: %d", val));
			resEnc.encodeFlag(isNeg);
			return;
		}
		if (val <= 2048) { // cat6
			resEnc.encodeTree(Defs.DCT_CAT6_VAL, probs, Defs.TOKEN_TREE, 1, 2);
			//EncLogger.log("DCT_CAT6");
			resEnc.encodeLitWithProbs(val - 67, 11, Defs.CAT6_PROBS);
			//EncLogger.log(String.format("VAL: %d", val));
			resEnc.encodeFlag(isNeg);
			return;
		}
		assert(false);
	}

	// debug
	public void showLastFrame() {
		lastImage.show(1, true, "");
	}
}
