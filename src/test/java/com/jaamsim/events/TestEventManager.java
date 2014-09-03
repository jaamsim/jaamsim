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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

public class TestEventManager {

	@Test
	public void testScheduleTime() {
		EventManager evt = new EventManager("TestScheduleTimeEVT");
		evt.clear();

		ArrayList<String> log = new ArrayList<String>();
		evt.scheduleProcessExternal(0, 0, false, new LogTarget(0, log), null);
		evt.scheduleProcessExternal(1, 0, false, new LogTarget(1, log), null);
		evt.scheduleProcessExternal(2, 0, false, new LogTarget(2, log), null);
		evt.scheduleProcessExternal(3, 0, false, new LogTarget(3, log), null);
		evt.scheduleProcessExternal(4, 0, false, new LogTarget(4, log), null);

		TestFrameworkHelpers.runEventsToTick(evt, 100, 1000);

		ArrayList<String> expected = new ArrayList<String>();
		expected.add("Target:0");
		expected.add("Target:1");
		expected.add("Target:2");
		expected.add("Target:3");
		expected.add("Target:4");

		assertTrue(expected.size() == log.size());
		for (int i = 0; i < expected.size(); i++) {
			assertTrue(expected.get(i).equals(log.get(i)));
		}
	}

	@Test
	public void testSchedulePriority() {
		EventManager evt = new EventManager("testSchedulePriorityEVT");
		evt.clear();

		ArrayList<String> log = new ArrayList<String>();
		evt.scheduleProcessExternal(0, 0, false, new LogTarget(0, log), null);
		evt.scheduleProcessExternal(0, 1, false, new LogTarget(1, log), null);
		evt.scheduleProcessExternal(0, 2, false, new LogTarget(2, log), null);
		evt.scheduleProcessExternal(0, 3, false, new LogTarget(3, log), null);
		evt.scheduleProcessExternal(0, 4, false, new LogTarget(4, log), null);

		TestFrameworkHelpers.runEventsToTick(evt, 100, 1000);

		ArrayList<String> expected = new ArrayList<String>();
		expected.add("Target:0");
		expected.add("Target:1");
		expected.add("Target:2");
		expected.add("Target:3");
		expected.add("Target:4");

		assertTrue(expected.size() == log.size());
		for (int i = 0; i < expected.size(); i++) {
			assertTrue(expected.get(i).equals(log.get(i)));
		}
	}

	/**
	 * Schedule events at the same time and test LIFO for the tiebreaker.
	 */
	@Test
	public void testScheduleLIFO() {
		EventManager evt = new EventManager("testScheduleLIFOEVT");
		evt.clear();

		ArrayList<String> log = new ArrayList<String>();
		evt.scheduleProcessExternal(0, 0, false, new LogTarget(0, log), null);
		evt.scheduleProcessExternal(0, 0, false, new LogTarget(1, log), null);
		evt.scheduleProcessExternal(0, 0, false, new LogTarget(2, log), null);
		evt.scheduleProcessExternal(0, 0, false, new LogTarget(3, log), null);
		evt.scheduleProcessExternal(0, 0, false, new LogTarget(4, log), null);

		TestFrameworkHelpers.runEventsToTick(evt, 100, 1000);

		ArrayList<String> expected = new ArrayList<String>();
		expected.add("Target:4");
		expected.add("Target:3");
		expected.add("Target:2");
		expected.add("Target:1");
		expected.add("Target:0");

		assertTrue(expected.size() == log.size());
		for (int i = 0; i < expected.size(); i++) {
			assertTrue(expected.get(i).equals(log.get(i)));
		}
	}

	/**
	 * Schedule events at the same time and test LIFO for the tiebreaker.
	 */
	@Test
	public void testScheduleFIFO() {
		EventManager evt = new EventManager("testScheduleFIFOEVT");
		evt.clear();

		ArrayList<String> log = new ArrayList<String>();
		evt.scheduleProcessExternal(0, 0, true, new LogTarget(0, log), null);
		evt.scheduleProcessExternal(0, 0, true, new LogTarget(1, log), null);
		evt.scheduleProcessExternal(0, 0, true, new LogTarget(2, log), null);
		evt.scheduleProcessExternal(0, 0, true, new LogTarget(3, log), null);
		evt.scheduleProcessExternal(0, 0, true, new LogTarget(4, log), null);

		TestFrameworkHelpers.runEventsToTick(evt, 100, 1000);

		ArrayList<String> expected = new ArrayList<String>();
		expected.add("Target:0");
		expected.add("Target:1");
		expected.add("Target:2");
		expected.add("Target:3");
		expected.add("Target:4");

		assertTrue(expected.size() == log.size());
		for (int i = 0; i < expected.size(); i++) {
			assertTrue(expected.get(i).equals(log.get(i)));
		}
	}

