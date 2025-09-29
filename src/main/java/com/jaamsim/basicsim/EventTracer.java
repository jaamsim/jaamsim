/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2021-2023 JaamSim Software Inc.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.jaamsim.events.EventManager;
import com.jaamsim.events.EventTraceListener;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.ui.GUIFrame;
import com.jaamsim.ui.LogBox;

class EventTracer implements EventTraceListener {
	private BufferedReader eventVerifyReader;
	private EventTraceRecord reader;
	private long bufferTime; // Internal sim time buffer has been filled to
	private final ArrayList<EventTraceRecord> eventBuffer;

	public EventTracer(String evtName) {
		eventBuffer = new ArrayList<>();
		bufferTime = 0;
		File evtFile = new File(evtName);
		try {
			eventVerifyReader = new BufferedReader(new FileReader(evtFile));
		}
		catch (FileNotFoundException e) {
			throw new InputErrorException("Unable to open the event verification file:%n%s", evtName);
		}

		reader = new EventTraceRecord();
	}

	private void fillBufferUntil(long internalTime) {
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
			if (temp.getInternalTime() > bufferTime) {
				bufferTime = temp.getInternalTime();
			}
			eventBuffer.add(temp);
		}
	}

	private void findEventInBuffer(EventTraceRecord record) {
		// Ensure we have read enough from the log to find this record
		this.fillBufferUntil(record.getInternalTime());

		// Try an optimistic approach first looking for exact matches
		for (EventTraceRecord each : eventBuffer) {
			if (!each.basicCompare(record)) {
				continue;
			}

			for (int i = 0; i < record.size(); i++) {
				if (!record.get(i).equals(each.get(i))) {
					StringBuilder sb = new StringBuilder();
					sb.append("Present event;\n");
					sb.append(record.get(i)).append("\n");

					sb.append("\n");
					sb.append("Next event in the trace file:\n");
					sb.append(each.get(i)).append("\n");

					sb.append("\n");
					sb.append("List of events at the present time:\n");
					for (String line : record) {
						sb.append(line).append("\n");
					}

					sb.append("List of events at the present time in the trace file:\n");
					for (String line : each) {
						sb.append(line).append("\n");
					}

					String msg = sb.toString();
					System.out.println(msg);
					Log.logLine(msg);
					EventManager.current().pause();

					if (GUIFrame.getInstance() != null) {
						GUIFrame.getRunManager().pause();
						GUIFrame.invokeErrorDialog("Event Verification Error",
								"Present event does not match the next event at this time in the "
								+ "trace file.",
								msg, "This message is repeated in the Log Viewer.");
					}
					break;
				}
			}

			// Found the event, it compared OK, remove from the buffer
			eventBuffer.remove(each);
			//System.out.println("Buffersize:" + eventBuffer.size());
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Present event:\n");
		for (String line : record) {
			sb.append(line).append("\n");
		}
		sb.append("Next events in the trace file:\n");
		for (EventTraceRecord rec : eventBuffer) {
			for (String line : rec) {
				sb.append(line).append("\n");
			}
		}

		String msg = sb.toString();
		System.out.println(msg);
		Log.logLine(msg);
		EventManager.current().pause();

		if (GUIFrame.getInstance() != null) {
			GUIFrame.getRunManager().pause();
			GUIFrame.invokeErrorDialog("Event Verification Error",
					"Present event has no matching event at this time in the trace file.",
					msg, "This message is repeated in the Log Viewer.");
		}
	}

	private void finish() {
		if (reader.traceLevel != 0)
			return;

		reader.add("");
		reader.parse();
		findEventInBuffer(reader);
		reader.clear();
	}

	@Override
	public void traceWait(long tick, int priority, ProcessTarget t) {
		reader.traceWait(tick, priority, t);
		this.finish();
	}

	@Override
	public void traceEvent(long tick, int priority, ProcessTarget t) {
		reader.traceEvent(tick, priority, t);
	}

	@Override
	public void traceSchedProcess(long tick, int priority, ProcessTarget t) {
		reader.traceSchedProcess(tick, priority, t);
	}

	@Override
	public void traceProcessStart(ProcessTarget t) {
		reader.traceProcessStart(t);
	}

	@Override
	public void traceProcessEnd() {
		reader.traceProcessEnd();
		this.finish();
	}

	@Override
	public void traceInterrupt(long tick, int priority, ProcessTarget t) {
		reader.traceInterrupt(tick, priority, t);
	}

	@Override
	public void traceKill(long tick, int priority, ProcessTarget t) {
		reader.traceKill(tick, priority, t);
	}

	@Override
	public void traceWaitUntil() {
		reader.traceWaitUntil();
		this.finish();
	}

	@Override
	public void traceSchedUntil(ProcessTarget t) {
		reader.traceSchedUntil(t);
	}

	@Override
	public void traceConditionalEval(ProcessTarget t) {}

	@Override
	public void traceConditionalEvalEnded(boolean wakeup, ProcessTarget t) {
		//FIXME: disable conditional tracing under the event recorder is also fixed
		//if (!wakeup)
		//	return;
		//reader.traceConditionalEvalEnded(wakeup, t);
		//this.finish(EventManager.current());
	}

}
