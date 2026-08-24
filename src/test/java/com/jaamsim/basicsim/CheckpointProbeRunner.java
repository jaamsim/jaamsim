/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2026 JaamSim Software Inc.
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

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import com.jaamsim.input.InputAgent;
import com.jaamsim.ui.GUIFrame;

/**
 * Runs CheckpointProbe against every example model shipped with JaamSim and reports, in
 * aggregate, which ProcessTarget classes would need hand-written support before snapshot
 * checkpointing could work.
 * <p>
 * Each model is paused halfway through its run so that the probe sees a realistic
 * mid-scenario event set rather than the sparse one that exists at time zero.
 */
public class CheckpointProbeRunner {

	static final double RUN_DURATION = 1000.0d;
	static final double PAUSE_AT = 500.0d;
	static final long TIMEOUT_MS = 20000L;

	static boolean verbose = false;

	public static void main(String[] args) {
		System.setProperty("java.awt.headless", "true");

		ArrayList<String> filters = new ArrayList<>();
		int limit = Integer.MAX_VALUE;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-v")) {
				verbose = true;
			}
			else if (args[i].equals("-n") && i + 1 < args.length) {
				limit = Integer.parseInt(args[++i]);
			}
			else {
				filters.add(args[i].toUpperCase());
			}
		}

		ArrayList<String> topics = new ArrayList<>();
		for (String topic : collectModels()) {
			if (!filters.isEmpty()) {
				boolean hit = false;
				for (String f : filters) {
					if (topic.toUpperCase().contains(f))
						hit = true;
				}
				if (!hit)
					continue;
			}
			topics.add(topic);
			if (topics.size() >= limit)
				break;
		}
		System.out.format("Probing %s example models, paused at %.0f s%n%n", topics.size(), PAUSE_AT);

		int numClear = 0;
		int numIncomplete = 0;
		int numBlocked = 0;
		int numFailed = 0;

		TreeMap<String, Integer> aggUnresolved = new TreeMap<>();
		TreeMap<String, String> aggSample = new TreeMap<>();
		ArrayList<String> blockedModels = new ArrayList<>();
		ArrayList<String> failedModels = new ArrayList<>();
		ArrayList<CheckpointProbe.Report> reports = new ArrayList<>();

		for (String topic : topics) {
			int index = topic.indexOf('/');
			String name = (index >= 0) ? topic.substring(index + 1) : topic;

			CheckpointProbe.Report rep = null;
			try {
				rep = probeModel(topic, name);
			}
			catch (Throwable t) {
				numFailed++;
				String msg = t.getClass().getSimpleName() + ": " + t.getMessage();
				failedModels.add(String.format("%-52s %s", name, msg));
				System.out.format("  %-52s FAILED  %s%n", trim(name), msg);
				continue;
			}

			reports.add(rep);
			System.out.format("  %-46s %-8s %-11s %3d evt  %5.1f%% named%n",
					trim(name), rep.stopReason, rep.getVerdict(),
					rep.numScheduled + rep.numConditional, 100.0d * rep.getCoverage());

			if (rep.numCaptured > 0) {
				numBlocked++;
				blockedModels.add(String.format("%-52s %s captured", name, rep.numCaptured));
			}
			else if (rep.isCheckpointable()) {
				numClear++;
			}
			else {
				numIncomplete++;
			}

			for (Map.Entry<String, Integer> e : rep.unresolvedByClass.entrySet()) {
				Integer n = aggUnresolved.get(e.getKey());
				aggUnresolved.put(e.getKey(), (n == null ? 0 : n) + e.getValue());
				if (!aggSample.containsKey(e.getKey()))
					aggSample.put(e.getKey(), rep.unresolvedSample.get(e.getKey()));
			}
		}

		// ---- aggregate ----

		int totalEvents = 0;
		int totalResolved = 0;
		for (CheckpointProbe.Report rep : reports) {
			totalEvents += rep.numTargetsResolved + rep.numTargetsUnresolved;
			totalResolved += rep.numTargetsResolved;
		}

		line();
		System.out.format("MODELS      %s clear, %s incomplete, %s blocked, %s failed to run%n",
				numClear, numIncomplete, numBlocked, numFailed);
		System.out.format("EVENTS      %s of %s targets named (%.2f%%)%n",
				totalResolved, totalEvents,
				totalEvents == 0 ? 100.0d : 100.0d * totalResolved / totalEvents);

		if (!aggUnresolved.isEmpty()) {
			line();
			System.out.format("TARGET CLASSES NEEDING HAND-WRITTEN SUPPORT (%s)%n%n", aggUnresolved.size());
			for (Map.Entry<String, Integer> e : aggUnresolved.entrySet()) {
				System.out.format("  %6s x  %s%n", e.getValue(), e.getKey());
				String sample = aggSample.get(e.getKey());
				if (sample != null)
					System.out.format("             e.g. \"%s\"%n", sample);
			}
		}

		if (!blockedModels.isEmpty()) {
			line();
			System.out.format("MODELS BLOCKED BY CAPTURED PROCESSES (%s)%n%n", blockedModels.size());
			for (String s : blockedModels) {
				System.out.format("  %s%n", s);
			}
		}

		if (!failedModels.isEmpty()) {
			line();
			System.out.format("MODELS THAT FAILED TO RUN (%s)%n%n", failedModels.size());
			for (String s : failedModels) {
				System.out.format("  %s%n", s);
			}
		}
		line();

		// The Process pool holds non-daemon threads parked in the pool forever, so the
		// JVM will not exit on its own once main returns
		System.exit(0);
	}

	/**
	 * Returns the resource path of every example model, relative to the examples folder
	 * and without the ".cfg" extension.
	 * <p>
	 * Note that ExampleBox.getExampleList cannot be used here. It keys its map on the bare
	 * model name and keeps the subfolder inside the ExampleModel object, so appending its
	 * entries to the examples path silently fails to find any model held in a subfolder.
	 */
	static ArrayList<String> collectModels() {
		ArrayList<String> ret = new ArrayList<>();
		String folder = "/resources/examples";

		for (String name : GUIFrame.getResourceFileNames(folder)) {
			if (name.endsWith(".cfg"))
				ret.add(name.substring(0, name.length() - 4));
		}

		for (String sub : GUIFrame.getResourceSubfolderNames(folder)) {
			for (String name : GUIFrame.getResourceFileNames(folder + "/" + sub)) {
				if (name.endsWith(".cfg"))
					ret.add(sub + "/" + name.substring(0, name.length() - 4));
			}
		}
		return ret;
	}

	/**
	 * Loads one example model, runs it to the halfway point, and probes it.
	 */
	static CheckpointProbe.Report probeModel(String topic, String name) {
		JaamSimModel simModel = new JaamSimModel(name + ".cfg");
		simModel.autoLoad();

		int entsBefore = countEntities(simModel);
		InputAgent.readResource(simModel, "<res>/examples/" + topic + ".cfg");
		simModel.postLoad();

		// A resource path that does not resolve is not reported as an error, it simply
		// loads nothing, which would leave an empty model that trivially "passes"
		if (countEntities(simModel) == entsBefore)
			throw new RuntimeException("config defined no entities - bad resource path?");
		if (simModel.getNumErrors() > 0)
			throw new RuntimeException(simModel.getNumErrors() + " input errors");

		// Give every model the same run window so that the probe happens at a comparable
		// point in each one
		simModel.setInput("Simulation", "InitializationDuration", "0 s");
		simModel.setInput("Simulation", "RunDuration", RUN_DURATION + " s");
		simModel.setInput("Simulation", "PauseTime", PAUSE_AT + " s");

		PauseWaiter waiter = new PauseWaiter();
		simModel.start(waiter, null);

		String stopReason = waitForQuiescence(simModel, waiter);

		try {
			CheckpointProbe.Report rep = CheckpointProbe.probe(simModel);
			rep.stopReason = stopReason;
			if (verbose)
				dump(simModel, rep);
			return rep;
		}
		finally {
			simModel.pause();
			simModel.close();
		}
	}

	/**
	 * Prints the full event set for one model, which is what identifies a target that the
	 * resolver could not name.
	 */
	static void dump(JaamSimModel simModel, CheckpointProbe.Report rep) {
		System.out.format("%n    stop=%s simTime=%.3f ticks=%s running=%s%n",
				rep.stopReason, rep.simTime, rep.simTicks, simModel.isRunning());

		int numEnts = 0;
		int numGenerated = 0;
		StringBuilder flow = new StringBuilder();
		for (Entity ent : simModel.getClonesOfIterator(Entity.class)) {
			numEnts++;
			if (ent.isGenerated())
				numGenerated++;
			if (!(ent instanceof com.jaamsim.ProcessFlow.LinkedComponent))
				continue;
			try {
				double n = simModel.getDoubleValue("[" + ent.getName() + "].NumberAdded");
				flow.append(String.format("      %-28s NumberAdded=%.0f%n", ent.getName(), n));
			}
			catch (Throwable t) {
				flow.append(String.format("      %-28s <%s>%n", ent.getName(), t.getMessage()));
			}
		}
		System.out.format("    entities=%s (generated=%s) runDuration=%.1f%n",
				numEnts, numGenerated, simModel.getSimulation().getRunDuration());
		System.out.print(flow);

		EventSlotResolver resolver = new EventSlotResolver(simModel);
		ArrayList<com.jaamsim.events.EventSnap> snaps = new ArrayList<>();
		simModel.getEventManager().getEventSnapList(snaps);
		for (com.jaamsim.events.EventSnap snap : snaps) {
			String slot = resolver.getSlot(snap.target);
			System.out.format("    %-9s %-6s %-34s %-38s %s%n",
					snap.conditional ? "cond" : Long.toString(snap.ticks),
					snap.conditional ? "-" : Integer.toString(snap.priority),
					snap.getDescription(),
					snap.getTargetClassName().replace("com.jaamsim.", ""),
					snap.captured ? "CAPTURED" : (slot == null ? "*** UNRESOLVED" : slot));
		}
		System.out.println();
	}

	/**
	 * Blocks until the model has stopped executing events, either because it reached the
	 * pause time or because the run ended.
	 */
	static String waitForQuiescence(JaamSimModel simModel, PauseWaiter waiter) {
		long deadline = System.currentTimeMillis() + TIMEOUT_MS;
		String reason = "timeout";

		// Wait for the model to reach the pause time or finish
		while (System.currentTimeMillis() < deadline) {
			if (waiter.error != null) {
				reason = "error";
				break;
			}
			if (waiter.ended) {
				reason = "ended";
				break;
			}
			if (simModel.getSimTime() >= PAUSE_AT && !simModel.isRunning()) {
				reason = "paused";
				break;
			}
			sleep();
		}

		// The event loop clears runningProc as it returns, so wait for that to settle
		// before reading the event tree
		simModel.pause();
		while (System.currentTimeMillis() < deadline && simModel.isRunning()) {
			sleep();
		}

		if (waiter.error != null)
			throw new RuntimeException("runtime error during run", waiter.error);

		if (simModel.isRunning())
			throw new RuntimeException("model did not stop within " + TIMEOUT_MS + " ms");

		return reason;
	}

	static int countEntities(JaamSimModel simModel) {
		int n = 0;
		for (@SuppressWarnings("unused") Entity ent : simModel.getClonesOfIterator(Entity.class)) {
			n++;
		}
		return n;
	}

	static void sleep() {
		
		try {
			Thread.sleep(2L);
		}
		catch (InterruptedException e) {}
	}

	static String trim(String s) {
		if (s.length() <= 52)
			return s;
		return s.substring(0, 49) + "...";
	}

	static void line() {
		System.out.println();
		System.out.println("--------------------------------------------------------------------------");
	}

	static class PauseWaiter implements RunListener {
		volatile boolean ended = false;
		volatile Throwable error = null;

		@Override
		public void runEnded() {
			ended = true;
		}

		@Override
		public void handleRuntimeError(JaamSimModel sm, Throwable t) {
			error = t;
		}
	}

}
