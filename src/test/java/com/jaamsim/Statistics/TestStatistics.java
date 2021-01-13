/*
 * JaamSim Discrete Event Simulation
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
package com.jaamsim.Statistics;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jaamsim.math.MathUtils;

public class TestStatistics {

	@Test
	public void testSampleStatistics() {
		SampleStatistics stats = new SampleStatistics();
		stats.addValue(5.0d);
		stats.addValue(3.0d);
		stats.addValue(8.0d);
		stats.addValue(-1.0d);
		stats.addValue(2.0d);

		assertTrue(stats.getCount() == 5L);
		assertTrue(stats.getMin() == -1.0d);
		assertTrue(stats.getMax() == 8.0d);
		assertTrue(stats.getSum() == 17.0d);
		assertTrue(stats.getSumSquared() == 103.0d);
		assertTrue(MathUtils.near(stats.getMean(), 3.4d));
		assertTrue(MathUtils.near(stats.getMeanSquared(), 20.6d));
		assertTrue(MathUtils.near(stats.getVariance(), 9.04d));
		assertTrue(MathUtils.near(stats.getStandardDeviation(), Math.sqrt(9.04d)));
	}

	@Test
	public void testTimeBasedStatistics() {
		TimeBasedStatistics stats = new TimeBasedStatistics();
		stats.addValue(0.0d, 2.0d);
		stats.clear();
		stats.addValue(2.0d, 5.0d);
		stats.addValue(4.0d, 3.0d);
		stats.addValue(5.0d, 8.0d);
		stats.addValue(8.0d, -1.0d);
		stats.addValue(9.0d, 2.0d);

		assertTrue(stats.getMin() == -1.0d);
		assertTrue(stats.getMax() == 8.0d);
		assertTrue(stats.getSum(12.0d) == 42.0d);
		assertTrue(stats.getSumSquared(12.0d) == 264.0d);
		assertTrue(MathUtils.near(stats.getMean(12.0d), 4.2d));
		assertTrue(MathUtils.near(stats.getMeanSquared(12.0d), 26.4d));
		assertTrue(MathUtils.near(stats.getVariance(12.0d), 8.76d));
		assertTrue(MathUtils.near(stats.getStandardDeviation(12.0d), Math.sqrt(8.76d)));
	}

	@Test
	public void testSampleFrequency() {
		SampleFrequency freq = new SampleFrequency(2, 7);
		freq.addValue(5);
		freq.addValue(3);
		freq.addValue(8);
		freq.addValue(-1);
		freq.addValue(2);
		freq.addValue(3);
		freq.addValue(5);
		freq.addValue(3);

		assertTrue(freq.getCount() == 8L);
		assertTrue(freq.getMin() == -1.0d);
		assertTrue(freq.getMax() == 8.0d);
		assertTrue(freq.getBinCount(3) == 3L);
		assertTrue(MathUtils.near(freq.getBinFraction(3), 0.375d));
		assertTrue(freq.getBinValues().length == 10);
		assertTrue(freq.getBinValues()[0] == -1);
		assertTrue(freq.getBinValues()[4] == 3);
		assertTrue(freq.getBinValues()[9] == 8);
		assertTrue(freq.getBinCounts().length == 10);
		assertTrue(freq.getBinCounts()[0] == 1L);
		assertTrue(freq.getBinCounts()[4] == 3L);
		assertTrue(freq.getBinCounts()[9] == 1L);
		assertTrue(freq.getBinFractions().length == 10);
		assertTrue(MathUtils.near(freq.getBinFractions()[0], 0.125d));
		assertTrue(MathUtils.near(freq.getBinFractions()[4], 0.375d));
		assertTrue(MathUtils.near(freq.getBinFractions()[9], 0.125d));
	}
	@Test
	public void testTimeBasedFrequency() {
		TimeBasedFrequency freq = new TimeBasedFrequency(2, 7);
		freq.addValue(0.0d, 1);
		freq.clear();
		freq.addValue(2.0d, 5);
		freq.addValue(3.0d, 3);
		freq.addValue(5.0d, 8);
		freq.addValue(8.0d, -1);
		freq.addValue(9.0d, 2);
		freq.addValue(11.0d, 3);
		freq.addValue(15.0d, 5);
		freq.addValue(16.0d, 3);

		assertTrue(MathUtils.near(freq.getTotalTime(22.0d), 20.0d));
		assertTrue(freq.getMin() == -1.0d);
		assertTrue(freq.getMax() == 8.0d);
		assertTrue(MathUtils.near(freq.getBinTime(22.0d, 3), 12.0d));
		assertTrue(MathUtils.near(freq.getBinFraction(22.0d, 3), 0.60d));
		assertTrue(freq.getBinValues().length == 10);
		assertTrue(freq.getBinValues()[0] == -1);
		assertTrue(freq.getBinValues()[4] == 3);
		assertTrue(freq.getBinValues()[9] == 8);
		assertTrue(freq.getBinTimes(22.0d).length == 10);
		assertTrue(MathUtils.near(freq.getBinTimes(22.0d)[0], 1.0d));
		assertTrue(MathUtils.near(freq.getBinTimes(22.0d)[4], 12.0d));
		assertTrue(MathUtils.near(freq.getBinTimes(22.0d)[9], 3.0d));
		assertTrue(freq.getBinFractions(22.0d).length == 10);
		assertTrue(MathUtils.near(freq.getBinFractions(22.0d)[0], 0.05d));
		assertTrue(MathUtils.near(freq.getBinFractions(22.0d)[4], 0.60d));
		assertTrue(MathUtils.near(freq.getBinFractions(22.0d)[9], 0.15d));
	}

}
