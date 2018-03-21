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

import com.jaamsim.Statistics.SampleStatistics;
import com.jaamsim.Statistics.TimeBasedStatistics;
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

}
