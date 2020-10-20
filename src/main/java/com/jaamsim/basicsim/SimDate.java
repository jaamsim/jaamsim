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

import java.util.Calendar;

public class SimDate {

	public final int year;
	public final int month;
	public final int dayOfMonth;
	public final int hourOfDay;
	public final int minute;
	public final int second;
	public final int millisecond;

	public SimDate(int YY, int MM, int DD) {
		this(YY, MM, DD, 0, 0, 0, 0);
	}

	public SimDate(int YY, int MM, int DD, int hh, int mm, int ss, int ms) {
		year = YY;
		month = MM;
		dayOfMonth = DD;
		hourOfDay = hh;
		minute = mm;
		second = ss;
		millisecond = ms;
	}

	public SimDate(Calendar calendar) {
		year = calendar.get(Calendar.YEAR);
		month = calendar.get(Calendar.MONTH) + 1;
		dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
		hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
		minute = calendar.get(Calendar.MINUTE);
		second = calendar.get(Calendar.SECOND);
		millisecond = calendar.get(Calendar.MILLISECOND);
	}

	public int[] toArray() {
		return new int[]{year, month, dayOfMonth, hourOfDay, minute, second, millisecond};
	}

	@Override
	public String toString() {
		if (hourOfDay == 0 && minute == 0 && second == 0 && millisecond == 0)
			return String.format("%04d-%02d-%02d", year, month, dayOfMonth);

		return String.format("%04d-%02d-%02d %02d:%02d:%02d.%s",
				year, month, dayOfMonth, hourOfDay, minute, second, millisecond);
	}

}
