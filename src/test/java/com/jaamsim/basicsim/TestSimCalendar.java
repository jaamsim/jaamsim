/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019-2022 JaamSim Software Inc.
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
package com.jaamsim.basicsim;

import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Test;

public class TestSimCalendar {

	@Test
	public void testGetMonthForDay() {
		assertTrue(SimCalendar.getMonthForDay(1) == 0);
		assertTrue(SimCalendar.getMonthForDay(31) == 0);
		assertTrue(SimCalendar.getMonthForDay(32) == 1);
		assertTrue(SimCalendar.getMonthForDay(31 + 28) == 1);
		assertTrue(SimCalendar.getMonthForDay(31 + 28 + 1) == 2);
		assertTrue(SimCalendar.getMonthForDay(365 - 31 + 1) == 11);
		assertTrue(SimCalendar.getMonthForDay(365) == 11);
	}

	@Test
	public void testSetTimeInMillis() {
		SimCalendar calendar = new SimCalendar();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss.SSS");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		// Simplified calendar
		calendar.setGregorian(false);

		long millisPerSec = 1000;
		long millisPerMin = 60 * millisPerSec;
		long millisPerHr  = 60 * millisPerMin;
		long millisPerDay = 24 * millisPerHr;
		long millisPerYr  = 365 * millisPerDay;

		long millis = 0L;
		millis += 2L * millisPerYr;  // 1972
		millis += (31L + 28L + 3L) * millisPerDay;  // March 4th
		millis += 10L * millisPerHr;  // 10am
		millis += 5L * millisPerMin;  // 10:05
		millis += 20L * millisPerSec; // 10:05:20
		millis += 500L;               // 10:05:20.500
		calendar.setTimeInMillis(millis);
		//System.out.println(dateFormat.format(calendar.getTime()));

		assertTrue(calendar.get(Calendar.YEAR) == 1972);
		assertTrue(calendar.get(Calendar.MONTH) == 2);
		assertTrue(calendar.get(Calendar.DAY_OF_MONTH) == 4);
		assertTrue(calendar.get(Calendar.HOUR_OF_DAY) == 10);
		assertTrue(calendar.get(Calendar.MINUTE) == 5);
		assertTrue(calendar.get(Calendar.SECOND) == 20);
		assertTrue(calendar.get(Calendar.MILLISECOND) == 500);

		assertTrue(calendar.getTimeInMillis(1972, 2, 4, 10, 5, 20, 500) == millis);

		// Gregorian calendar
		calendar.setGregorian(true);
		millis = calendar.getTimeInMillis(1972, 2, 4, 10, 5, 20, 500); // 1972 is a leap year
		calendar.setTimeInMillis(millis);
		//System.out.println(dateFormat.format(calendar.getTime()));

		assertTrue(calendar.get(Calendar.YEAR) == 1972);
		assertTrue(calendar.get(Calendar.MONTH) == 2);
		assertTrue(calendar.get(Calendar.DAY_OF_MONTH) == 4);
		assertTrue(calendar.get(Calendar.HOUR_OF_DAY) == 10);
		assertTrue(calendar.get(Calendar.MINUTE) == 5);
		assertTrue(calendar.get(Calendar.SECOND) == 20);
		assertTrue(calendar.get(Calendar.MILLISECOND) == 500);
	}

}
