/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.video.vp8;

public class Transform {



	public static void deWHT(short[] in, short[] out, short[] temp) {
		baseWHT(in, out, temp);
		for (int i = 0; i < 16; ++i) {
			out[i] = (short)((out[i] + 3) >> 3);
		}
	}

	public static void WHT(short[] in, short[] out, short[] temp) {
		baseWHT(in, out, temp);
		for (int i = 0; i < 16; ++i) {
			out[i] = (short)((out[i] + 1) >> 1);
		}
	}

	private static void baseWHT(short[] in, short[] out, short[] temp) {
		int a, b, c, d;

		// columns
		for (int i = 0; i < 4; i++)
		{
			a = in[0+i] + in[12+i];
			b = in[4+i] + in[ 8+i];
			c = in[4+i] - in[ 8+i];
			d = in[0+i] - in[12+i];

			temp[ 0+i] = (short)(a + b);
			temp[ 4+i] = (short)(c + d);
			temp[ 8+i] = (short)(a - b);
			temp[12+i] = (short)(d - c);
		}

		// rows
		for (int i = 0; i < 4; i++)
		{
			a = temp[0+4*i] + temp[3+4*i];
			b = temp[1+4*i] + temp[2+4*i];
			c = temp[1+4*i] - temp[2+4*i];
			d = temp[0+4*i] - temp[3+4*i];

			out[0+4*i] = (short)(a + b);
			out[1+4*i] = (short)(c + d);
			out[2+4*i] = (short)(a - b);
			out[3+4*i] = (short)(d - c);
		}
	}



	private static final int cospi8sqrt2minus1 = 20091;
	private static final int sinpi8sqrt2       = 35468;

	public static void deDCT(short[] in, short[] out, short[] temp) {
		int a, b, c, d;
		int t1, t2;

		// Columns
		for (int i = 0; i < 4; i++)
		{
			a = in[0+i] + in[8+i];
			b = in[0+i] - in[8+i];

			t1= (in[ 4+i] * sinpi8sqrt2) >> 16;
			t2 = in[12+i] + ((in[12+i] * cospi8sqrt2minus1) >> 16);
			c = t1 - t2;

			t1 = in[ 4+i] + ((in[4+i] * cospi8sqrt2minus1) >> 16);
			t2 = (in[12+i] * sinpi8sqrt2) >> 16;
			d = t1 + t2;

			temp[ 0+i] = (short)(a + d);
			temp[12+i] = (short)(a - d);

			temp[ 4+i] = (short)(b + c);
			temp[ 8+i] = (short)(b - c);
		}

		// Rows
		for (int i = 0; i < 4; i++)
		{
			a = temp[0+4*i] + temp[2+4*i];
			b = temp[0+4*i] - temp[2+4*i];

			t1= (temp[1+4*i] * sinpi8sqrt2) >> 16;
			t2 = temp[3+4*i] + ((temp[3+4*i] * cospi8sqrt2minus1) >> 16);
			c = t1 - t2;

			t1 = temp[1+4*i] + ((temp[1+4*i] * cospi8sqrt2minus1) >> 16);
			t2 = (temp[3+4*i] * sinpi8sqrt2) >> 16;
			d = t1 + t2;

			out[0+4*i] = (short)((a + d + 4) >> 3);
			out[3+4*i] = (short)((a - d + 4) >> 3);

			out[1+4*i] = (short)((b + c + 4) >> 3);
			out[2+4*i] = (short)((b - c + 4) >> 3);
		}


	}

	public static void DCT(short[] in, short[] out, short[] temp) {

		// Columns
		for (int i = 0; i < 4; i++)
		{
			temp[ 0+i] = (short)(in[ 0+i] + in[ 4+i] + in[ 8+i] + in[12+i]);
			temp[ 8+i] = (short)(in[ 0+i] - in[ 4+i] - in[ 8+i] + in[12+i]);

			temp[ 4+i] = (short)(
			                   (in[ 0+i] + ((in[ 0+i] * cospi8sqrt2minus1) >> 16))
			                + ((in[ 4+i] * sinpi8sqrt2) >> 16)
			                - ((in[ 8+i] * sinpi8sqrt2) >> 16)
			                -  (in[12+i] + ((in[12+i] * cospi8sqrt2minus1) >> 16))
			                );
			temp[12+i] = (short)(
	                  ((in[ 0+i] * sinpi8sqrt2) >> 16)
	                -  (in[ 4+i] + ((in[ 4+i] * cospi8sqrt2minus1) >> 16))
	                +  (in[ 8+i] + ((in[ 8+i] * cospi8sqrt2minus1) >> 16))
	                - ((in[12+i] * sinpi8sqrt2) >> 16)
	                );
		}

		// Rows
		for (int i = 0; i < 4; i++)
		{
			out[0+i*4] = (short)(temp[0+i*4] + temp[1+i*4] + temp[2+i*4] + temp[3+i*4]);
			out[2+i*4] = (short)(temp[0+i*4] - temp[1+i*4] - temp[2+i*4] + temp[3+i*4]);

			out[1+i*4] = (short)(
			                   (temp[0+i*4] + ((temp[0+i*4] * cospi8sqrt2minus1) >> 16))
			                + ((temp[1+i*4] * sinpi8sqrt2) >> 16)
			                - ((temp[2+i*4] * sinpi8sqrt2) >> 16)
			                -  (temp[3+i*4] + ((temp[3+i*4] * cospi8sqrt2minus1) >> 16))
			                );
			out[3+i*4] = (short)(
			               ((temp[0+i*4] * sinpi8sqrt2) >> 16)
			             -  (temp[1+i*4] + ((temp[1+i*4] * cospi8sqrt2minus1) >> 16))
			             +  (temp[2+i*4] + ((temp[2+i*4] * cospi8sqrt2minus1) >> 16))
			             - ((temp[3+i*4] * sinpi8sqrt2) >> 16)
	                );

			out[0+i*4] = (short)(out[0+i*4] + 1 >> 1);
			out[1+i*4] = (short)(out[1+i*4] + 1 >> 1);
			out[2+i*4] = (short)(out[2+i*4] + 1 >> 1);
			out[3+i*4] = (short)(out[3+i*4] + 1 >> 1);
		}
	}

	/**
	 * Get the equivalent of the DC component, useful for encoding Y2 residue blocks
	 * @param in
	 * @return
	 */
	public static short DCTVal0(short[] in) {
		short ret = 0;
		for (int i = 0; i < 16; ++i) {
			ret += in[i];
		}
		return (short)((ret + 1) >> 1);
	}

}
