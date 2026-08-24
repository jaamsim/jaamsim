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

import com.jaamsim.events.EventManager;
import com.jaamsim.events.EventSnap;

/**
 * Answers the one question that determines whether snapshot checkpointing is feasible for
 * a given model: can every pending event be named?
 * <p>
 * The probe reports two independent blockers:
 * <ul>
 * <li>Captured processes - a thread parked inside waitSeconds or waitUntil, whose
 *     continuation is a live Java call stack that cannot be written to a file. There is no
 *     way to checkpoint while one exists.
 * <li>Unresolved targets - a scheduled event whose ProcessTarget is not held in a field of
 *     any entity, so it cannot be re-created on restore. Each one names a class that needs
 *     hand-written support.
 * </ul>
 * The probe must be run while the model is quiescent, either from a scheduled event or
 * while the model is paused.
 */
public class CheckpointProbe {

	public static class Report {

		public String modelName = "";
		public double simTime;
		public long simTicks;

		/** Why the model stopped before it was probed, set by the caller. */
		public String stopReason = "";

		public int numScheduled;      // pending events held in the event tree
		public int numConditional;    // pending conditional events
		public int numCaptured;       // parked process continuations - hard blocker

		public int numTargetsResolved;
		public int numTargetsUnresolved;
		public int numHandlesPresent;
		public int numHandlesUnresolved;

		public int numOwnersScanned;
		public int numTargetSlots;
		public int numHandleSlots;

		/** Count of unresolved events by target class name. */
		public final TreeMap<String, Integer> unresolvedByClass = new TreeMap<>();

		/** One example description per unresolved target class. */
		public final TreeMap<String, String> unresolvedSample = new TreeMap<>();

		/** Target class names for the captured processes, for diagnosis. */
		public final ArrayList<String> capturedDescriptions = new ArrayList<>();

		/**
		 * Returns whether a snapshot checkpoint could be taken at this instant.
		 */
		public boolean isCheckpointable() {
			return numCaptured == 0 && numTargetsUnresolved == 0 && numHandlesUnresolved == 0;
		}

		public String getVerdict() {
			if (numCaptured > 0)
				return "BLOCKED";
			if (numTargetsUnresolved > 0 || numHandlesUnresolved > 0)
				return "INCOMPLETE";
			return "CLEAR";
		}

		/**
		 * Returns the fraction of pending events whose target could be named.
		 */
		public double getCoverage() {
			int total = numTargetsResolved + numTargetsUnresolved;
			if (total == 0)
				return 1.0d;
			return (double) numTargetsResolved / total;
		}

		public String format() {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("%s%n", modelName));
			sb.append(String.format("  verdict            %s%n", getVerdict()));
			sb.append(String.format("  simTime            %.3f s  (%s ticks)%n", simTime, simTicks));
			sb.append(String.format("  pending events     %s scheduled, %s conditional%n",
					numScheduled, numConditional));
			sb.append(String.format("  slots found        %s targets, %s handles, over %s owners%n",
					numTargetSlots, numHandleSlots, numOwnersScanned));
			sb.append(String.format("  target coverage    %s/%s (%.1f%%)%n",
					numTargetsResolved, numTargetsResolved + numTargetsUnresolved,
					100.0d * getCoverage()));
			sb.append(String.format("  handle coverage    %s/%s%n",
					numHandlesPresent - numHandlesUnresolved, numHandlesPresent));

			if (numCaptured > 0) {
				sb.append(String.format("  CAPTURED PROCESSES %s - cannot checkpoint%n", numCaptured));
			}

			if (!unresolvedByClass.isEmpty()) {
				sb.append("  unresolved targets:\n");
				for (Map.Entry<String, Integer> e : unresolvedByClass.entrySet()) {
					sb.append(String.format("    %5s x %s%n", e.getValue(), e.getKey()));
					String sample = unresolvedSample.get(e.getKey());
					if (sample != null)
						sb.append(String.format("            e.g. \"%s\"%n", sample));
				}
			}
			return sb.toString();
		}
	}

	/**
	 * Examines the pending events for the given model and reports whether each one could
	 * be written to a checkpoint file.
	 * @param simModel - model to examine, which must be quiescent
	 * @return the probe result
	 */
	public static Report probe(JaamSimModel simModel) {
		Report rep = new Report();
		rep.modelName = simModel.getName();
		rep.simTime = simModel.getSimTime();
		rep.simTicks = simModel.getSimTicks();

		EventManager evt = simModel.getEventManager();

		EventSlotResolver resolver = new EventSlotResolver(simModel);
		rep.numOwnersScanned = resolver.getNumOwnersScanned();
		rep.numTargetSlots = resolver.getNumTargetSlots();
		rep.numHandleSlots = resolver.getNumHandleSlots();

		ArrayList<EventSnap> snaps = new ArrayList<>();
		evt.getEventSnapList(snaps);

		for (EventSnap snap : snaps) {
			if (snap.conditional)
				rep.numConditional++;
			else
				rep.numScheduled++;

			// A captured process is reported on its own terms. It is never resolvable,
			// so counting it as an unresolved target as well would double count it.
			if (snap.captured) {
				rep.numCaptured++;
				rep.capturedDescriptions.add(snap.getDescription());
				continue;
			}

			if (resolver.getSlot(snap.target) != null) {
				rep.numTargetsResolved++;
			}
			else {
				rep.numTargetsUnresolved++;
				String key = snap.getTargetClassName();
				Integer n = rep.unresolvedByClass.get(key);
				rep.unresolvedByClass.put(key, n == null ? 1 : n + 1);
				if (!rep.unresolvedSample.containsKey(key))
					rep.unresolvedSample.put(key, snap.getDescription());
			}

			if (snap.handle != null) {
				rep.numHandlesPresent++;
				if (resolver.getSlot(snap.handle) == null)
					rep.numHandlesUnresolved++;
			}
		}

		return rep;
	}

}
