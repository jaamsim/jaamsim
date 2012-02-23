/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
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

class EventTraceRecord extends ArrayList<String> {
	private String eventManagerName;
	private long internalTime;
	private String targetName;
	private String method;
	private int traceLevel;

	public EventTraceRecord() {
		traceLevel = 0;
	}

	public EventTraceRecord(FileEntity traceFile) {
		// Trace records read from an external file don't need the traceLevel as
		// they are read-only at that point
		traceLevel = 0;

		// Read a full trace record form the file, terminated at a blank line
		while (true) {
			String line = traceFile.readLine();

			if (line == null)
				break;

			this.add(line);

			if (line.length() == 0)
				break;
		}

		if (this.size() == 0) {
			return;
		}
		// Parse the key information from the record
		this.parse();

		//System.out.format("Read trace target:%s method:%s evt:%s time:%d\n",
		//				  targetName, method, eventManagerName, internalTime);
	}

	void clearLevel() {
		traceLevel = 0;
	}

	private void parse() {
		String[] temp;

		// The first line of the trace is always <eventManagerName>\t<InternalSimulationTime>
		temp = this.get(0).split("\t");
		eventManagerName = temp[0];
		internalTime = Long.parseLong(temp[1]);

		// Try to parse a target entity and method form the second line
		temp = this.get(1).split("\t");

		// A regular event wakeup, parse target/method
		if (temp[0].endsWith("Event")) {
			targetName = temp[3];
			method = temp[5];
			return;
		}

		// We need to look one line further for the Wait line that accompanies a
		// conditonal wait wakeup
		if (temp[0].endsWith("Event-WaitUntilEnded")) {
			temp = this.get(2).split("\t");
			targetName = temp[3];
			method = temp[5];
			return;
		}

		if (temp[0].endsWith("StartProcess")) {
			targetName = temp[1];
			method = temp[2];
			return;
		}

		if (temp[0].endsWith("SchedProcess")) {
			targetName = temp[3];
			method = temp[5];
			return;
		}
	}

	private void append(String record) {
		StringBuilder rec = new StringBuilder();

		for (int i = 0; i < traceLevel; i++) {
			rec.append("  ");
		}
		rec.append(record);
		this.add(rec.toString());
	}

	synchronized void addHeader(String name, long internalTime) {
		// Don't write anything if not at level 0
		if (traceLevel != 0)
			return;

		StringBuilder header = new StringBuilder(name).append("\t").append(internalTime);
		this.add(header.toString());
		traceLevel++;
	}

	synchronized void finish(Simulation simulation) {
		if(traceLevel != 1)
			return;

		this.add("");
		this.parse();
		simulation.processTraceData(this);
		this.clear();
		traceLevel--;
	}

	synchronized void formatEventTrace(Event evt, int reason) {
		final String[] eventStates = {"Wait", "Event", "Int", "Kill"};

		if (reason == 0)
			traceLevel--;

		this.append(String.format("%s\t%d\t%d\t%s\t%s\t%s",
					eventStates[reason],
					evt.eventTime, evt.priority, evt.caller.getName(),
					evt.caller.getInputName(), evt.getClassMethod()));

		if (reason == 1 || reason == 2)
			traceLevel++;
	}

	synchronized void formatWaitUntilTrace(int reason) {
		if (reason == 0) {
			traceLevel--;
			this.append("WaitUntil");
		} else {
			this.append("Event-WaitUntilEnded");
			traceLevel++;
		}
	}

	synchronized void formatProcessTrace(Entity target, String methodName) {
		if (target == null) {
			traceLevel--;
			this.append("Exit");
		} else {
			this.append(String.format("%s\t%s\t%s", "StartProcess", target.getName(), methodName));
			traceLevel++;
		}
	}

	synchronized void formatSchedProcessTrace(Event evt) {
		this.append(String.format("SchedProcess\t%d\t%d\t%s\t%s\t%s",
				evt.eventTime, evt.priority, evt.caller.getName(),
				evt.caller.getInputName(), evt.getClassMethod()));
	}

	boolean isDefaultEventManager() {
		return eventManagerName.equals("DefaultEventManager");
	}

	long getInternalTime() {
		return internalTime;
	}

	/**
	 * Does a superficial comparison of two records, check number of entries,
	 * time/target/method and finally the basic contents of the record.
	 */
	boolean basicCompare(EventTraceRecord record) {
		if (record.size() != this.size())
			return false;

		if (record.internalTime != this.internalTime)
			return false;

		if (!record.targetName.equals(this.targetName))
			return false;

		if (!record.method.equals(this.method))
			return false;

		return true;
	}
}
