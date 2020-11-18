/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019-2020 JaamSim Software Inc.
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

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Provides the option of a fixed 365 days per year calendar, without leap years.
 * @author Harry King
 *
 */
public final class SimCalendar extends GregorianCalendar {

	private boolean gregorian;  // true for the Gregorian calendar, false for a fixed 365 days/year

	private static final int epoch = 1970;

	private static final long millisPerSec = 1000;
	private static final long millisPerMin = 60 * millisPerSec;
	private static final long millisPerHr  = 60 * millisPerMin;
	private static final long millisPerDay = 24 * millisPerHr;
	private static final long millisPerYr  = 365 * millisPerDay;

	private static final int[] daysInMonth;
	private static final int[] firstDayOfMonth;

	static {
		daysInMonth = new int[12];
		daysInMonth[0] = 31;
		daysInMonth[1] = 28;
		daysInMonth[2] = 31;
		daysInMonth[3] = 30;
		daysInMonth[4] = 31;
		daysInMonth[5] = 30;
		daysInMonth[6] = 31;
		daysInMonth[7] = 31;
		daysInMonth[8] = 30;
		daysInMonth[9] = 31;
		daysInMonth[10] = 30;
		daysInMonth[11] = 31;

		firstDayOfMonth = new int[12];
		firstDayOfMonth[0] = 1;
		for (int i = 1; i < firstDayOfMonth.length; i++) {
			firstDayOfMonth[i] = firstDayOfMonth[i - 1] + daysInMonth[i - 1];
		}
	}

	public SimCalendar() {
		super(TimeZone.getTimeZone( "GMT" ));
	}

	/**
	 * Sets whether to use the usual Gregorian calendar with leap years or to use a simplified
	 * calendar with a fixed 365 days per year (no leap years).
	 * @param bool - true for the Gregorian calendar, false for the simple calendar
	 */
	public void setGregorian(boolean bool) {
		gregorian = bool;
	}

	public boolean isGregorian() {
		return gregorian;
	}

	/**
	 * Returns the month number for the given day of the year.
	 * Does not account for leap years.
	 * @param d - day of year (1 - 365)
	 * @return month number (0 - 11)
	 */
	public static int getMonthForDay(int d) {
		int k = Arrays.binarySearch(firstDayOfMonth, d);
		if (k >= 0)
			return k;
		return -k - 2;
	}

	/**
	 * Sets this Calendar's current time from the given long value.
	 * @param millis - time in milliseconds from the epoch
	 */
	@Override
	public void setTimeInMillis(long millis) {

		// Gregorian calendar
		if (gregorian) {
			super.setTimeInMillis(millis);
			return;
		}

		// Simple calendar with 365 days per year
		long years = Math.floorDiv(millis, millisPerYr);
		long millisInYear = millis - years*millisPerYr;  // millis in the present year (always > 0)
		long seconds = millisInYear / millisPerSec;
		long minutes = millisInYear / millisPerMin;
		long hours = millisInYear / millisPerHr;
		long days = millisInYear / millisPerDay;

		int dayOfYear = (int) (days % 365L) + 1;  // dayOfYear = 1 - 365;
		int month = getMonthForDay(dayOfYear);    // month = 0 - 11
		int dayOfMonth = dayOfYear - firstDayOfMonth[month] + 1;

		super.set(Calendar.YEAR, (int) years + epoch);
		super.set(Calendar.MONTH, month);
		super.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		super.set(Calendar.HOUR_OF_DAY, (int) (hours % 24L));
		super.set(Calendar.MINUTE, (int) (minutes % 60L));
		super.set(Calendar.SECOND, (int) (seconds % 60L));
		super.set(Calendar.MILLISECOND, (int) (millisInYear % 1000L));
	}

	/**
	 * Returns the time in milliseconds from the epoch corresponding to the specified date.
	 * @param year - year
	 * @param month - month (0 - 11)
	 * @param dayOfMonth - day of the month (1 - 31)
	 * @param hourOfDay - hour of the day (0 - 23)
	 * @param minute - minutes (0 - 59)
	 * @param second - seconds (0 - 59)
	 * @param millis - millisecond (0 - 999)
	 * @return time in milliseconds from the epoch
	 */
	public long getTimeInMillis(int year, int month, int dayOfMonth, int hourOfDay, int minute, int second, int millis) {

		// Gregorian calendar
		if (gregorian) {
			super.set(year, month, dayOfMonth, hourOfDay, minute, second);
			super.set(Calendar.MILLISECOND, millis);
			return super.getTimeInMillis();
		}

		// Simple calendar with 365 days per year
		long ret = 0;
		ret += (year - epoch) * millisPerYr;
		ret += (firstDayOfMonth[month] - 1) * millisPerDay;
		ret += (dayOfMonth - 1) * millisPerDay;
		ret += hourOfDay * millisPerHr;
		ret += minute * millisPerMin;
		ret += second * millisPerSec;
		ret += millis;
		return ret;
	}

	/**
	 * Returns the SimDate containing the date and time data.
	 * @return SimDate
	 */
	public SimDate getSimDate() {
		return new SimDate(this);
	}

}
