/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.Graphics;

import java.text.SimpleDateFormat;
import java.util.Date;
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

	private SimpleDateFormat dateFormat;

	{
		formatText.setHidden(true);
		dataSource.setHidden(true);
		unitType.setHidden(true);
		unit.setHidden(true);

		startingYear = new IntegerInput("StartingYear", KEY_INPUTS, 2000);
		this.addInput(startingYear);

		dateFormatInput = new StringInput("DateFormat", KEY_INPUTS, "yyyy-MMM-dd HH:mm:ss.SSS");
		this.addInput(dateFormatInput);
	}

	public OverlayClock() {
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

	@Override
	public String getRenderText(double simTime) {
		double startTime = (startingYear.getValue() - 1970) * 8760.0d * 3600.0d;
		long millis = getJaamSimModel().simTimeToCalendarMillis(startTime + simTime);
		Date date = getJaamSimModel().getCalendarDate(millis);
		return dateFormat.format(date);
	}

}
