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

class EventTraceRecord extends ArrayList<String> implements EventTraceListener {
	private long internalTime;
	private String targetName;
	int traceLevel;

	public EventTraceRecord() {
		traceLevel = 0;
	}

	void parse() {
		String[] temp;

		// The first line of the trace is always Event\ttime
		temp = this.get(0).split("\t");
		internalTime = Long.parseLong(temp[1]);

		// A regular event wakeup, parse target/method
		if (temp[0].equals("Event")) {
			targetName = temp[3];
			return;
		}

		throw new ErrorException("All events must start with an event record");
	}

	private void append(String record) {
		StringBuilder rec = new StringBuilder();

		for (int i = 0; i < traceLevel; i++) {
			rec.append("\t");
		}
		rec.append(record);
		this.add(rec.toString());
	}

	@Override
	public final void traceWait(long tick, int priority, ProcessTarget t) {
		traceLevel--;
		this.append(String.format("Wait\t%d\t%d\t%s", tick, priority, getWaitDescription()));
	}

	@Override
	public final void traceEvent(long tick, int priority, ProcessTarget t) {
		this.append(String.format("Event\t%d\t%d\t%s", tick, priority, t.getDescription()));
		traceLevel++;
	}

	@Override
	public final void traceInterrupt(long tick, int priority, ProcessTarget t) {
		this.append(String.format("Int\t%d\t%d\t%s", tick, priority, t.getDescription()));
		traceLevel++;
	}

	@Override
	public final void traceKill(long tick, int priority, ProcessTarget t) {
		this.append(String.format("Kill\t%d\t%d\t%s", tick, priority, t.getDescription()));
	}

	@Override
	public final void traceWaitUntil() {
		traceLevel--;
		this.append("WaitUntil");
	}

	@Override
	public final void traceSchedUntil(ProcessTarget t) {
		this.append(String.format("SchedUntil\t%s", t.getDescription()));
	}

	@Override
	public final void traceProcessStart(ProcessTarget t) {
		this.append(String.format("StartProcess\t%s", t.getDescription()));
		traceLevel++;
	}

	@Override
	public final void traceProcessEnd() {
		traceLevel--;
		this.append("Exit");
	}

	@Override
	public final void traceSchedProcess(long tick, int priority, ProcessTarget t) {
		this.append(String.format("SchedProcess\t%d\t%d\t%s", tick, priority, t.getDescription()));
	}

	@Override
	public final void traceConditionalEval(ProcessTarget t) {}

	@Override
	public final void traceConditionalEvalEnded(boolean wakeup, ProcessTarget t) {
		//if (!wakeup)
		//	return;
		//EventManager e = EventManager.current();
		//this.addHeader(e.name, e.getTicks());
		//this.append(String.format("WaitUntilEnded\t%s", t.getDescription()));
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

	private static final String entClassName = Entity.class.getName();
	private static final String evtManClassName = EventManager.class.getName();
	static String getWaitDescription() {
		StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
		int evtManIdx = -1;
		// walk out of any EventManager methods
		for (int i = 0; i < callStack.length; i++) {
			if (callStack[i].getClassName().equals(evtManClassName)) {
				evtManIdx = i;
				continue;
			}

			// we have walked through the eventManager methods
			if (evtManIdx != -1)
				break;
		}

		// walk past any Entity methods
		int entIdx = -1;
		for (int i = evtManIdx + 1; i < callStack.length; i++) {
			if (callStack[i].getClassName().equals(entClassName)) {
				entIdx = i;
				continue;
			}

			break;
		}

		StackTraceElement elem;
		if (entIdx > -1)
			elem = callStack[entIdx + 1];
		else
			elem = callStack[evtManIdx + 1];

		return String.format("%s:%s", elem.getClassName(), elem.getMethodName());
	}
}
