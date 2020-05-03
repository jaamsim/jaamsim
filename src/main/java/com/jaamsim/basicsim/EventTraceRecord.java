/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;

import com.jaamsim.events.EventManager;
import com.jaamsim.events.EventTraceListener;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.Input;

class EventTraceRecord extends ArrayList<String> implements EventTraceListener {
	private String eventManagerName;
	private long internalTime;
	private String targetName;
	int traceLevel;

	public EventTraceRecord() {
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
			return;
		}

		if (temp[0].endsWith("WaitUntilEnded")) {
			targetName = temp[3];
			return;
		}

		if (temp[0].endsWith("StartProcess")) {
			targetName = temp[1];
			return;
		}

		if (temp[0].endsWith("SchedProcess")) {
			targetName = temp[3];
			return;
		}
	}

	private void append(String record) {
		StringBuilder rec = new StringBuilder();

		for (int i = 0; i < traceLevel; i++) {
			rec.append(Input.SEPARATOR);
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

	@Override
	public void traceWait(long tick, int priority, ProcessTarget t) {
		this.addHeader(EventManager.current().name, EventManager.current().getTicks());
		traceLevel--;

		this.append(String.format("Wait\t%d\t%d\t%s", tick, priority, EventRecorder.getWaitDescription()));
	}

	@Override
	public void traceEvent(long tick, int priority, ProcessTarget t) {
		this.addHeader(EventManager.current().name, tick);
		this.append(String.format("Event\t%d\t%d\t%s", tick, priority, t.getDescription()));

		traceLevel++;
	}

	@Override
	public void traceInterrupt(long tick, int priority, ProcessTarget t) {
		this.addHeader(EventManager.current().name, EventManager.current().getTicks());
		this.append(String.format("Int\t%d\t%d\t%s", tick, priority, t.getDescription()));
		traceLevel++;
	}

	@Override
	public void traceKill(long tick, int priority, ProcessTarget t) {
		this.addHeader(EventManager.current().name, EventManager.current().getTicks());
		this.append(String.format("Kill\t%d\t%d\t%s", tick, priority, t.getDescription()));
	}

	@Override
	public void traceWaitUntil() {
		this.addHeader(EventManager.current().name, EventManager.current().getTicks());
		traceLevel--;
		this.append("WaitUntil");
	}

	@Override
	public void traceSchedUntil(ProcessTarget t) {
		this.addHeader(EventManager.current().name, EventManager.current().getTicks());
		traceLevel--;
		this.append(String.format("SchedUntil\t%s", t.getDescription()));
	}

	@Override
	public void traceProcessStart(ProcessTarget t) {
		EventManager e = EventManager.current();
		this.addHeader(e.name, e.getTicks());
		this.append(String.format("StartProcess\t%s", t.getDescription()));
		traceLevel++;
	}

	@Override
	public void traceProcessEnd() {
		EventManager e = EventManager.current();
		this.addHeader(e.name, e.getTicks());
		traceLevel--;
		this.append("Exit");
	}

	@Override
	public void traceSchedProcess(long tick, int priority, ProcessTarget t) {
		EventManager e = EventManager.current();
		this.addHeader(e.name, e.getTicks());
		this.append(String.format("SchedProcess\t%d\t%d\t%s", tick, priority, t.getDescription()));
	}

	@Override
	public void traceConditionalEval(ProcessTarget t) {}

	@Override
	public void traceConditionalEvalEnded(boolean wakeup, ProcessTarget t) {
		if (!wakeup)
			return;
		EventManager e = EventManager.current();
		this.addHeader(e.name, e.getTicks());
		this.append(String.format("WaitUntilEnded\t%s", t.getDescription()));
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

		return true;
	}
}
