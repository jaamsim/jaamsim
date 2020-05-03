/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.jaamsim.events.EventManager;
import com.jaamsim.events.EventTraceListener;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;

public class EventRecorder implements EventTraceListener {
	private BufferedWriter outputStream;
	private int traceLevel;
	private final ArrayList<String> traces = new ArrayList<>();

	public EventRecorder(String fileName) {
		traceLevel = 0;
		try {
			File backingFileObject = new File(fileName);
			backingFileObject.createNewFile();
			outputStream = new BufferedWriter(new FileWriter(backingFileObject, false));
		}
		catch (IOException e) {
			throw new InputErrorException("IOException thrown trying to open FileEntity: " + e);
		}
		catch (IllegalArgumentException e) {
			throw new InputErrorException("IllegalArgumentException thrown trying to open File (Should not happen): " + e);
		}
		catch (SecurityException e) {
			throw new InputErrorException("SecurityException thrown trying to open File: " + e);
		}
	}

	private void append(String record) {
		StringBuilder rec = new StringBuilder();

		for (int i = 0; i < traceLevel; i++) {
			rec.append(Input.SEPARATOR);
		}
		rec.append(record);
		traces.add(rec.toString());
	}

	private void addHeader(String name, long internalTime) {
		// Don't write anything if not at level 0
		if (traceLevel != 0)
			return;

		StringBuilder header = new StringBuilder(name).append("\t").append(internalTime);
		traces.add(header.toString());
		traceLevel++;
	}

	private void finish(EventManager e) {
		if(traceLevel != 1)
			return;

		traces.add("");
		for (String each : traces) {
			try {
				outputStream.write( each );
				outputStream.newLine();
			}
			catch( IOException ioe ) {}
		}
		try {
			outputStream.flush();
		}
		catch( IOException ioe ) {}

		traces.clear();
		traceLevel--;
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

	@Override
	public synchronized void traceWait(EventManager e, long curTick, long tick, int priority, ProcessTarget t) {
		this.addHeader(e.name, curTick);
		traceLevel--;

		this.append(String.format("Wait\t%d\t%d\t%s", tick, priority, getWaitDescription()));

		this.finish(e);
	}

	@Override
	public synchronized void traceEvent(EventManager e, long curTick, long tick, int priority, ProcessTarget t) {
		this.addHeader(e.name, curTick);
		this.append(String.format("Event\t%d\t%d\t%s", tick, priority, t.getDescription()));
		traceLevel++;
		this.finish(e);
	}

	@Override
	public synchronized void traceInterrupt(EventManager e, long curTick, long tick, int priority, ProcessTarget t) {
		this.addHeader(e.name, curTick);
		this.append(String.format("Int\t%d\t%d\t%s", tick, priority, t.getDescription()));
		traceLevel++;
		this.finish(e);
	}

	@Override
	public synchronized void traceKill(EventManager e, long curTick, long tick, int priority, ProcessTarget t) {
		this.addHeader(e.name, curTick);
		this.append(String.format("Kill\t%d\t%d\t%s", tick, priority, t.getDescription()));
		this.finish(e);
	}

	@Override
	public synchronized void traceWaitUntil(EventManager e, long tick) {
		this.addHeader(e.name, tick);
		traceLevel--;
		this.append("WaitUntil");
		this.finish(e);
	}

	@Override
	public synchronized void traceSchedUntil(EventManager e, long tick) {
		this.addHeader(e.name, tick);
		traceLevel--;
		this.append("SchedUntil");
		this.finish(e);
	}

	@Override
	public synchronized void traceProcessStart(EventManager e, ProcessTarget t, long tick) {
		this.addHeader(e.name, tick);
		this.append(String.format("StartProcess\t%s", t.getDescription()));
		traceLevel++;
		this.finish(e);
	}

	@Override
	public synchronized void traceProcessEnd(EventManager e, long tick) {
		this.addHeader(e.name, tick);
		traceLevel--;
		this.append("Exit");
		this.finish(e);
	}

	@Override
	public synchronized void traceSchedProcess(EventManager e, long curTick, long tick, int priority, ProcessTarget t) {
		this.addHeader(e.name, curTick);
		this.append(String.format("SchedProcess\t%d\t%d\t%s", tick, priority, t.getDescription()));
		this.finish(e);
	}

	@Override
	public void traceConditionalEval(EventManager e, long tick, ProcessTarget t) {}

	@Override
	public void traceConditionalEvalEnded(boolean wakeup, EventManager e, long tick, ProcessTarget t) {
		if (!wakeup)
			return;
		this.addHeader(e.name, tick);
		this.append(String.format("WaitUntilEnded\t%s", t.getDescription()));
		this.finish(e);
	}

}
