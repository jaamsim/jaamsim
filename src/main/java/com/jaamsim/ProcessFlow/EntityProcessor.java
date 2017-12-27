/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017 JaamSim Software Inc.
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
package com.jaamsim.ProcessFlow;

import java.util.ArrayList;
import java.util.Arrays;

import com.jaamsim.BasicObjects.DowntimeEntity;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.TimeUnit;

public class EntityProcessor extends Seize {

	@Keyword(description = "The service time required to process an entity.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput serviceTime;

	private final ArrayList<ProcessorEntry> entryList;  // List of the entities being processed
	private final ArrayList<ProcessorEntry> newEntryList;  // List of the entities to add to entryList

	{
		trace.setHidden(false);

		processPosition.setHidden(false);
		workingStateListInput.setHidden(false);
		immediateMaintenanceList.setHidden(false);
		forcedMaintenanceList.setHidden(false);
		opportunisticMaintenanceList.setHidden(false);
		immediateBreakdownList.setHidden(false);
		forcedBreakdownList.setHidden(false);
		opportunisticBreakdownList.setHidden(false);

		serviceTime = new SampleInput("ServiceTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setEntity(this);
		serviceTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(serviceTime);
	}

	public EntityProcessor() {
		entryList = new ArrayList<>();
		newEntryList = new ArrayList<>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		entryList.clear();
		newEntryList.clear();
	}

	private static class ProcessorEntry {
		final DisplayEntity entity;
		final int[] resourceUnits;
		long remainingTicks;

		public ProcessorEntry(DisplayEntity ent, int[] units, long ticks) {
			entity = ent;
			resourceUnits = units;
			remainingTicks = ticks;
		}

		@Override
		public String toString() {
			return String.format("(%s, %s, %s)",
					entity, Arrays.toString(resourceUnits), remainingTicks);
		}
	}

	@Override
	public void startNextEntity() {
		if (isTraceFlag()) trace(2, "startNextEntity");

		// Remove the first entity from the queue
		double simTime = getSimTime();
		String m = this.getNextMatchValue(simTime);
		DisplayEntity ent = waitQueue.getValue().removeFirstForMatch(m);
		if (ent == null)
			error("Entity not found for specified Match value: %s", m);
		this.registerEntity(ent);

		// Seize the resources and pass the entity to the next component
		this.seizeResources();

		// Set the service duration
		double dur = serviceTime.getValue().getNextSample(simTime);
		long ticks = EventManager.secsToNearestTick(dur);

		// Add the entity to the list of entities to be processed
		newEntryList.add(new ProcessorEntry(ent, getSeizedUnits(simTime), ticks));
		if (isTraceFlag()) traceLine(3, "newEntryList=%s", newEntryList);

		// Interrupt the processing loop
		this.performUnscheduledUpdate();
	}

	@Override
	protected boolean startProcessing(double simTime) {
		if (isTraceFlag()) trace(2, "startProcessing");

		// Add any new entries
		if (!newEntryList.isEmpty()) {
			entryList.addAll(newEntryList);
			newEntryList.clear();
			if (isTraceFlag()) traceLine(3, "entryList=%s", entryList);
		}

		// Stop when there are no entities to process
		return !entryList.isEmpty();  // return true to continue
	}

	@Override
	protected double getStepDuration(double simTime) {
		long ticks = Long.MAX_VALUE;
		for (ProcessorEntry entry : entryList) {
			ticks = Math.min(ticks, entry.remainingTicks);
		}
		return EventManager.ticksToSecs(ticks);
	}

	@Override
	public void updateProgress(double dt) {
		if (isTraceFlag()) trace(2, "updateProgress(%s)", dt);

		// Decrement the remaining durations for each of the entities
		if (isTraceFlag()) traceLine(3, "BEFORE - entryList=%s", entryList);
		long delta = EventManager.secsToNearestTick(dt);
		for (ProcessorEntry entry : entryList) {
			entry.remainingTicks -= delta;
		}
		if (isTraceFlag()) traceLine(3, "AFTER  - entryList=%s", entryList);
	}

	@Override
	protected boolean processStep(double simTime) {

		// Identify the entities whose processing is finished
		ArrayList<ProcessorEntry> completedEntries = new ArrayList<>(entryList.size());
		for (ProcessorEntry entry : entryList) {
			if (entry.remainingTicks <= 0L) {
				completedEntries.add(entry);
			}
		}
		entryList.removeAll(completedEntries);

		// Release the resources for each entity
		for (ProcessorEntry entry : completedEntries) {
			for (int i = 0; i < entry.resourceUnits.length; i++) {
				getResourceList().get(i).release(entry.resourceUnits[i]);
			}
		}

		// Notify any resource users that are waiting for these Resources
		Resource.notifyResourceUsers(getResourceList());

		// Pass the entities to the next component
		for (ProcessorEntry entry : completedEntries) {
			this.sendToNextComponent(entry.entity);
		}

		return true;
	}

	@Override
	protected boolean isNewStepReqd(boolean completed) {
		return true;
	}

	@Override
	public void thresholdChanged() {
		if (isReadyToStart()) {
			Resource.notifyResourceUsers(getResourceList());
		}
		if (isImmediateReleaseThresholdClosure()) {
			for (ProcessorEntry entry : entryList) {
				entry.remainingTicks = 0L;
			}
		}
		super.thresholdChanged();
	}

	@Override
	public boolean isReadyForDowntime() {
		return isImmediateDowntimePending() || (isForcedDowntimePending() && entryList.isEmpty());
	}

	@Override
	public void endDowntime(DowntimeEntity down) {
		if (isReadyToStart()) {
			Resource.notifyResourceUsers(getResourceList());
		}
		super.endDowntime(down);
	}

	@Override
	public void updateGraphics(double simTime) {
		for (ProcessorEntry entry : entryList) {
			moveToProcessPosition(entry.entity);
		}
	}

}
