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

		System.out.println("LIFO Events");
		ProcessTarget targ = new TestTarget(1);
		long firstNano = System.nanoTime();
		long lastnanos = firstNano;
		for (int i = 0; i <= 1000000; i++) {
			if (i % 100000 == 0 && i > 0) {
				long nanos = System.nanoTime();
				long deltns = (nanos - lastnanos);
				double perevt = deltns / 100000.0d;
				System.out.format("%7d - %12d ns (%f ns/evt)%n", i, deltns, perevt);
				lastnanos = nanos;
			}
			evt.scheduleProcessExternal(0, 0, false, targ, null);
		}

		long endSchedNanos = System.nanoTime();
		System.out.format("Sched total - %d ns%n", (endSchedNanos - firstNano));

		TestFrameworkHelpers.runEventsToTick(evt, 100, Long.MAX_VALUE);

		long endExecNanos = System.nanoTime();
		long execNanos = endExecNanos - endSchedNanos;
		double perEvtExec = execNanos / 1000000.0d;
		System.out.format("Done exec - %12d ns (%f ns/evt)%n", execNanos, perEvtExec);
		System.out.println("Done all - " + (endExecNanos - firstNano) + " ns");
	}

	@Test
	public void testFIFOEvents() {
		EventManager evt = new EventManager("TestEVT");
		evt.clear();

		System.out.println("FIFO Events");
		ProcessTarget targ = new TestTarget(1);
		long firstNano = System.nanoTime();
		long lastnanos = firstNano;
		for (int i = 0; i <= 1000000; i++) {
			if (i % 100000 == 0 && i > 0) {
				long nanos = System.nanoTime();
				long deltns = (nanos - lastnanos);
				double perevt = deltns / 100000.0d;
				System.out.format("%7d - %12d ns (%f ns/evt)%n", i, deltns, perevt);
				lastnanos = nanos;
			}
			evt.scheduleProcessExternal(0, 0, true, targ, null);
		}

		long endSchedNanos = System.nanoTime();
		System.out.format("Sched total - %d ns%n", (endSchedNanos - firstNano));

		TestFrameworkHelpers.runEventsToTick(evt, 100, Long.MAX_VALUE);

		long endExecNanos = System.nanoTime();
		long execNanos = endExecNanos - endSchedNanos;
		double perEvtExec = execNanos / 1000000.0d;
		System.out.format("Done exec - %12d ns (%f ns/evt)%n", execNanos, perEvtExec);
		System.out.println("Done all - " + (endExecNanos - firstNano) + " ns");
	}

	@Test
	public void testPriorityEvents() {
		EventManager evt = new EventManager("TestEVT");
		evt.clear();

		System.out.println("Different Priority Events");
		ProcessTarget targ = new TestTarget(1);
		long firstNano = System.nanoTime();
		long lastnanos = firstNano;
		for (int i = 0; i <= 1000000; i++) {
			if (i % 100000 == 0 && i > 0) {
				long nanos = System.nanoTime();
				long deltns = (nanos - lastnanos);
				double perevt = deltns / 100000.0d;
				System.out.format("%7d - %12d ns (%f ns/evt)%n", i, deltns, perevt);
				lastnanos = nanos;
			}
			evt.scheduleProcessExternal(0, i, false, targ, null);
		}

		long endSchedNanos = System.nanoTime();
		System.out.format("Sched total - %d ns%n", (endSchedNanos - firstNano));

		TestFrameworkHelpers.runEventsToTick(evt, 10000000, Long.MAX_VALUE);

		long endExecNanos = System.nanoTime();
		long execNanos = endExecNanos - endSchedNanos;
		double perEvtExec = execNanos / 1000000.0d;
		System.out.format("Done exec - %12d ns (%f ns/evt)%n", execNanos, perEvtExec);
		System.out.println("Done all - " + (endExecNanos - firstNano) + " ns");
	}

	@Test
	public void testTimeEvents() {
		EventManager evt = new EventManager("TestEVT");
		evt.clear();

		System.out.println("Different Time Events");
		ProcessTarget targ = new TestTarget(1);
		long firstNano = System.nanoTime();
		long lastnanos = firstNano;
		for (int i = 0; i <= 1000000; i++) {
			if (i % 100000 == 0 && i > 0) {
				long nanos = System.nanoTime();
				long deltns = (nanos - lastnanos);
				double perevt = deltns / 100000.0d;
				System.out.format("%7d - %12d ns (%f ns/evt)%n", i, deltns, perevt);
				lastnanos = nanos;
			}
			evt.scheduleProcessExternal(i, 0, false, targ, null);
		}

		long endSchedNanos = System.nanoTime();
		System.out.format("Sched total - %d ns%n", (endSchedNanos - firstNano));

		TestFrameworkHelpers.runEventsToTick(evt, 10000000, Long.MAX_VALUE);

		long endExecNanos = System.nanoTime();
		long execNanos = endExecNanos - endSchedNanos;
		double perEvtExec = execNanos / 1000000.0d;
		System.out.format("Done exec - %12d ns (%f ns/evt)%n", execNanos, perEvtExec);
		System.out.println("Done all - " + (endExecNanos - firstNano) + " ns");
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
