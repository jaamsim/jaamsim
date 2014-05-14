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
		evt.scheduleProcess(0, 0, false, new LogTarget(0, log));
		evt.scheduleProcess(1, 0, false, new LogTarget(1, log));
		evt.scheduleProcess(2, 0, false, new LogTarget(2, log));
		evt.scheduleProcess(3, 0, false, new LogTarget(3, log));
		evt.scheduleProcess(4, 0, false, new LogTarget(4, log));

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
		evt.scheduleProcess(0, 0, false, new LogTarget(0, log));
		evt.scheduleProcess(0, 1, false, new LogTarget(1, log));
		evt.scheduleProcess(0, 2, false, new LogTarget(2, log));
		evt.scheduleProcess(0, 3, false, new LogTarget(3, log));
		evt.scheduleProcess(0, 4, false, new LogTarget(4, log));

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
		evt.scheduleProcess(0, 0, false, new LogTarget(0, log));
		evt.scheduleProcess(0, 0, false, new LogTarget(1, log));
		evt.scheduleProcess(0, 0, false, new LogTarget(2, log));
		evt.scheduleProcess(0, 0, false, new LogTarget(3, log));
		evt.scheduleProcess(0, 0, false, new LogTarget(4, log));

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
		evt.scheduleProcess(0, 0, true, new LogTarget(0, log));
		evt.scheduleProcess(0, 0, true, new LogTarget(1, log));
		evt.scheduleProcess(0, 0, true, new LogTarget(2, log));
		evt.scheduleProcess(0, 0, true, new LogTarget(3, log));
		evt.scheduleProcess(0, 0, true, new LogTarget(4, log));

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
		evt.scheduleProcess(0, 0, false, new LogTarget(0, log));
		evt.scheduleProcess(1, 0, false, new LogTarget(1, log));
		evt.scheduleProcess(2, 0, false, new LogTarget(2, log));
		evt.scheduleProcess(3, 0, false, new LogTarget(3, log));
		evt.scheduleProcess(4, 0, false, new LogTarget(4, log));

		evt.scheduleProcess(0, 0, true, new LogTarget(10, log));
		evt.scheduleProcess(1, 0, true, new LogTarget(11, log));
		evt.scheduleProcess(2, 0, true, new LogTarget(12, log));
		evt.scheduleProcess(3, 0, true, new LogTarget(13, log));
		evt.scheduleProcess(4, 0, true, new LogTarget(14, log));

		evt.scheduleProcess(0, 0, false, new LogTarget(20, log));
		evt.scheduleProcess(1, 0, false, new LogTarget(21, log));
		evt.scheduleProcess(2, 0, false, new LogTarget(22, log));
		evt.scheduleProcess(3, 0, false, new LogTarget(23, log));
		evt.scheduleProcess(4, 0, false, new LogTarget(24, log));

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

	@Test
	public void testRBTree() {
		EventTree tree = new EventTree();

		for (int i = 0; i < 10000; ++i) {
			tree.createOrFindNode(i, 0);
			tree.verify();
			int nodeCount = tree.verifyNodeCount();
			assertTrue((i+1) == nodeCount);
		}
		for (int i = 0; i < 10000; ++i) {
			assertTrue(tree.find(i,  0) != null);
		}
		assertTrue(tree.getNextNode().schedTick == 0);

		for (int i = 0; i < 10000; ++i) {
			tree.removeNode(i, 0);
			tree.verify();
			int nodeCount = tree.verifyNodeCount();
			assertTrue(nodeCount == 10000 - i - 1);
		}

		tree = new EventTree();
		for (int i = 10000; i > 0; --i) {
			tree.createOrFindNode(i, 0);
			tree.verify();
			int nodeCount = tree.verifyNodeCount();
			assertTrue((10001-i) == nodeCount);
		}

		assertTrue(tree.getNextNode().schedTick == 1);

		for (int i = 1; i <= 10000; ++i) {
			assertTrue(tree.find(i,  0) != null);
		}

		for (int i = 10000; i > 0; --i) {
			tree.removeNode(i, 0);
			tree.verify();
			int nodeCount = tree.verifyNodeCount();
			assertTrue(nodeCount == i - 1);
		}

		for (int i = 10000; i > 0; --i) {
			tree.createOrFindNode(i, 0);
			tree.verify();
			tree.createOrFindNode(-i, 0);
			tree.verify();
			int nodeCount = tree.verifyNodeCount();
			assertTrue(((10001-i)*2) == nodeCount);
		}

		assertTrue(tree.getNextNode().schedTick == -10000);

		for (int i = 0; i > 10000; --i) {
			tree.removeNode(i, 0);
			tree.verify();
			tree.removeNode(-i, 0);
			tree.verify();
			int nodeCount = tree.verifyNodeCount();
			assertTrue((20000-2-2*i) == nodeCount);
		}

	}
}
