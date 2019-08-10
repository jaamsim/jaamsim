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
package com.jaamsim.events;

import org.junit.Test;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.EntityIterator;
import com.jaamsim.basicsim.JaamSimModel;

public class TestSchedEvent {

	@Test
	public void testEntityCreate() {
		JaamSimModel simModel = new JaamSimModel();

		long[] nanoStamps = new long[11];
		for (int i = 0; i <= 1000000; i++) {
			if (i % 100000 == 0) {
				int idx = i / 100000;
				nanoStamps[idx] = System.nanoTime();
			}
			simModel.createInstance(Entity.class);
		}
		long endSchedNanos = System.nanoTime();

		long endExecNanos = System.nanoTime();

		outputResults("Entity Create", nanoStamps, endSchedNanos, endExecNanos);
	}


	@Test
	public void testEntityKillFirst() {
		JaamSimModel simModel = new JaamSimModel();

		for (int i = 0; i <= 1000000; i++) {
			simModel.createInstance(Entity.class);
		}

		EntityIterator<Entity> it = simModel.getClonesOfIterator(Entity.class);
		long[] nanoStamps = new long[11];
		for (int i = 0; i <= 1000000; i++) {
			if (i % 100000 == 0) {
				int idx = i / 100000;
				nanoStamps[idx] = System.nanoTime();
			}
			it.next().kill();
		}
		long endSchedNanos = System.nanoTime();

		long endExecNanos = System.nanoTime();

		outputResults("Entity Kill First", nanoStamps, endSchedNanos, endExecNanos);
	}

	/* This test can not be effectively run on a singly-linked list of entities
	@Test
	public void testEntityKillLast() {
		JaamSimModel simModel = new JaamSimModel();

		for (int i = 0; i <= 1000000; i++) {
			simModel.createInstance(Entity.class);
		}

		ArrayList<? extends Entity> ents = simModel.getEntities();
		long[] nanoStamps = new long[11];
		for (int i = 0; i <= 1000000; i++) {
			if (i % 100000 == 0) {
				int idx = i / 100000;
				nanoStamps[idx] = System.nanoTime();
			}
			ents.get(ents.size() - 1).kill();
		}
		long endSchedNanos = System.nanoTime();

		long endExecNanos = System.nanoTime();

		outputResults("Entity Kill Last", nanoStamps, endSchedNanos, endExecNanos);
	}
*/
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
		System.out.format("Total Time - %f sec%n%n", (endExecNanos - nanoStamps[0])/1e9d);
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
