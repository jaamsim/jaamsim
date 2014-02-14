/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.render.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jaamsim.ui.LogBox;

/**
 * This is a utility class that will keep statistical track of exceptions
 * @author matt.chudleigh
 *
 */
public class ExceptionLogger {

	private final Map<StackTraceElement, Integer> _exceptionStats;
	private int stackDumpThreshold;

	public ExceptionLogger(int dumpThreshold) {
		_exceptionStats = new HashMap<StackTraceElement, Integer>();
		stackDumpThreshold = dumpThreshold;
	}

	// Sort exceptions list in descending order (most common first)
	private static class ExceptionSorter implements Comparator<Map.Entry<StackTraceElement, Integer>> {

		@Override
		public int compare(Entry<StackTraceElement, Integer> arg0,
				Entry<StackTraceElement, Integer> arg1) {
			return arg1.getValue().compareTo(arg0.getValue());
		}

	}

	public void logException(Throwable t) {

		StackTraceElement[] callStack = t.getStackTrace();
		if (callStack.length <= 0) {
			return; // Something went oddly wrong here...
		}

		StackTraceElement key = callStack[0]; // We only care about the original throw for now
		Integer count = _exceptionStats.get(key);
		if (count == null) {
			// First time
			count = new Integer(0);
		}

		if (count + 1 == stackDumpThreshold) {
			LogBox.renderLogException(t);
		}

		//Otherwise increment the count for this element
		_exceptionStats.put(key, count + 1);
	}

	public void printExceptionLog() {
		// Build up a list of the values to be sorted
		List<Map.Entry<StackTraceElement, Integer>> exceptions = new ArrayList<Map.Entry<StackTraceElement, Integer>>(_exceptionStats.entrySet());
		// Now sort it, then print it
		Collections.sort(exceptions, new ExceptionSorter());

		for (Map.Entry<StackTraceElement, Integer> e : exceptions) {
			StackTraceElement st = e.getKey();
			int count = e.getValue();
			LogBox.renderLog(st.getFileName() + ":" + st.getLineNumber() + " In: " + st.getClassName() + "." + st.getMethodName() + " " + count + " exceptions");
		}
	}


}
