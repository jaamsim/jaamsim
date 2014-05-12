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
package com.sandwell.JavaSimulation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.jaamsim.events.EventManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.ui.LogBox;

class EventTracer {
	private static FileEntity eventTraceFile;
	private static BufferedReader eventVerifyReader;
	private static long bufferTime; // Internal sim time buffer has been filled to
	private static final ArrayList<EventTraceRecord> eventBuffer;

	static {
		eventBuffer = new ArrayList<EventTraceRecord>();
	}

	private EventTracer() {}

	static void init() {
		eventBuffer.clear();
		bufferTime = 0;
	}

	private static void fillBufferUntil(long internalTime) {
		while (bufferTime <= internalTime) {
			// Read a full trace record form the file, terminated at a blank line
			EventTraceRecord temp = new EventTraceRecord();
			while (true) {
				String line = null;
				try {
					line = eventVerifyReader.readLine();
				}
				catch (IOException e) {}

				if (line == null)
					break;

				temp.add(line);

				if (line.length() == 0)
					break;
			}

			if (temp.size() == 0)
				break;

			// Parse the key information from the record
			temp.parse();
			if (temp.isDefaultEventManager() && temp.getInternalTime() > bufferTime) {
				bufferTime = temp.getInternalTime();
			}
			eventBuffer.add(temp);
		}
	}

	static void traceAllEvents(EventManager evt, boolean enable) {
		if (enable) {
			verifyAllEvents(evt, false);
			eventTraceFile = new FileEntity(InputAgent.getConfigFile().getParentFile()+ File.separator+ InputAgent.getRunName() + ".evt");
			evt.setTraceListener(new EventTraceRecord());
		} else if (eventTraceFile != null) {
			eventTraceFile.close();
			eventTraceFile = null;
			evt.setTraceListener(null);
		}
	}

	static void verifyAllEvents(EventManager evt, boolean enable) {
		if (enable) {
			traceAllEvents(evt, false);
			eventBuffer.clear();
			bufferTime = 0;
			File evtFile = new File(InputAgent.getConfigFile().getParentFile(), InputAgent.getRunName() + ".evt");
			try {
				eventVerifyReader = new BufferedReader(new FileReader(evtFile));
			}
			catch (FileNotFoundException e) {}
			if (eventVerifyReader == null)
				LogBox.logLine("Unable to open an event verification file.");
			evt.setTraceListener(new EventTraceRecord());
		} else if (eventVerifyReader != null) {
			try {
				eventVerifyReader.close();
			}
			catch (IOException e) {}
			eventVerifyReader = null;
			evt.setTraceListener(null);
		}
	}

	private static void findEventInBuffer(EventManager e, EventTraceRecord record) {
		// Ensure we have read enough from the log to find this record
		EventTracer.fillBufferUntil(record.getInternalTime());

		// Try an optimistic approach first looking for exact matches
		for (EventTraceRecord each : eventBuffer) {
			if (!each.basicCompare(record)) {
				continue;
			}

			for (int i = 1; i < record.size(); i++) {
				if (!record.get(i).equals(each.get(i))) {
					System.out.println("Difference in event stream detected");
					System.out.println("Received:");
					for (String line : record) {
						System.out.println(line);
					}

					System.out.println("Expected:");
					for (String line : each) {
						System.out.println(line);
					}

					System.out.println("Lines:");
					System.out.println("R:" + record.get(i));
					System.out.println("E:" + each.get(i));

					e.pause();
					new Throwable().printStackTrace();
					break;
				}
			}

			// Found the event, it compared OK, remove from the buffer
			eventBuffer.remove(each);
			//System.out.println("Buffersize:" + eventBuffer.size());
			return;
		}

		System.out.println("No matching event found for:");
		for (String line : record) {
			System.out.println(line);
		}
		for (EventTraceRecord rec : eventBuffer) {
			System.out.println("Buffered Record:");
			for (String line : rec) {
				System.out.println(line);
			}
			System.out.println();
		}
		e.pause();
	}

	private static void writeEventToBuffer(EventTraceRecord record) {
		for (String each : record) {
			eventTraceFile.putString(each);
			eventTraceFile.newLine();
		}
		eventTraceFile.flush();
	}

	static void processTraceData(EventManager e, EventTraceRecord traceRecord) {
		if (eventTraceFile != null) {
			synchronized (eventTraceFile) {
				EventTracer.writeEventToBuffer(traceRecord);
			}
		}

		if (eventVerifyReader != null) {
			synchronized (eventVerifyReader) {
				EventTracer.findEventInBuffer(e, traceRecord);
			}
		}
	}
}
