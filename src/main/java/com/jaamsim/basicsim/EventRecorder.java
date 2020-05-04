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

import com.jaamsim.events.EventTraceListener;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.InputErrorException;

public class EventRecorder implements EventTraceListener {
	private BufferedWriter outputStream;
	private final EventTraceRecord trcRecord = new EventTraceRecord();

	public EventRecorder(String fileName) {
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

	private void finish() {
		if(trcRecord.traceLevel != 0)
			return;

		trcRecord.add("");
		for (String each : trcRecord) {
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

		trcRecord.clear();
	}

	@Override
	public void traceEvent(long tick, int priority, ProcessTarget t) {
		// Don't write anything if not at level 0
		if (trcRecord.traceLevel != 0)
			throw new ErrorException("Tracing started incorrectly");

		trcRecord.traceEvent(tick, priority, t);
	}

	@Override
	public void traceInterrupt(long tick, int priority, ProcessTarget t) {
		trcRecord.traceInterrupt(tick, priority, t);
	}

	@Override
	public void traceProcessStart(ProcessTarget t) {
		trcRecord.traceProcessStart(t);
	}

	@Override
	public void traceProcessEnd() {
		trcRecord.traceProcessEnd();
		this.finish();
	}

	@Override
	public void traceWait(long tick, int priority, ProcessTarget t) {
		trcRecord.traceWait(tick, priority, t);
		this.finish();
	}

	@Override
	public void traceWaitUntil() {
		trcRecord.traceWaitUntil();
		this.finish();
	}

	@Override
	public void traceSchedUntil(ProcessTarget t) {
		trcRecord.traceSchedUntil(t);
	}

	@Override
	public void traceSchedProcess(long tick, int priority, ProcessTarget t) {
		trcRecord.traceSchedProcess(tick, priority, t);
	}

	@Override
	public void traceKill(long tick, int priority, ProcessTarget t) {
		trcRecord.traceKill(tick, priority, t);
	}

	@Override
	public void traceConditionalEval(ProcessTarget t) {
		//FIXME: fix conditonal tracing
		//trcRecord.traceConditionalEval(t);
	}

	@Override
	public void traceConditionalEvalEnded(boolean wakeup, ProcessTarget t) {
		//FIXME: fix conditonal tracing
		//trcRecord.traceConditionalEvalEnded(wakeup, t);
	}

}
