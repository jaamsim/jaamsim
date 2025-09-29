/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.render.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jaamsim.basicsim.Log;
import com.jaamsim.ui.LogBox;

/**
 * This is a utility class that will keep statistical track of exceptions
 * @author matt.chudleigh
 *
 */
public class ExceptionLogger {

	private final Map<StackTraceElement, ExceptionCount> _exceptionStats;
	private int stackDumpThreshold;

	public ExceptionLogger(int dumpThreshold) {
		_exceptionStats = new HashMap<>();
		stackDumpThreshold = dumpThreshold;
	}

	private static final Comparator<ExceptionCount> sorter = new ExceptionCountComparator();
	private static class ExceptionCountComparator implements Comparator<ExceptionCount> {
		@Override
		public int compare(ExceptionCount arg0, ExceptionCount arg1) {
			int diff = arg1.count - arg0.count;
			if (diff < 0)
				return -1;
			else if (diff > 0)
				return 1;
			else
				return 0;
		}
	}

	private static class ExceptionCount {
		final StackTraceElement elem;
		int count;

		ExceptionCount(StackTraceElement ste) {
			elem = ste;
			count = 0;
		}
	}

	public void logException(Throwable t) {

		StackTraceElement[] callStack = t.getStackTrace();
		if (callStack.length <= 0) {
			return; // Something went oddly wrong here...
		}

		ExceptionCount counter = _exceptionStats.get(callStack[0]); // We only care about the original throw for now
		if (counter == null) {
			counter = new ExceptionCount(callStack[0]);
			_exceptionStats.put(counter.elem, counter);
		}

		counter.count++;
		if (counter.count == stackDumpThreshold) {
			Log.logException(t);
		}
	}

	public void printExceptionLog() {
		// Build up a list of the values to be sorted
		List<ExceptionCount> exceptions = new ArrayList<>(_exceptionStats.values());
		// Now sort it, then print it
		Collections.sort(exceptions, sorter);

		for (ExceptionCount e : exceptions) {
			StackTraceElement st = e.elem;
			int count = e.count;
			Log.logLine(st.getFileName() + ":" + st.getLineNumber() + " In: " + st.getClassName() + "." + st.getMethodName() + " " + count + " exceptions");
		}
	}


}
