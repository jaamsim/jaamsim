/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.rng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Combined MRG based on L'Ecuyer (1999a), implementation ported from the ANSI C
 * version provided in Simulation Modelling and Analyis 4th Ed. Averill M. Law. (Appendix 7B)
 */
public class MRG1999a {
	static final long[] seeds;
	private static final long m1 = 4294967087l;
	private static final long m2 = 4294944443l;
	private static final double norm   = 2.328306549295727688e-10d; // 1.0 / (m1 + 1)

	// The internal state machine is held in 6 long values
	long s0, s1, s2, s3, s4, s5;

	static {
		InputStream s = MRG1999a.class.getResourceAsStream("MRG1999a.seed");
		BufferedReader buf =  new BufferedReader(new InputStreamReader(s));
		ArrayList<long[]> tempseeds = new ArrayList<long[]>(10001);

		while (true) {
			String line = null;
			try {
				line = buf.readLine();
			}
			catch (IOException e) {
				throw new RuntimeException("Could not read all seeds for MRG1999a");
			}

			if (line == null) break;

			String[] split = line.split(",");
			if (split.length != 6)
				throw new RuntimeException("Corrupt seed (need 6 values) found in MRG1999a.seed");
			long[] d = new long[6];
			for (int i = 0; i < 6; i++)
				d[i] = Long.parseLong(split[i]);
			tempseeds.add(d);
		}
		try { buf.close(); } catch (IOException e) {}
		buf = null;

		seeds = new long[tempseeds.size() * 6];
		for (int i = 0; i < tempseeds.size(); i++) {
			int offset = i * 6;
			long[] each = tempseeds.get(i);
			for (int j = 0; j < 6; j++)
				seeds[offset + j] = each[j];
		}
	}

	/**
	 * Constructs a random generator seeded with values the first entry in the seed
	 * table.
	 */
	public MRG1999a() {
		this(0);
	}

	/**
	 * Constructs a random generator seeded with values from the given stream number.
	 * @param stream the number of the desired stream to seed the generator
	 */
	public MRG1999a(int stream) {
		setSeedStream(stream);
	}

	/**
	 * Constructs a random generator seeded with the given values.
	 * @param s0
	 * @param s1
	 * @param s2
	 * @param s3
	 * @param s4
	 * @param s5
	 */
	public MRG1999a(long s0, long s1, long s2, long s3, long s4, long s5) {
		setSeed(s0, s1, s2, s3, s4, s5);
	}

	/**
	 * Seed the MRG with values form the given stream number.
	 * @param stream
	 */
	public void setSeedStream(int stream) {
		int offset = stream * 6;
		if (stream < 0)
			throw new IllegalArgumentException("Stream numbers must be positive");
		if (offset >= seeds.length)
			throw new IllegalArgumentException("Maximum stream number is " + seeds.length / 6);

		setSeed(seeds[offset + 0], seeds[offset + 1], seeds[offset + 2],
				seeds[offset + 3], seeds[offset + 4], seeds[offset + 5]);
	}

	public void setSeed(long s0, long s1, long s2, long s3, long s4, long s5) {
		if (s0 == 0 && s1 == 0 && s2 == 0)
			throw new IllegalArgumentException("The first three seeds cannot all be 0");
		if (s3 == 0 && s4 == 0 && s5 == 0)
			throw new IllegalArgumentException("The last three seeds cannot all be 0");
		if (s0 >= m1 || s1 >= m1 || s2 >= m1)
			throw new IllegalArgumentException("The first three seeds must be < " + m1);
		if (s3 >= m1 || s4 >= m1 || s5 >= m1)
			throw new IllegalArgumentException("The last three seeds must be < " + m2);
		if (s0 < 0 || s1 < 0 || s2 < 0 || s3 < 0 || s4 < 0 || s5 < 0)
			throw new IllegalArgumentException("All seeds must be > 0");
		this.s0 = s0; this.s1 = s1; this.s2 = s2;
		this.s3 = s3; this.s4 = s4; this.s5 = s5;
	}

	/**
	 * Get the next uniformly distributed doubel value U(0,1)
	 * @return
	 */
	public double getUniform() {
		long p;

		// Mix the first half of the state
		p = 1403580l * s1 - 810728l * s0;
		p = p % m1;
		if (p < 0) p += m1;
		s0 = s1; s1 = s2; s2 = p;

		// Mix the second half of the state
		p = 527612l * s5 - 1370589l * s3;
		p = p % m2;
		if (p < 0) p += m2;
		s3 = s4; s4 = s5; s5 = p;

		p = s2 - s5;
		if (p <= 0) p += m1;
		return p * norm;
	}

	@Override
	public String toString() {
		return String.format("%d, %d, %d, %d, %d, %d", s0, s1, s2, s3, s4, s5);
	}
}
