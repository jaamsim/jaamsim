/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018 JaamSim Software Inc.
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
package com.jaamsim.rng;

/**
 * Combined MRG based on L'Ecuyer (1999a), implementation ported from the ANSI C
 * version provided in Simulation Modeling and Analysis 5th Ed. Averill M. Law. (Appendix 7B)
 */
public class MRG1999a {
	private static final long m1 = 4294967087l;
	private static final long m2 = 4294944443l;
	private static final double norm   = 2.328306549295727688e-10d; // 1.0 / (m1 + 1)

	// The internal state machine is held in 6 integer values (treat as unsigned)
	int s0, s1, s2, s3, s4, s5;

	// Saved initial state
	int stream = -1;
	int substream;
	long[] initSeeds;

	private static final long streamAdvance[][] = {
		{ 2427906178L, 3580155704L,  949770784L },
		{  226153695L, 1230515664L, 3580155704L },
		{ 1988835001L,  986791581L, 1230515664L },
		{ 1464411153L,  277697599L, 1610723613L },
		{   32183930L, 1464411153L, 1022607788L },
		{ 2824425944L,   32183930L, 2093834863L }
	};

	private static final long substreamAdvance[][] = {
		{   82758667L, 1871391091L, 4127413238L },
		{ 3672831523L,   69195019L, 1871391091L },
		{ 3672091415L, 3528743235L,   69195019L },
		{ 1511326704L, 3759209742L, 1610795712L },
		{ 4292754251L, 1511326704L, 3889917532L },
		{ 3859662829L, 4292754251L, 3708466080L }
	};

	private static final int seedCacheSize = 20;
	private static final int seedCacheIncrement = 5000;
	private static final long seedCache[][] = new long[seedCacheSize][6];  // seeds after calling advanceStream

	static {
		long seeds[] = { 12345, 12345, 12345, 12345, 12345, 12345 };
		for (int i = 0; i <= (seedCacheSize - 1) * seedCacheIncrement; i++) {
			if (i % seedCacheIncrement == 0) {
				int seedIdx = i / seedCacheIncrement;
				for (int j = 0; j < seeds.length; j++)
					seedCache[seedIdx][j] = seeds[j];
			}
			advanceStream(seeds);
		}
	}

	/**
	 * Constructs a random generator seeded with values the first entry in the seed
	 * table.
	 */
	public MRG1999a() {
		this(0, 0);
	}

	/**
	 * Constructs a random generator seeded with values from the given stream number.
	 * @param stream the number of the desired stream to seed the generator
	 */
	public MRG1999a(int stream, int substream) {
		setSeedStream(stream, substream);
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
	 * Return an integer as a long, treating the int as unsigned
	 * @param i
	 * @return
	 */
	private long uint(int i) {
		return i & 0xffffffffl;
	}

	/**
	 * Seed the MRG with values form the given stream number.
	 * @param stream
	 */
	public void setSeedStream(int stream, int substream) {
		if (stream < 0)
			throw new IllegalArgumentException("Stream numbers must be positive");

		if (substream < 0)
			throw new IllegalArgumentException("Substream numbers must be positive");

		long seeds[] = new long[6];
		int initSubstream = 0;

		// If the same stream is used, start with the saved substream
		if (stream == this.stream && substream >= this.substream) {
			initSubstream = this.substream;
			seeds = initSeeds;
		}

		// Otherwise, start from the cached stream data
		else {

			// Find the cached seed with stream closest to, but not exceeding this stream
			int cacheSeedIdx = Math.min(stream / seedCacheIncrement, seedCacheSize - 1);
			for (int j = 0; j < seeds.length; j++)
				seeds[j] = seedCache[cacheSeedIdx][j];

			for (int i = cacheSeedIdx * seedCacheIncrement; i < stream; i++)
				advanceStream(seeds);
		}

		for (int i = initSubstream; i < substream; i++)
			advanceSubstream(seeds);

		setSeed(seeds[0], seeds[1], seeds[2], seeds[3], seeds[4], seeds[5]);

		// Save the initial state for the generator
		this.stream = stream;
		this.substream = substream;
		initSeeds = seeds;
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
		this.s0 = (int)s0; this.s1 = (int)s1; this.s2 = (int)s2;
		this.s3 = (int)s3; this.s4 = (int)s4; this.s5 = (int)s5;
	}

	/**
	 * Get the next uniformly distributed double value U(0,1)
	 */
	public double nextUniform() {
		// Mix the first half of the state
		long p1 = 1403580l * uint(s1) - 810728l * uint(s0);
		p1 = p1 % m1;
		if (p1 < 0) p1 += m1;
		s0 = s1; s1 = s2; s2 = (int)p1;

		// Mix the second half of the state
		long p2 = 527612l * uint(s5) - 1370589l * uint(s3);
		p2 = p2 % m2;
		if (p2 < 0) p2 += m2;
		s3 = s4; s4 = s5; s5 = (int)p2;

		long p = p1 - p2;
		if (p <= 0) p += m1;
		return p * norm;
	}

	@Override
	public String toString() {
		return String.format("%d, %d, %d, %d, %d, %d",
		                     uint(s0), uint(s1), uint(s2), uint(s3), uint(s4), uint(s5));
	}

	private static long ulong_mod(long val, long mod) {
		if (val < 0) {
			val &= Long.MAX_VALUE;
			val = (val % mod) - (Long.MIN_VALUE % mod);
		}

		return val % mod;
	}

	private static long mixHalf1(long[] a, long[] s) {
		long tmp;
		tmp = ulong_mod(a[0] * s[0]      , m1);
		tmp = ulong_mod(a[1] * s[1] + tmp, m1);
		tmp = ulong_mod(a[2] * s[2] + tmp, m1);
		return tmp;
	}

	private static long mixHalf2(long[] a, long[] s) {
		long tmp;
		tmp = ulong_mod(a[0] * s[3]      , m2);
		tmp = ulong_mod(a[1] * s[4] + tmp, m2);
		tmp = ulong_mod(a[2] * s[5] + tmp, m2);
		return tmp;
	}

	static void advanceStream(long[] seeds) {
		long s0 = mixHalf1(streamAdvance[0], seeds);
		long s1 = mixHalf1(streamAdvance[1], seeds);
		long s2 = mixHalf1(streamAdvance[2], seeds);

		long s3 = mixHalf2(streamAdvance[3], seeds);
		long s4 = mixHalf2(streamAdvance[4], seeds);
		long s5 = mixHalf2(streamAdvance[5], seeds);

		seeds[0] = s0; seeds[1] = s1; seeds[2] = s2;
		seeds[3] = s3; seeds[4] = s4; seeds[5] = s5;
	}

	static void advanceSubstream(long[] seeds) {
		long s0 = mixHalf1(substreamAdvance[0], seeds);
		long s1 = mixHalf1(substreamAdvance[1], seeds);
		long s2 = mixHalf1(substreamAdvance[2], seeds);

		long s3 = mixHalf2(substreamAdvance[3], seeds);
		long s4 = mixHalf2(substreamAdvance[4], seeds);
		long s5 = mixHalf2(substreamAdvance[5], seeds);

		seeds[0] = s0; seeds[1] = s1; seeds[2] = s2;
		seeds[3] = s3; seeds[4] = s4; seeds[5] = s5;
	}
}