	/**
	 * Schedule events at the same time and test LIFO for the tiebreaker.
	 */
	@Test
	public void testScheduleMixed() {
		EventManager evt = new EventManager("testScheduleMixedEVT");
		evt.clear();

		ArrayList<String> log = new ArrayList<String>();
		evt.scheduleProcessExternal(0, 0, false, new LogTarget(0, log), null);
		evt.scheduleProcessExternal(1, 0, false, new LogTarget(1, log), null);
		evt.scheduleProcessExternal(2, 0, false, new LogTarget(2, log), null);
		evt.scheduleProcessExternal(3, 0, false, new LogTarget(3, log), null);
		evt.scheduleProcessExternal(4, 0, false, new LogTarget(4, log), null);

		evt.scheduleProcessExternal(0, 0, true, new LogTarget(10, log), null);
		evt.scheduleProcessExternal(1, 0, true, new LogTarget(11, log), null);
		evt.scheduleProcessExternal(2, 0, true, new LogTarget(12, log), null);
		evt.scheduleProcessExternal(3, 0, true, new LogTarget(13, log), null);
		evt.scheduleProcessExternal(4, 0, true, new LogTarget(14, log), null);

		evt.scheduleProcessExternal(0, 0, false, new LogTarget(20, log), null);
		evt.scheduleProcessExternal(1, 0, false, new LogTarget(21, log), null);
		evt.scheduleProcessExternal(2, 0, false, new LogTarget(22, log), null);
		evt.scheduleProcessExternal(3, 0, false, new LogTarget(23, log), null);
		evt.scheduleProcessExternal(4, 0, false, new LogTarget(24, log), null);

		TestFrameworkHelpers.runEventsToTick(evt, 100, 1000);

		ArrayList<String> expected = new ArrayList<String>();
		expected.add("Target:20");
		expected.add("Target:0");
		expected.add("Target:10");
		expected.add("Target:21");
		expected.add("Target:1");
		expected.add("Target:11");
		expected.add("Target:22");
		expected.add("Target:2");
		expected.add("Target:12");
		expected.add("Target:23");
		expected.add("Target:3");
		expected.add("Target:13");
		expected.add("Target:24");
		expected.add("Target:4");
		expected.add("Target:14");

		assertTrue(expected.size() == log.size());
		for (int i = 0; i < expected.size(); i++) {
			assertTrue(expected.get(i).equals(log.get(i)));
		}
	}

	/**
	 * Schedule events at the same time and test LIFO for the tiebreaker.
	 */
	@Test
	public void testScheduleWait() {
		EventManager evt = new EventManager("testScheduleWaitEVT");
		evt.clear();

		final ArrayList<String> log = new ArrayList<String>();
		evt.scheduleProcessExternal(0, 0, false, new ProcessTarget() {
			@Override
			public String getDescription() { return ""; }

			@Override
			public void process() {
				log.add("Wait1:" + EventManager.simTicks());
				EventManager.waitTicks(1, 0, true, null);
				log.add("Wait1:" + EventManager.simTicks());
				EventManager.waitTicks(1, 0, true, null);
				log.add("Wait1:" + EventManager.simTicks());
				EventManager.waitTicks(1, 0, false, null);
				log.add("Wait1:" + EventManager.simTicks());
			}
		}, null);

		evt.scheduleProcessExternal(0, 0, false, new ProcessTarget() {
			@Override
			public String getDescription() { return ""; }

			@Override
			public void process() {
				log.add("Wait2:" + EventManager.simTicks());
				EventManager.waitTicks(1, 0, true, null);
				log.add("Wait2:" + EventManager.simTicks());
				EventManager.waitTicks(1, 0, true, null);
				log.add("Wait2:" + EventManager.simTicks());
				EventManager.waitTicks(1, 0, true, null);
				log.add("Wait2:" + EventManager.simTicks());
			}
		}, null);

		TestFrameworkHelpers.runEventsToTick(evt, 100, 1000);

		ArrayList<String> expected = new ArrayList<String>();
		expected.add("Wait2:0");
		expected.add("Wait1:0");
		expected.add("Wait2:1");
		expected.add("Wait1:1");
		expected.add("Wait2:2");
		expected.add("Wait1:2");
		expected.add("Wait1:3");
		expected.add("Wait2:3");

		assertTrue(expected.size() == log.size());
		for (int i = 0; i < expected.size(); i++) {
			assertTrue(expected.get(i).equals(log.get(i)));
		}
	}

	private static class LogTarget extends ProcessTarget {
		final ArrayList<String> log;
		final int num;
		LogTarget(int i, ArrayList<String> l) {
			log = l;
			num = i;
		}

		@Override
		public String getDescription() {
			return "Target:" + num;
		}

		@Override
		public void process() {
			log.add("Target:" + num);
		}
	}
}
