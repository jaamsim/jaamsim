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


public class Pred {

	public static void predictDC(int x, int y, boolean yBlock, int stride, byte[] plane) {
		int val = 0;
		int blockSize = yBlock ? 16 : 8;
		int shift = yBlock ? 5 : 4;
		if (x == 0 && y == 0) {
			val = 128;
		} else if (x == 0) {
			for (int i = 0; i < blockSize; ++i) {
				val += Util.getUByte(plane,(y-1)*stride + x + i);
			}
			val += blockSize >> 1; // average
			val = val >> (shift - 1);
		} else if (y == 0) {
			for (int i = 0; i < blockSize; ++i) {
				val += Util.getUByte(plane,(y+i)*stride + x -1);
			}
			val += blockSize >> 1;
			val = val >> (shift - 1);
		} else {
			for (int i = 0; i < blockSize; ++i) {
				val += Util.getUByte(plane,(y-1)*stride + x + i);
				val += Util.getUByte(plane,(y+i)*stride + x -1);
			}
			val += blockSize;
			val = val >> shift;
		}

		for (int j = 0; j < blockSize; ++j) {
			for (int i = 0; i < blockSize; ++i) {
				plane[(y+j)*stride+x+i] = (byte)val;
			}
		}
	}

	public static void predictV(int x, int y, boolean yBlock, int stride, byte[] plane) {
		int blockSize = yBlock ? 16 : 8;

		if (y == 0) {
			for (int j = 0; j < blockSize; ++j) {
				for (int i = 0; i < blockSize; ++i) {
					plane[(y+j)*stride+x+i] = (byte)127;
				}
			}
			return;
		}
		for (int i = 0; i < blockSize; ++i) {
			byte val = (byte)Util.getUByte(plane,(y-1)*stride+x+i);

			for (int j = 0; j < blockSize; ++j) {
				plane[(y+j)*stride+x+i] = val;
			}
		}
	}

	public static void predictH(int x, int y, boolean yBlock, int stride, byte[] plane) {
		int blockSize = yBlock ? 16 : 8;

		if (x == 0) {
			for (int j = 0; j < blockSize; ++j) {
				for (int i = 0; i < blockSize; ++i) {
					plane[(y+j)*stride+x+i] = (byte)129;
				}
			}
			return;
		}
		for (int j = 0; j < blockSize; ++j) {
			byte val = (byte)Util.getUByte(plane,(y+j)*stride+x-1);

			for (int i = 0; i < blockSize; ++i) {
				plane[(y+j)*stride+x+i] = val;
			}
		}
	}

	public static void predictTM(int x, int y, boolean yBlock, int stride, byte[] plane) {
		int blockSize = yBlock ? 16 : 8;

		int p = 0;
		if (x == 0 && y == 0) {
			p = 128;
		} else if (x == 0) {
			p = 129;
		} else if (y == 0) {
			p = 127;
		} else {
			p = Util.getUByte(plane,(y-1)*stride+x-1);
		}
		int[] l = new int[blockSize];
		int[] a = new int[blockSize];
		for (int i = 0; i < blockSize; ++i) {
			l[i] = (x==0)?129:Util.getUByte(plane,(y+i)*stride+x-1);
			a[i] = (y==0)?127:Util.getUByte(plane,(y-1)*stride+x+i);
		}

		for (int j = 0; j < blockSize; ++j) {
			for (int i = 0; i < blockSize; ++i) {
				int val = Util.clamp255(a[i] + l[j] - p);
				plane[(y+j)*stride+x+i] = (byte)val;
			}
		}
	}

