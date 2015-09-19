/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.Graphics;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import com.jaamsim.input.Input;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;

/**
 * An overlay display of time and/or date.
 * @author Harry King
 *
 */
public class OverlayClock extends OverlayText {

	@Keyword(description = "The year in which the simulation will begin.",
	         exampleList = {"2000"})
	private final IntegerInput startingYear;

	@Keyword(description = "The Java date format in which the date and time are to be displayed.  " +
			"If spaces are included, enclose the text in single quotes.  " +
			 "e.g. 'yyyy-MMM-dd H:mm:ss.SSS'",
	         exampleList = {"'yyyy-MMM-dd HH:mm:ss.SSS'"})
	protected final StringInput dateFormatInput;

	private Calendar calendar;
	private SimpleDateFormat dateFormat;

	private long year;
	private long dayOfYear;
	private long hour;
	private long minute;
	private long second;
	private long milli;

	private int month;
	private int dayOfMonth;

	{
		formatText.setHidden(true);
		dataSource.setHidden(true);
		unitType.setHidden(true);
		unit.setHidden(true);

		startingYear = new IntegerInput("StartingYear", "Key Inputs", 2000);
		this.addInput(startingYear);

		dateFormatInput = new StringInput("DateFormat", "Key Inputs", "yyyy-MMM-dd HH:mm:ss.SSS");
		this.addInput(dateFormatInput);
	}

	public OverlayClock() {
		calendar = Calendar.getInstance();
		calendar.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
		dateFormat = new SimpleDateFormat(dateFormatInput.getValue());
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if( in == dateFormatInput ) {
			dateFormat = new SimpleDateFormat(dateFormatInput.getValue());
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		}
	}

	private static final int[] firstDayOfMonth;
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
	}

	/**
	 * Return the month 1-12 for the given day of the year 1-365
	 */
	private static int getMonthForDay( int d ) {
		for (int i = 1; i < firstDayOfMonth.length; i++) {
			if (d < firstDayOfMonth[i]) {
				return i;
			}
		}
		return 12;
	}

	/**
	 * Return the day of the year (1-365) that corresponds to the first day of the given month (1-12).
	 */
	private static int getFirstDayOfMonth(int month) {
		return firstDayOfMonth[month-1];
	}

	@Override
	public String getRenderText(double simTime) {

		double time = simTime / 3600.0d;  // time in hours

		year = (long)Math.floor(time/8760.0d);
		dayOfYear = (long)Math.floor(time/24.0d) % 365 + 1;  // dayOfYear = 1 - 365;
		hour = (long)Math.floor(time) % 24;
		minute = (long)Math.floor(time*60.0d) % 60;
		second = (long)Math.floor(simTime) % 60;
		milli = (long)Math.floor(simTime*1000.0d) % 1000;

		month = getMonthForDay((int) dayOfYear);       // month = 1 - 12
		dayOfMonth = (int)dayOfYear - getFirstDayOfMonth(month) + 1;

		calendar.set(Calendar.YEAR, (int) year + startingYear.getValue());
		calendar.set(Calendar.MONTH, month - 1);  // Java months are 0 - 11
		calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		calendar.set(Calendar.HOUR_OF_DAY, (int) hour);
		calendar.set(Calendar.MINUTE, (int) minute);
		calendar.set(Calendar.SECOND, (int) second);
		calendar.set(Calendar.MILLISECOND, (int) milli);

		return dateFormat.format(calendar.getTime());
	}

}
