/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2024 JaamSim Software Inc.
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
package com.jaamsim.events;

import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.junit.Test;

public class TestEventListener {
	private static class InitTarget extends ProcessTarget {
		@Override
		public String getDescription() {
			return "InitTarget";
		}

		@Override
		public void process() {
			EventManager.scheduleTicks(0, 0, false, new StartTarget(), null);
			EventManager.startProcess(new StartTarget());
		}
	}


	private static class StartTarget extends ProcessTarget {
		@Override
		public String getDescription() {
			return "StartTarget";
		}

		@Override
		public void process() {
			EventManager.scheduleTicks(0, 0, false, new WaitTarget(), null);
			EventManager.startProcess(new WaitTarget());
			EventManager.waitTicks(10, 0, false, null);
			EventManager.scheduleTicks(0, 0, false, new WaitTarget(), null);
			EventManager.startProcess(new WaitTarget());
		}
	}

	private static class WaitTarget extends ProcessTarget {
		@Override
		public String getDescription() {
			return "WaitTarget";
		}

		@Override
		public void process() {
			EventManager.waitTicks(10, 0, false, null);
		}
	}

	@Test
	public void testEventLogging() {
		EventManager evt = new EventManager("testScheduleWaitEVT");
		evt.clear();
		EvtLogger log = new EvtLogger();
		evt.setTraceListener(log);

		evt.scheduleProcessExternal(0, 0, false, new InitTarget(), null);
		TestFrameworkHelpers.runEventsToTick(evt, 100, 1000);

		//for (String s : log) {
		//	System.out.println(s);
		//}
		if (log.size() != transcript.size())
			fail("Transcript size mismatch");

		for (int i = 0; i < transcript.size(); i++) {
			if (transcript.get(i).equals(log.get(i)))
				continue;
			fail("Transcript mismatch at row " + i);
		}
	}

	private static final ArrayList<String> transcript = new ArrayList<>();
	static {
	transcript.add("Event	0	0	InitTarget");
	transcript.add("-SchedProcess	0	0	StartTarget");
	transcript.add("-StartProcess	StartTarget");
	transcript.add("--SchedProcess	0	0	WaitTarget");
	transcript.add("--StartProcess	WaitTarget");
	transcript.add("--Wait	10	0	Waiting");
	transcript.add("-Wait	10	0	Waiting");
	transcript.add("Exit");
	transcript.add("");
	transcript.add("Event	0	0	WaitTarget");
	transcript.add("Wait	10	0	Waiting");
	transcript.add("");
	transcript.add("Event	0	0	StartTarget");
	transcript.add("-SchedProcess	0	0	WaitTarget");
	transcript.add("-StartProcess	WaitTarget");
	transcript.add("-Wait	10	0	Waiting");
	transcript.add("Wait	10	0	Waiting");
	transcript.add("");
	transcript.add("Event	0	0	WaitTarget");
	transcript.add("Wait	10	0	Waiting");
	transcript.add("");
	transcript.add("Event	10	0	Waiting");
	transcript.add("Exit");
	transcript.add("");
	transcript.add("Event	10	0	Waiting");
	transcript.add("-SchedProcess	10	0	WaitTarget");
	transcript.add("-StartProcess	WaitTarget");
	transcript.add("-Wait	20	0	Waiting");
	transcript.add("Exit");
	transcript.add("");
	transcript.add("Event	10	0	WaitTarget");
	transcript.add("Wait	20	0	Waiting");
	transcript.add("");
	transcript.add("Event	10	0	Waiting");
	transcript.add("Exit");
	transcript.add("");
	transcript.add("Event	10	0	Waiting");
	transcript.add("Exit");
	transcript.add("");
	transcript.add("Event	10	0	Waiting");
	transcript.add("-SchedProcess	10	0	WaitTarget");
	transcript.add("-StartProcess	WaitTarget");
	transcript.add("-Wait	20	0	Waiting");
	transcript.add("Exit");
	transcript.add("");
	transcript.add("Event	10	0	WaitTarget");
	transcript.add("Wait	20	0	Waiting");
	transcript.add("");
	transcript.add("Event	10	0	Waiting");
	transcript.add("Exit");
	transcript.add("");
	transcript.add("Event	20	0	Waiting");
	transcript.add("Exit");
	transcript.add("");
	transcript.add("Event	20	0	Waiting");
	transcript.add("Exit");
	transcript.add("");
	transcript.add("Event	20	0	Waiting");
	transcript.add("Exit");
	transcript.add("");
	transcript.add("Event	20	0	Waiting");
	transcript.add("Exit");
	transcript.add("");
	}

	private static class EvtLogger extends ArrayList<String> implements EventTraceListener {
		private int traceLevel = 0;

		@Override
		public final void traceWait(long tick, int priority, ProcessTarget t) {
			traceLevel--;
			if (traceLevel < 0) fail("Trace level underflow");
			this.addLevel(String.format("Wait\t%d\t%d\t%s", tick, priority, t.getDescription()));
			if (traceLevel == 0)
				this.addLevel("");
		}

		@Override
		public final void traceEvent(long tick, int priority, ProcessTarget t) {
			if (traceLevel != 0) fail("Trace level not zero at Event entry");
			this.addLevel(String.format("Event\t%d\t%d\t%s", tick, priority, t.getDescription()));
			traceLevel++;
		}

		@Override
		public final void traceInterrupt(long tick, int priority, ProcessTarget t) {
			this.addLevel(String.format("Int\t%d\t%d\t%s", tick, priority, t.getDescription()));
			traceLevel++;
		}

		@Override
		public final void traceKill(long tick, int priority, ProcessTarget t) {
			this.addLevel(String.format("Kill\t%d\t%d\t%s", tick, priority, t.getDescription()));
		}

		@Override
		public final void traceWaitUntil() {
			traceLevel--;
			if (traceLevel < 0) fail("Trace level underflow");
			this.addLevel("WaitUntil");
			if (traceLevel == 0)
				this.addLevel("");
		}

		@Override
		public final void traceSchedUntil(ProcessTarget t) {
			this.addLevel(String.format("SchedUntil\t%s", t.getDescription()));
		}

		@Override
		public final void traceProcessStart(ProcessTarget t) {
			this.addLevel(String.format("StartProcess\t%s", t.getDescription()));
			traceLevel++;
		}

		@Override
		public final void traceProcessEnd() {
			traceLevel--;
			if (traceLevel < 0) fail("Trace level underflow");
			this.addLevel("Exit");
			if (traceLevel == 0)
				this.addLevel("");
		}

		@Override
		public final void traceSchedProcess(long tick, int priority, ProcessTarget t) {
			this.addLevel(String.format("SchedProcess\t%d\t%d\t%s", tick, priority, t.getDescription()));
		}

		private void addLevel(String str) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < traceLevel; i++) {
				sb.append("-");
			}
			sb.append(str);
			this.add(sb.toString());
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

	}


}
