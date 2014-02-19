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

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.junit.Test;

public class TestMRG1999a {
	@Test
	public void testSeedTable() {
		InputStream s = MRG1999a.class.getResourceAsStream("MRG1999a.seed");
		BufferedReader buf =  new BufferedReader(new InputStreamReader(s));
		ArrayList<String> lines = new ArrayList<String>();

		while (true) {
			String line = null;
			try {
				line = buf.readLine();
			}
			catch (IOException e) {
				throw new RuntimeException("Could not read all seeds for MRG1999a");
			}

			if (line == null) break;
			lines.add(line);
		}

		for (int i = 0; i < lines.size(); i++) {
			int offset = i * 6;
			long s0 = MRG1999a.seeds[offset + 0];
			long s1 = MRG1999a.seeds[offset + 1];
			long s2 = MRG1999a.seeds[offset + 2];
			long s3 = MRG1999a.seeds[offset + 3];
			long s4 = MRG1999a.seeds[offset + 4];
			long s5 = MRG1999a.seeds[offset + 5];
			String fmtLine = String.format("%d,%d,%d,%d,%d,%d", s0, s1, s2, s3, s4, s5);
			assertTrue(lines.get(i).equals(fmtLine));
		}
	}

	@Test
	public void testAllSeedsTable() {
		MRG1999a tmp = new MRG1999a();
		int numStreams = MRG1999a.seeds.length / 6;
		for (int i = 0; i < numStreams; i++) {
			tmp.setSeedStream(i);
		}
	}

	@Test
	public void testKnownStreams() {
		MRG1999a test1 = new MRG1999a(0, 0, 1, 0, 0, 1);
		String[] known1 = {
		"0, 0, 1, 0, 0, 1",
		"0, 1, 0, 0, 1, 527612",
		"1, 0, 1403580, 1, 527612, 3497978192",
		"0, 1403580, 4294156359, 527612, 3497978192, 3281754271",
		"1403580, 4294156359, 2941890554, 3497978192, 3281754271, 1673476130",
		"4294156359, 2941890554, 489343630, 3281754271, 1673476130, 1430724370",
		"2941890554, 489343630, 1831234280, 1673476130, 1430724370, 893509979",
		"489343630, 1831234280, 2758233149, 1430724370, 893509979, 3280220074",
		"1831234280, 2758233149, 939574583, 893509979, 3280220074, 361718588",
		"2758233149, 939574583, 3228066636, 3280220074, 361718588, 951529882",
		"939574583, 3228066636, 513534955, 361718588, 951529882, 856588367"
		};
		assertTrue(test1.toString().equals(known1[0]));
		for (int i = 1; i < known1.length; i++) {
			test1.getUniform(); // advance the state
			assertTrue(test1.toString().equals(known1[i]));
		}

		test1.setSeedStream(0); // we know stream 0 is {0,0,1,0,0,1}
		assertTrue(test1.toString().equals(known1[0]));
		for (int i = 1; i < known1.length; i++) {
			test1.getUniform(); // advance the state
			assertTrue(test1.toString().equals(known1[i]));
		}

		MRG1999a test2 = new MRG1999a(12345, 12345, 12345, 12345, 12345, 12345);
		String[] known2 = {
		"12345, 12345, 12345, 12345, 12345, 12345",
		"12345, 12345, 3023790853, 12345, 12345, 2478282264",
		"12345, 3023790853, 3023790853, 12345, 2478282264, 1655725443",
		"3023790853, 3023790853, 3385359573, 2478282264, 1655725443, 2057415812",
		"3023790853, 3385359573, 1322208174, 1655725443, 2057415812, 2070190165",
		"3385359573, 1322208174, 2930192941, 2057415812, 2070190165, 1978299747",
		"1322208174, 2930192941, 2462079208, 2070190165, 1978299747, 171163572",
		"2930192941, 2462079208, 2386811717, 1978299747, 171163572, 321902337",
		"2462079208, 2386811717, 2989318136, 171163572, 321902337, 1462200156",
		"2386811717, 2989318136, 3378525425, 321902337, 1462200156, 2794459678",
		"2989318136, 3378525425, 1773647758, 1462200156, 2794459678, 2822254363"
		};
		assertTrue(test2.toString().equals(known2[0]));
		for (int i = 1; i < known2.length; i++) {
			test2.getUniform(); // advance the state
			assertTrue(test2.toString().equals(known2[i]));
		}
	}

	@Test
	public void testAllStreams() {
		InputStream s = MRG1999a.class.getResourceAsStream("MRGKnownStreams.txt");
		BufferedReader buf =  new BufferedReader(new InputStreamReader(s));
		ArrayList<String> lines = new ArrayList<String>(110011);

		while (true) {
			String line = null;
			try {
				line = buf.readLine();
			}
			catch (IOException e) {
				throw new RuntimeException("Could not read all seeds for MRG1999a");
			}

			if (line == null) break;
			lines.add(line);
		}

		MRG1999a tmp = new MRG1999a();
		int numStreams = MRG1999a.seeds.length / 6;
		for (int i = 0; i < numStreams; i++) {
			tmp.setSeedStream(i);
			int knownStreamIdx = i * 11;
			assertTrue(tmp.toString().equals(lines.get(knownStreamIdx)));
			for (int j = 1; j < 11; j++) {
				tmp.getUniform(); // advance the state
				assertTrue(tmp.toString().equals(lines.get(knownStreamIdx + j)));
			}
		}
	}
}
