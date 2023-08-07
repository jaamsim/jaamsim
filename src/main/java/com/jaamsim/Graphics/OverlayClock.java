/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2021-2023 JaamSim Software Inc.
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

import com.jaamsim.Samples.SampleInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
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
	private final SampleInput startingYear;

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

		startingYear = new SampleInput("StartingYear", KEY_INPUTS, 2000);
		startingYear.setIntegerValue(true);
		startingYear.setHidden(true);  // not used
		this.addInput(startingYear);

		dateFormatInput = new StringInput("DateFormat", KEY_INPUTS, "yyyy-MMM-dd HH:mm:ss.SSS");
		dateFormatInput.setCallback(inputCallback);
		this.addInput(dateFormatInput);
	}

	public OverlayClock() {
		dateFormat = new SimpleDateFormat(dateFormatInput.getValue());
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	static final InputCallback inputCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((OverlayClock)ent).updateInputValue();
		}
	};

	void updateInputValue() {
		dateFormat = new SimpleDateFormat(dateFormatInput.getValue());
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		startingYear.reset();  // Delete an unnecessary input
	}

	@Override
	public String getRenderText(double simTime) {
		long millis = getJaamSimModel().simTimeToCalendarMillis(simTime);
		Date date = getJaamSimModel().getCalendarDate(millis);
		return dateFormat.format(date);
	}

}