	public static void predictBSubBlock(int blockX, int blockY, int subX, int subY, int subMode, int stride, byte[] plane) {
		int[] e = new int[13];

		// left
		if (subX == 0) {
			e[0] = e[1] = e[2] = e[3] = 129;
		} else {
			e[0] = Util.getUByte(plane,(subY+3)*stride + subX - 1);
			e[1] = Util.getUByte(plane,(subY+2)*stride + subX - 1);
			e[2] = Util.getUByte(plane,(subY+1)*stride + subX - 1);
			e[3] = Util.getUByte(plane,(subY+0)*stride + subX - 1);
		}

		// Top
		if (subY == 0) {
			e[5] = e[6] = e[7] = e[8] = e[9] = e[10] = e[11] = e[12] = 127;
		} else {
			// Right top
			e[5] = Util.getUByte(plane,(subY-1)*stride + subX + 0);
			e[6] = Util.getUByte(plane,(subY-1)*stride + subX + 1);
			e[7] = Util.getUByte(plane,(subY-1)*stride + subX + 2);
			e[8] = Util.getUByte(plane,(subY-1)*stride + subX + 3);

			if (subX + 4 == stride) {
				if (blockY == 0) {
					e[ 9] = e[10] = e[11] = e[12] = 127;
				} else {
					e[ 9] = e[10] = e[11] = e[12] = Util.getUByte(plane,(blockY-1)*stride + subX+3); // equal to the pixel above the right edge
				}

			} else if (subX - blockX == 12) { // We are in the 4th column,
				if (blockY == 0) {
					e[ 9] = e[10] = e[11] = e[12] = 127;
				} else {
					e[ 9] = Util.getUByte(plane,(blockY-1)*stride + subX + 4);
					e[10] = Util.getUByte(plane,(blockY-1)*stride + subX + 5);
					e[11] = Util.getUByte(plane,(blockY-1)*stride + subX + 6);
					e[12] = Util.getUByte(plane,(blockY-1)*stride + subX + 7);
				}
			} else {
				e[ 9] = Util.getUByte(plane,(subY-1)*stride + subX + 4);
				e[10] = Util.getUByte(plane,(subY-1)*stride + subX + 5);
				e[11] = Util.getUByte(plane,(subY-1)*stride + subX + 6);
				e[12] = Util.getUByte(plane,(subY-1)*stride + subX + 7);
			}
		}
		if (subX == 0 && subY == 0) {
			e[4] = 128;
		} else if (subX == 0) {
			e[4] = 129;
		} else if (subY == 0) {
			e[4] = 127;
		} else {
			e[4] = Util.getUByte(plane,(subY-1)*stride + subX - 1);
		}

		switch(subMode) {
		case Defs.B_DC_PRED:
			predictBDC(subX, subY, e, stride, plane);
			break;
		case Defs.B_V_PRED:
			predictBVE(subX, subY, e, stride, plane);
			break;
		case Defs.B_H_PRED:
			predictBHE(subX, subY, e, stride, plane);
			break;
		case Defs.B_TM_PRED:
			predictBTM(subX, subY, e, stride, plane);
			break;
		case Defs.B_LD_PRED:
			predictBLD(subX, subY, e, stride, plane);
			break;
		case Defs.B_RD_PRED:
			predictBRD(subX, subY, e, stride, plane);
			break;
		case Defs.B_VR_PRED:
			predictBVR(subX, subY, e, stride, plane);
			break;
		case Defs.B_VL_PRED:
			predictBVL(subX, subY, e, stride, plane);
			break;
		case Defs.B_HD_PRED:
			predictBHD(subX, subY, e, stride, plane);
			break;
		case Defs.B_HU_PRED:
			predictBHU(subX, subY, e, stride, plane);
			break;
		default:
			assert(false);
		}
	}

	private static void predictBDC(int x, int y, int[] e, int stride, byte[] plane) {
		int val = 4;
		for (int i = 0; i < 4; ++i) {
			val += e[i] + e[5+i]; // edge left and above
		}
		val = val >> 3;
		for (int j = 0; j < 4; ++j) {
			for (int i = 0; i < 4; ++i) {
				plane[(y+j)*stride+x+i] = (byte)val;
			}
		}
	}

	private static void predictBVE(int x, int y, int[] e, int stride, byte[] plane) {

		for (int i = 0; i < 4; ++i) {
			int val = Util.avg3(e[4+i], e[5+i], e[6+i]);
			for (int j = 0; j < 4; ++j) {
				plane[(y+j)*stride+x+i] = (byte)val;
			}
		}
	}

	private static void predictBHE(int x, int y, int[] e, int stride, byte[] plane) {

		for (int j = 0; j < 4; ++j) {
			int val = Util.avg3(e[4-j], e[3-j], (j == 3) ? e[0] : e[2-j]);
			for (int i = 0; i < 4; ++i) {
				plane[(y+j)*stride+x+i] = (byte)val;
			}
		}
	}

	private static void predictBTM(int x, int y, int[] e, int stride, byte[] plane) {
		for (int j = 0; j < 4; ++j) {
			for (int i = 0; i < 4; ++i) {
				int val = Util.clamp255(e[5+i] + e[3-j] - e[4]);
				plane[(y+j)*stride+x+i] = (byte)val;

			}
		}

	}


