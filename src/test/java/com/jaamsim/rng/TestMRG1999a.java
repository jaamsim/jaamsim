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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestMRG1999a {

	@Test
	public void testKnownStreams() {
		long[] seeds1 = { 0, 0, 1, 0, 0, 1 };

		String[] known1 = {
		"0, 0, 1, 0, 0, 1",
		"949770784, 3580155704, 1230515664, 1610723613, 1022607788, 2093834863",
		"3106427820, 3199155377, 4290900039, 3167429889, 3572939265, 698814233",
		"693938118, 3153461912, 1636264737, 2581649800, 1834943086, 290967548",
		"3188270128, 1934119675, 826363896, 4070445105, 1933248566, 3962062826",
		"3757485922, 572568472, 2395114552, 3590438463, 538674679, 2165238291",
		"4075809725, 1035961073, 56103715, 329763193, 1230302420, 3869031993",
		"687667041, 1179017059, 5861898, 3354556565, 1742222303, 2941424961",
		"2481284279, 4288164505, 1177260757, 567967482, 784865788, 3134382704",
		"1515873698, 4089072055, 163684167, 1094056604, 3798743924, 3295131466",
		"920035025, 408996081, 2433373148, 673478093, 1620020757, 2269902114"
		};
		String test1 = String.format("%d, %d, %d, %d, %d, %d", seeds1[0], seeds1[1], seeds1[2], seeds1[3], seeds1[4], seeds1[5]);
		assertTrue(test1.equals(known1[0]));
		for (int i = 1; i < known1.length; i++) {
			MRG1999a.advanceStream(seeds1);
			test1 = String.format("%d, %d, %d, %d, %d, %d", seeds1[0], seeds1[1], seeds1[2], seeds1[3], seeds1[4], seeds1[5]);
			assertTrue(test1.equals(known1[i]));
		}

		String[] known2 = {
		"12345, 12345, 12345, 12345, 12345, 12345",
		"3692455944, 1366884236, 2968912127, 335948734, 4161675175, 475798818",
		"1015873554, 1310354410, 2249465273, 994084013, 2912484720, 3876682925",
		"2338701263, 1119171942, 2570676563, 317077452, 3194180850, 618832124",
		"1597262096, 3906379055, 3312112953, 1016013135, 4099474108, 275305423",
		"97147054, 3131372450, 829345164, 3691032523, 3006063034, 4259826321",
		"796079799, 2105258207, 955365076, 2923159030, 4116632677, 3067683584",
		"3281794178, 2616230133, 1457051261, 2762791137, 2480527362, 2282316169",
		"3777646647, 1837464056, 4204654757, 664239048, 4190510072, 2959195122",
		"4215590817, 3862461878, 1087200967, 1544910132, 936383720, 1611370123",
		"1683636369, 362165168, 814316280, 869382050, 980203903, 2062101717"
		};

		long[] seeds2 = { 12345, 12345, 12345, 12345, 12345, 12345 };
		String test2 = String.format("%d, %d, %d, %d, %d, %d", seeds2[0], seeds2[1], seeds2[2], seeds2[3], seeds2[4], seeds2[5]);
		assertTrue(test2.equals(known2[0]));
		for (int i = 1; i < known2.length; i++) {
			MRG1999a.advanceStream(seeds2);
			test2 = String.format("%d, %d, %d, %d, %d, %d", seeds2[0], seeds2[1], seeds2[2], seeds2[3], seeds2[4], seeds2[5]);
			assertTrue(test2.equals(known2[i]));
		}
	}

	@Test
	public void testKnownSubstreams() {
		long[] seeds1 = { 0, 0, 1, 0, 0, 1 };

		String[] known1 = {
		"0, 0, 1, 0, 0, 1",
		"4127413238, 1871391091, 69195019, 1610795712, 3889917532, 3708466080",
		"99651939, 2329303404, 2931758910, 878035866, 1926628839, 3196114006",
		"230171794, 3210181465, 3536018417, 2865846472, 1249197731, 3331097822",
		"1314332382, 1588259595, 2508077280, 3182508868, 3038399593, 2980208068",
		"4010413858, 401645099, 2106045662, 384279948, 1923026173, 564222425",
		"2307614257, 2703042396, 2823695054, 2384208331, 3412123799, 1365035178",
		"520437440, 4210727080, 3707259965, 804702670, 2645232736, 4072194992",
		"4049009082, 4183591379, 1453913233, 4095757548, 789475914, 2145357457",
		"2828141255, 752526256, 2097509046, 2724043008, 84549310, 1412103825",
		"2040707498, 3221815101, 1825015381, 3287341287, 2602801723, 4228411920"
		};
		String test1 = String.format("%d, %d, %d, %d, %d, %d", seeds1[0], seeds1[1], seeds1[2], seeds1[3], seeds1[4], seeds1[5]);
		assertTrue(test1.equals(known1[0]));
		for (int i = 1; i < known1.length; i++) {
			MRG1999a.advanceSubstream(seeds1);
			test1 = String.format("%d, %d, %d, %d, %d, %d", seeds1[0], seeds1[1], seeds1[2], seeds1[3], seeds1[4], seeds1[5]);
			assertTrue(test1.equals(known1[i]));
		}

		String[] known2 = {
		"12345, 12345, 12345, 12345, 12345, 12345",
		"870504860, 2641697727, 884013853, 339352413, 2374306706, 3651603887",
		"460387934, 1532391390, 877287553, 120103512, 2153115941, 335837774",
		"3775110060, 3208296044, 1257177538, 378684317, 2867112178, 2201306083",
		"1870130441, 490396226, 1081325149, 3685085721, 2348042618, 1094489500",
		"934940479, 1950100692, 4183308048, 1834563867, 1457690863, 2911850358",
		"36947768, 3877286680, 1490366786, 2869536097, 1753150659, 575546150",
		"2591627614, 2744298060, 626085041, 644044487, 2091171169, 3539660345",
		"2906523984, 140710505, 1144258739, 2076719571, 1742524362, 768984958",
		"2483450279, 3767309577, 2486764677, 4056403678, 792164890, 998062628",
		"1065618315, 827657608, 299165607, 461289958, 2074659312, 274796520"
		};

		long[] seeds2 = { 12345, 12345, 12345, 12345, 12345, 12345 };
		String test2 = String.format("%d, %d, %d, %d, %d, %d", seeds2[0], seeds2[1], seeds2[2], seeds2[3], seeds2[4], seeds2[5]);
		assertTrue(test2.equals(known2[0]));
		for (int i = 1; i < known2.length; i++) {
			MRG1999a.advanceSubstream(seeds2);
			test2 = String.format("%d, %d, %d, %d, %d, %d", seeds2[0], seeds2[1], seeds2[2], seeds2[3], seeds2[4], seeds2[5]);
			assertTrue(test2.equals(known2[i]));
		}
	}

	@Test
	public void testKnownStates() {
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
			test1.nextUniform(); // advance the state
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
			test2.nextUniform(); // advance the state
			assertTrue(test2.toString().equals(known2[i]));
		}
	}

	@Test
	public void testCachedSeeds() {
		MRG1999a test1 = new MRG1999a(0, 0);
		MRG1999a test2 = new MRG1999a(12345, 12345, 12345, 12345, 12345, 12345);

		assertTrue(test1.toString().equals(test2.toString()));

		// test stream 1 for each
		test1 = new MRG1999a(1, 0);
		long[] seeds2 = { 12345, 12345, 12345, 12345, 12345, 12345 };
		MRG1999a.advanceStream(seeds2);
		test2 = new MRG1999a(seeds2[0], seeds2[1], seeds2[2], seeds2[3], seeds2[4], seeds2[5]);

		assertTrue(test1.toString().equals(test2.toString()));

		// test stream 10500 for each
		test1 = new MRG1999a(10500, 0);
		long[] seeds3 = { 12345, 12345, 12345, 12345, 12345, 12345 };
		for (int i = 0; i < 10500; i++) {
			MRG1999a.advanceStream(seeds3);
		}
		test2 = new MRG1999a(seeds3[0], seeds3[1], seeds3[2], seeds3[3], seeds3[4], seeds3[5]);

		assertTrue(test1.toString().equals(test2.toString()));

		// test stream 20000 for each
		test1 = new MRG1999a(20000, 0);
		long[] seeds4 = { 12345, 12345, 12345, 12345, 12345, 12345 };
		for (int i = 0; i < 20000; i++) {
			MRG1999a.advanceStream(seeds4);
		}
		test2 = new MRG1999a(seeds4[0], seeds4[1], seeds4[2], seeds4[3], seeds4[4], seeds4[5]);

		assertTrue(test1.toString().equals(test2.toString()));
	}

	@Test
	public void testSavedSubstream() {

		// Test stream 1
		MRG1999a test1 = new MRG1999a(1, 100);

		MRG1999a test2 = new MRG1999a(1, 50);
		test2.setSeedStream(1, 100);

		assertTrue(test1.toString().equals(test2.toString()));

		// Test stream 10500
		test1 = new MRG1999a(10500, 1001);

		test2 = new MRG1999a(10500, 1000);
		test2.setSeedStream(10500, 1001);

		assertTrue(test1.toString().equals(test2.toString()));

		// Test later substream for 10500
		test1 = new MRG1999a(10500, 1010);
		test2.setSeedStream(10500, 1010);

		assertTrue(test1.toString().equals(test2.toString()));
	}
}
