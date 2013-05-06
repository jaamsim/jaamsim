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

import java.util.ArrayList;

import com.jaamsim.input.InputAgent;

class EventTracer {
	private static FileEntity eventTraceFile;
	private static FileEntity eventVerifyFile;
	private static long bufferTime; // Internal sim time buffer has been filled to
	private static ArrayList<EventTraceRecord> eventBuffer;

	private EventTracer() {}


	static void init() {
		if (eventVerifyFile == null)
			return;

		eventVerifyFile.toStart();
		eventBuffer.clear();
		bufferTime = 0;
	}

	private static void fillBufferUntil(long internalTime) {
		while (bufferTime <= internalTime) {
			EventTraceRecord temp = new EventTraceRecord(eventVerifyFile);

			// reached end of verify file, don't add an empty record
			if (temp.size() == 0) {
				break;
			}
			if (temp.isDefaultEventManager() && temp.getInternalTime() > bufferTime) {
				bufferTime = temp.getInternalTime();
			}
			eventBuffer.add(temp);
		}
	}

	static void traceAllEvents(boolean enable) {
		if (enable) {
			verifyAllEvents(false);
			eventTraceFile = new FileEntity(InputAgent.getRunName() + ".evt", FileEntity.FILE_WRITE, false);
		} else if (eventTraceFile != null) {
			eventTraceFile.close();
			eventTraceFile = null;
		}

		EventManager.rootManager.traceEvents = enable;
	}

	static void verifyAllEvents(boolean enable) {
		if (enable) {
			traceAllEvents(false);
			eventBuffer = new ArrayList<EventTraceRecord>();
			bufferTime = 0;
			eventVerifyFile = new FileEntity(InputAgent.getRunName() + ".evt", FileEntity.FILE_READ, false);
		} else if (eventVerifyFile != null) {
			eventVerifyFile.close();
			eventVerifyFile = null;
		}

		EventManager.rootManager.traceEvents = enable;
	}

	private static void findEventInBuffer(EventTraceRecord record) {
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

					Simulation.pause();
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
		Simulation.pause();
	}

	static void processTraceData(EventTraceRecord traceRecord) {
		if (eventTraceFile != null) {
			synchronized (eventTraceFile) {
				for (String each : traceRecord) {
					eventTraceFile.putString(each);
					eventTraceFile.newLine();
				}
				eventTraceFile.flush();
			}
		}

		if (eventVerifyFile != null) {
			synchronized (eventVerifyFile) {
				EventTracer.fillBufferUntil(traceRecord.getInternalTime());
				EventTracer.findEventInBuffer(traceRecord);
			}
		}
	}
}