	private static void predictBLD(int x, int y, int[] e, int stride, byte[] plane) {
		int val = Util.avg3(e[5], e[6], e[7]);
		plane[(y+0)*stride+x+0] = (byte)val;

		val = Util.avg3(e[6], e[7], e[8]);
		plane[(y+1)*stride+x+0] = (byte)val;
		plane[(y+0)*stride+x+1] = (byte)val;

		val = Util.avg3(e[7], e[8], e[9]);
		plane[(y+2)*stride+x+0] = (byte)val;
		plane[(y+1)*stride+x+1] = (byte)val;
		plane[(y+0)*stride+x+2] = (byte)val;

		val = Util.avg3(e[8], e[9], e[10]);
		plane[(y+3)*stride+x+0] = (byte)val;
		plane[(y+2)*stride+x+1] = (byte)val;
		plane[(y+1)*stride+x+2] = (byte)val;
		plane[(y+0)*stride+x+3] = (byte)val;

		val = Util.avg3(e[9], e[10], e[11]);
		plane[(y+3)*stride+x+1] = (byte)val;
		plane[(y+2)*stride+x+2] = (byte)val;
		plane[(y+1)*stride+x+3] = (byte)val;

		val = Util.avg3(e[10], e[11], e[12]);
		plane[(y+3)*stride+x+2] = (byte)val;
		plane[(y+2)*stride+x+3] = (byte)val;

		val = Util.avg3(e[11], e[12], e[12]);
		plane[(y+3)*stride+x+3] = (byte)val;
	}

	private static void predictBRD(int x, int y, int[] e, int stride, byte[] plane) {
		int val = Util.avg3(e[0], e[1], e[2]);
		plane[(y+3)*stride+x+0] = (byte)val;

		val = Util.avg3(e[1], e[2], e[3]);
		plane[(y+3)*stride+x+1] = (byte)val;
		plane[(y+2)*stride+x+0] = (byte)val;

		val = Util.avg3(e[2], e[3], e[4]);
		plane[(y+3)*stride+x+2] = (byte)val;
		plane[(y+2)*stride+x+1] = (byte)val;
		plane[(y+1)*stride+x+0] = (byte)val;

		val = Util.avg3(e[3], e[4], e[5]);
		plane[(y+3)*stride+x+3] = (byte)val;
		plane[(y+2)*stride+x+2] = (byte)val;
		plane[(y+1)*stride+x+1] = (byte)val;
		plane[(y+0)*stride+x+0] = (byte)val;

		val = Util.avg3(e[4], e[5], e[6]);
		plane[(y+2)*stride+x+3] = (byte)val;
		plane[(y+1)*stride+x+2] = (byte)val;
		plane[(y+0)*stride+x+1] = (byte)val;

		val = Util.avg3(e[5], e[6], e[7]);
		plane[(y+1)*stride+x+3] = (byte)val;
		plane[(y+0)*stride+x+2] = (byte)val;

		val = Util.avg3(e[6], e[7], e[8]);
		plane[(y+0)*stride+x+3] = (byte)val;
	}

	private static void predictBVR(int x, int y, int[] e, int stride, byte[] plane) {
		int val = Util.avg3(e[1], e[2], e[3]);
		plane[(y+3)*stride+x+0] = (byte)val;

		val = Util.avg3(e[2], e[3], e[4]);
		plane[(y+2)*stride+x+0] = (byte)val;

		val = Util.avg3(e[3], e[4], e[5]);
		plane[(y+3)*stride+x+1] = (byte)val;
		plane[(y+1)*stride+x+0] = (byte)val;

		val = Util.avg2(e[4], e[5]);
		plane[(y+2)*stride+x+1] = (byte)val;
		plane[(y+0)*stride+x+0] = (byte)val;

		val = Util.avg3(e[4], e[5], e[6]);
		plane[(y+3)*stride+x+2] = (byte)val;
		plane[(y+1)*stride+x+1] = (byte)val;

		val = Util.avg2(e[5], e[6]);
		plane[(y+2)*stride+x+2] = (byte)val;
		plane[(y+0)*stride+x+1] = (byte)val;

		val = Util.avg3(e[5], e[6], e[7]);
		plane[(y+3)*stride+x+3] = (byte)val;
		plane[(y+1)*stride+x+2] = (byte)val;

		val = Util.avg2(e[6], e[7]);
		plane[(y+2)*stride+x+3] = (byte)val;
		plane[(y+0)*stride+x+2] = (byte)val;

		val = Util.avg3(e[6], e[7], e[8]);
		plane[(y+1)*stride+x+3] = (byte)val;

		val = Util.avg2(e[7], e[8]);
		plane[(y+0)*stride+x+3] = (byte)val;
	}

