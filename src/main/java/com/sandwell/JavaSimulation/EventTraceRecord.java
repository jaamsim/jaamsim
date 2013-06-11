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

	void clearLevel() {
		traceLevel = 0;
	}

	void parse() {
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

	private void addHeader(String name, long internalTime) {
		// Don't write anything if not at level 0
		if (traceLevel != 0)
			return;

		StringBuilder header = new StringBuilder(name).append("\t").append(internalTime);
		this.add(header.toString());
		traceLevel++;
	}

	private void finish() {
		if(traceLevel != 1)
			return;

		this.add("");
		this.parse();
		EventTracer.processTraceData(this);
		this.clear();
		traceLevel--;
	}

	private static final String[] eventStates = {"Wait", "Event", "Int", "Kill"};
	synchronized void formatEventTrace(String name, Event evt, int reason) {
		this.addHeader(name, evt.schedTick);
		if (reason == 0)
			traceLevel--;

		this.append(String.format("%s\t%d\t%d\t%s\t%s\t%s",
					eventStates[reason],
					evt.schedTick, evt.priority, evt.caller.getName(),
					evt.caller.getInputName(), evt.getClassMethod()));

		if (reason == 1 || reason == 2)
			traceLevel++;

		this.finish();
	}

	synchronized void formatWaitUntilTrace(String name, long currentTime, int reason) {
		this.addHeader(name, currentTime);
		if (reason == 0) {
			traceLevel--;
			this.append("WaitUntil");
		} else {
			this.append("Event-WaitUntilEnded");
			traceLevel++;
		}
		this.finish();
	}

	synchronized void formatProcessTrace(String name, long currentTime, Entity target, String methodName) {
		this.addHeader(name, currentTime);
		if (target == null) {
			traceLevel--;
			this.append("Exit");
		} else {
			this.append(String.format("%s\t%s\t%s", "StartProcess", target.getName(), methodName));
			traceLevel++;
		}
		this.finish();
	}

	synchronized void formatSchedProcessTrace(String name, long currentTime, Event evt) {
		this.addHeader(name, currentTime);
		this.append(String.format("SchedProcess\t%d\t%d\t%s\t%s\t%s",
				evt.schedTick, evt.priority, evt.caller.getName(),
				evt.caller.getInputName(), evt.getClassMethod()));
		this.finish();
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
