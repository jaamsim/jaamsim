/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation3D;

import com.sandwell.JavaSimulation.InputErrorException;

/**
 * Class to implement Audition-style clock calculations. Re-implementing the
 * class digitalclock.
 */
public class Clock{

	private static final int[] firstDayOfMonth;
	private static final int[] daysInMonth;
	private static final int[] secsInMonth;
	private static final String monthNames[];

	private static int startingYear;
	private static int startingMonth;
	private static int startingDay;

	private static final double HOURS_PER_YEAR = 8760.0d;
	private static final double HOURS_PER_DAY = 24.0d;

	static {
		firstDayOfMonth = new int[13];
		firstDayOfMonth[0] = 1;
		firstDayOfMonth[1] = 32;
		firstDayOfMonth[2] = 60;
		firstDayOfMonth[3] = 91;
		firstDayOfMonth[4] = 121;
		firstDayOfMonth[5] = 152;
		firstDayOfMonth[6] = 182;
		firstDayOfMonth[7] = 213;
		firstDayOfMonth[8] = 244;
		firstDayOfMonth[9] = 274;
		firstDayOfMonth[10] = 305;
		firstDayOfMonth[11] = 335;
		firstDayOfMonth[12] = 366;

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

		secsInMonth = new int[12];
		for (int i = 0; i < daysInMonth.length; i++)
			secsInMonth[i] = daysInMonth[i] * 24 * 60 * 60;

		monthNames = new String[] { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
	}

	public static void setStartDate(int year, int month, int day) {
		startingYear = year;
		startingMonth = month;
		startingDay = day;
	}

public static class ClockTime {
	public final int year;
	public final int month;
	public final int day;
	public final double hour;

	public ClockTime(int y, int m, int d, double h) {
		year = y;
		month = m;
		day = d;
		hour = h;
	}
}
	public static ClockTime getClockTime(double time) {
		int lyear = (int)Math.floor( time / 8760.0 );

		double remainder = time % 8760.0;

		int lday = (int)Math.floor( remainder / 24.0 );
		lday++;

		double lhour = remainder % 24.0;

		// Calculate what month this is
		int lmonth = getMonthForDay( lday );

		lday -= firstDayOfMonth[lmonth - 1];

		return new ClockTime(lyear + 1, lmonth, lday + 1, lhour);
	}

	/**
	 * Return the day of the year (1-365) that corresponds to the first day of the given month (1-12).
	 */
	public static int getFirstDayOfMonth(int month) {
		return firstDayOfMonth[month-1];
	}

	public static double calcTimeForYear_Month_Day_Hour( int y, int m, int d, double h ) {
		double result = (y - 1) * 8760.0; // year
		result += ((firstDayOfMonth[m - 1] - 1) * 24.0); // months to this point
		result += (d - 1) * 24.0; // days into this month
		result += h; // hours into this day
		return result;
	}

	public static int getYearForTime( double time ) {
		return (int)Math.floor( time / 8760.0 ) + 1;
	}

	public static int getHourForTime( double time ) {
		return (int)Math.floor( time % 24.0 );
	}

	public static int getMinuteForTime( double time ) {
		double temp = time - Math.floor( time );
		return (int)Math.floor( temp * 60.0 );
	}

	public static int getSecondForTime( double time ) {
		double temp = time - Math.floor( time );
		temp = temp * 60.0 ;
		temp = temp - Math.floor( temp );
		return (int)Math.round( temp * 60.0 );

	}

	/**
	 * Return the month 1-12 for the given day of the year 1-365
	 */
	public static int getMonthForDay( int d ) {
		for (int i = 1; i < firstDayOfMonth.length; i++) {
			if (d < firstDayOfMonth[i]) {
				return i;
			}
		}
		return 12;
	}

	public static int getMonthForTime(double hours) {
		// Add 1 to make the first day of year be 1 rather than 0
		int day = (int)(((hours % HOURS_PER_YEAR) / HOURS_PER_DAY) + 1.0d);

		return getMonthForDay(day);
	}

	public static int getMonthIndex(double hours) {
		// Add 1 to make the first day of year be 1 rather than 0
		int day = (int)(((hours % HOURS_PER_YEAR) / HOURS_PER_DAY) + 1.0d);

		for (int i = 1; i < firstDayOfMonth.length; i++) {
			if (day < firstDayOfMonth[i]) {
				return i - 1;
			}
		}
		return 11;
	}

	public static int getNextMonthForTime(double hours) {
		int month = getMonthForTime(hours);
		return (month % 12) + 1;
	}

	public static double getDecHour(double time) {
		return time % 24.0;
	}

	public static int getStartingYear() {
		return startingYear;
	}

	public static int getStartingMonth() {
		return startingMonth;
	}

	public static int getStartingDay() {
		return startingDay;
	}

	/**
	 * Return the number of days in the given month 1-12
	 */
	public static int getDaysInMonth( int m ) {
		return daysInMonth[m - 1];
	}

	/**
	 * Return the number of seconds in the given month index 0-11
	 */
	public static int getSecsInMonthIdx(int idx) {
		return secsInMonth[idx];
	}

	public static void getStartingDateFromString( String startingDate ) {

		String[] startingVal = startingDate.split( "-" );

		if( startingVal.length < 3 ) {
			throw new InputErrorException( "Starting date string not formatted correctly" );
		}

		startingYear = Integer.parseInt( startingVal[0] );
		startingMonth = Integer.parseInt( startingVal[1] );
		startingDay = Integer.parseInt( startingVal[2] );
	}

	/**
	 * Returns a formatted string to reflect the current simulation time.
	 */
	public static String getDateStringForTime(double time) {
		ClockTime cTime = getClockTime(time);
		int y = cTime.year + startingYear - 1;

		return String.format("%04d-%s-%02d  %02d:%02d", y, monthNames[cTime.month - 1], cTime.day, getHourForTime( time ), getMinuteForTime( time ));
	}

	/**
	 * Returns a formatted string to reflect the given year, month, day, and hour
	 */
	public static String getDateString( int y, int m, int d, double h) {

		int hr = getHourForTime( h );
		int min = getMinuteForTime( h );

		return String.format("%04d-%02d-%02d %02d:%02d", y, m, d, hr, min);
	}
}