	private static void predictBVL(int x, int y, int[] e, int stride, byte[] plane) {
		int val = Util.avg2(e[5], e[6]);
		plane[(y+0)*stride+x+0] = (byte)val;

		val = Util.avg3(e[5], e[6], e[7]);
		plane[(y+1)*stride+x+0] = (byte)val;

		val = Util.avg2(e[6], e[7]);
		plane[(y+2)*stride+x+0] = (byte)val;
		plane[(y+0)*stride+x+1] = (byte)val;

		val = Util.avg3(e[6], e[7], e[8]);
		plane[(y+1)*stride+x+1] = (byte)val;
		plane[(y+3)*stride+x+0] = (byte)val;

		val = Util.avg2(e[7], e[8]);
		plane[(y+2)*stride+x+1] = (byte)val;
		plane[(y+0)*stride+x+2] = (byte)val;

		val = Util.avg3(e[7], e[8], e[9]);
		plane[(y+3)*stride+x+1] = (byte)val;
		plane[(y+1)*stride+x+2] = (byte)val;

		val = Util.avg2(e[8], e[9]);
		plane[(y+2)*stride+x+2] = (byte)val;
		plane[(y+0)*stride+x+3] = (byte)val;

		val = Util.avg3(e[8], e[9], e[10]);
		plane[(y+3)*stride+x+2] = (byte)val;
		plane[(y+1)*stride+x+3] = (byte)val;

		val = Util.avg3(e[9], e[10], e[11]);
		plane[(y+2)*stride+x+3] = (byte)val;

		val = Util.avg3(e[10], e[11], e[12]);
		plane[(y+3)*stride+x+3] = (byte)val;

	}

	private static void predictBHD(int x, int y, int[] e, int stride, byte[] plane) {
		int val = Util.avg2(e[0], e[1]);
		plane[(y+3)*stride+x+0] = (byte)val;

		val = Util.avg3(e[0], e[1], e[2]);
		plane[(y+3)*stride+x+1] = (byte)val;

		val = Util.avg2(e[1], e[2]);
		plane[(y+2)*stride+x+0] = (byte)val;
		plane[(y+3)*stride+x+2] = (byte)val;

		val = Util.avg3(e[1], e[2], e[3]);
		plane[(y+2)*stride+x+1] = (byte)val;
		plane[(y+3)*stride+x+3] = (byte)val;

		val = Util.avg2(e[2], e[3]);
		plane[(y+2)*stride+x+2] = (byte)val;
		plane[(y+1)*stride+x+0] = (byte)val;

		val = Util.avg3(e[2], e[3], e[4]);
		plane[(y+2)*stride+x+3] = (byte)val;
		plane[(y+1)*stride+x+1] = (byte)val;

		val = Util.avg2(e[3], e[4]);
		plane[(y+1)*stride+x+2] = (byte)val;
		plane[(y+0)*stride+x+0] = (byte)val;

		val = Util.avg3(e[3], e[4], e[5]);
		plane[(y+1)*stride+x+3] = (byte)val;
		plane[(y+0)*stride+x+1] = (byte)val;

		val = Util.avg3(e[4], e[5], e[6]);
		plane[(y+0)*stride+x+2] = (byte)val;

		val = Util.avg3(e[5], e[6], e[7]);
		plane[(y+0)*stride+x+3] = (byte)val;

	}

	private static void predictBHU(int x, int y, int[] e, int stride, byte[] plane) {
		int val = Util.avg2(e[2], e[3]);
		plane[(y+0)*stride+x+0] = (byte)val;

		val = Util.avg3(e[1], e[2], e[3]);
		plane[(y+0)*stride+x+1] = (byte)val;

		val = Util.avg2(e[1], e[2]);
		plane[(y+0)*stride+x+2] = (byte)val;
		plane[(y+1)*stride+x+0] = (byte)val;

		val = Util.avg3(e[0], e[1], e[2]);
		plane[(y+0)*stride+x+3] = (byte)val;
		plane[(y+1)*stride+x+1] = (byte)val;

		val = Util.avg2(e[0], e[1]);
		plane[(y+1)*stride+x+2] = (byte)val;
		plane[(y+2)*stride+x+0] = (byte)val;

		val = Util.avg3(e[0], e[0], e[1]);
		plane[(y+1)*stride+x+3] = (byte)val;
		plane[(y+2)*stride+x+1] = (byte)val;

		val = e[0];
		plane[(y+2)*stride+x+2] = (byte)val;
		plane[(y+2)*stride+x+3] = (byte)val;

		plane[(y+3)*stride+x+0] = (byte)val;
		plane[(y+3)*stride+x+1] = (byte)val;
		plane[(y+3)*stride+x+2] = (byte)val;
		plane[(y+3)*stride+x+3] = (byte)val;
	}


}
