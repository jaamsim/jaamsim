/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.jaamsim.events.EventManager;
import com.jaamsim.events.EventTraceListener;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.InputErrorException;

public class EventRecorder implements EventTraceListener {
	private BufferedWriter outputStream;
	private int traceLevel;
	private final ArrayList<String> traces = new ArrayList<String>();

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
			rec.append("  ");
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

	@Override
	public synchronized void traceWait(EventManager e, long curTick, long tick, int priority, ProcessTarget t) {
		this.addHeader(e.name, curTick);
		traceLevel--;

		this.append(String.format("Wait\t%d\t%d\t%s", tick, priority, t.getDescription()));

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
	public synchronized void traceWaitUntilEnded(EventManager e, long curTick, ProcessTarget t) {
		this.addHeader(e.name, curTick);
		this.append(String.format("WaitUntilEnded\t%s", t.getDescription()));
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
}
