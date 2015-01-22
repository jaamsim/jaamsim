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
package com.jaamsim.events;

import org.junit.Test;

public class TestSchedEvent {

	@Test
	public void testLIFOEvents() {
		EventManager evt = new EventManager("TestEVT");
		evt.clear();

		ProcessTarget targ = new TestTarget(1);
		long[] nanoStamps = new long[11];
		for (int i = 0; i <= 1000000; i++) {
			if (i % 100000 == 0) {
				int idx = i / 100000;
				nanoStamps[idx] = System.nanoTime();
			}
			evt.scheduleProcessExternal(0, 0, false, targ, null);
		}
		long endSchedNanos = System.nanoTime();

		TestFrameworkHelpers.runEventsToTick(evt, 100, Long.MAX_VALUE);

		long endExecNanos = System.nanoTime();

		outputResults("LIFO Events", nanoStamps, endSchedNanos, endExecNanos);
	}

	@Test
	public void testFIFOEvents() {
		EventManager evt = new EventManager("TestEVT");
		evt.clear();

		ProcessTarget targ = new TestTarget(1);
		long[] nanoStamps = new long[11];
		for (int i = 0; i <= 1000000; i++) {
			if (i % 100000 == 0) {
				int idx = i / 100000;
				nanoStamps[idx] = System.nanoTime();
			}
			evt.scheduleProcessExternal(0, 0, true, targ, null);
		}
		long endSchedNanos = System.nanoTime();

		TestFrameworkHelpers.runEventsToTick(evt, 100, Long.MAX_VALUE);

		long endExecNanos = System.nanoTime();

		outputResults("FIFO Events", nanoStamps, endSchedNanos, endExecNanos);
	}

	@Test
	public void testPriorityEvents() {
		EventManager evt = new EventManager("TestEVT");
		evt.clear();

		ProcessTarget targ = new TestTarget(1);
		long[] nanoStamps = new long[11];
		for (int i = 0; i <= 1000000; i++) {
			if (i % 100000 == 0) {
				int idx = i / 100000;
				nanoStamps[idx] = System.nanoTime();
			}
			evt.scheduleProcessExternal(0, i, false, targ, null);
		}
		long endSchedNanos = System.nanoTime();

		TestFrameworkHelpers.runEventsToTick(evt, 10000000, Long.MAX_VALUE);

		long endExecNanos = System.nanoTime();

		outputResults("Different Priority Events", nanoStamps, endSchedNanos, endExecNanos);
	}

	@Test
	public void testTimeEvents() {
		EventManager evt = new EventManager("TestEVT");
		evt.clear();

		ProcessTarget targ = new TestTarget(1);
		long[] nanoStamps = new long[11];
		for (int i = 0; i <= 1000000; i++) {
			if (i % 100000 == 0) {
				int idx = i / 100000;
				nanoStamps[idx] = System.nanoTime();
			}
			evt.scheduleProcessExternal(i, 0, false, targ, null);
		}
		long endSchedNanos = System.nanoTime();

		TestFrameworkHelpers.runEventsToTick(evt, 10000000, Long.MAX_VALUE);

		long endExecNanos = System.nanoTime();

		outputResults("Different Time Events", nanoStamps, endSchedNanos, endExecNanos);
	}

	private final void outputResults(String test, long[] nanoStamps, long endSchedNanos, long endExecNanos) {
		long execNanos = endExecNanos - endSchedNanos;
		double perEvtExec = execNanos / 1000000.0d;
		System.out.println(test);

		for (int i = 0; i < 10; i++) {
			long deltns = nanoStamps[i + 1] - nanoStamps[i];
			double perevt = deltns / 100000.0d;
			System.out.format("%7d - %12d ns (%f ns/evt)%n", (i + 1) * 100000, deltns, perevt);
		}

		System.out.format("Sched total - %d ns%n", (endSchedNanos - nanoStamps[0]));
		System.out.format("Done exec - %12d ns (%f ns/evt)%n", execNanos, perEvtExec);
		System.out.format("Done all - %d ns%n%n", (endExecNanos - nanoStamps[0]));
	}

	private static class TestTarget extends ProcessTarget {
		final int num;
		TestTarget(int i) {
			num = i;
		}

		@Override
		public String getDescription() {
			return "SimulationInit-" + num;
		}

		@Override
		public void process() {
			//System.out.println("Running init:" + num);
			//System.out.flush();
		}
	}
}
