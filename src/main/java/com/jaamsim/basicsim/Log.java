/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) Harvey Harrison
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

/**
 * Simple logger for global log not tied to any particular model.
 */
public class Log {
	private static final ArrayList<String> log = new ArrayList<>();

	public static void format(String format, Object... args) {
		logLine(String.format(format, args));
	}

	public static void logLine(String line) {
		synchronized (log) {
			log.add(line);
		}
	}

	public static void logException(Throwable ex) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		pw.flush();

		String stackTrace = sw.toString();
		logLine(stackTrace);

		System.err.println(stackTrace);
	}

	public static String getLog(int fromIdx) {
		StringBuilder sb = new StringBuilder();
		synchronized (log) {
			for (int i = fromIdx; i < log.size(); i++) {
				sb.append(log.get(i));
				sb.append("\n");
			}
		}
		return sb.toString();
	}
}
