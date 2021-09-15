/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2018 JaamSim Software Inc.
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
import com.jaamsim.Samples.TimeSeries;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.resourceObjects.AbstractResourceProvider;
import com.jaamsim.ui.Federate;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

public class EntityProcessor extends Seize {

	@Keyword(description = "The maximum number of entities that can be processed simultaneously.\n"
	                     + "If the capacity changes during the simulation run, the EntityProcessor "
	                     + "will attempt to use an increase in capacity as soon as it occurs. "
	                     + "However, a decrease in capacity will have no affect on entities that "
	                     + "have already started processing.",
	         exampleList = {"3", "TimeSeries1", "this.attrib1"})
	private final SampleInput capacity;

	@Keyword(description = "The service time required to process an entity.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput serviceTime;

	private final ArrayList<ProcessorEntry> entryList;  // List of the entities being processed
	private final ArrayList<ProcessorEntry> newEntryList;  // List of the entities to add to entryList
	private int lastCapacity; // Last recorded value for capacity

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

		immediateThresholdList.setHidden(false);
		immediateReleaseThresholdList.setHidden(false);

		resourceList.setRequired(false);

		capacity = new SampleInput("Capacity", KEY_INPUTS, new SampleConstant(1.0));
		capacity.setUnitType(DimensionlessUnit.class);
		capacity.setEntity(this);
		capacity.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(capacity);

		serviceTime = new SampleInput("ServiceTime", KEY_INPUTS, new SampleConstant(TimeUnit.class, 0.0));
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

	@Override
	public void startUp() {
		super.startUp();

		if (capacity.getValue() instanceof SampleConstant)
			return;

		// Track any changes in capacity
		this.waitForCapacityChange();
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
	public void queueChanged() {
		if (getResourceList().isEmpty()) {
			while (isReadyToStart()) {
				startNextEntity();
			}
		}
		else {
			super.queueChanged();
		}
	}

	@Override
	public boolean isReadyToStart() {
		if (entryList.size() + newEntryList.size() >= getCapacity(getSimTime())) {
			return false;
		}
		return super.isReadyToStart();
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
	protected void processStep(double simTime) {

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
				getResourceList().get(i).release(entry.resourceUnits[i], entry.entity);
			}
		}

		// Notify any resource users that are waiting for these Resources
		if (getResourceList().isEmpty()) {
			while (isReadyToStart()) {
				startNextEntity();
			}
		}
		else {
			AbstractResourceProvider.notifyResourceUsers(getResourceList());
		}

		// Pass the entities to the next component
		for (ProcessorEntry entry : completedEntries) {
			
			//added by Jalal
			ArrayList<String> atts = this.getAttributeNames();
			if(atts.size() > 0 && atts.get(0).equals("waitRTIOrder")) 
				Federate.instance.onAddEntity(this);
			//till here
			
			this.sendToNextComponent(entry.entity);
		}
	}

	@Override
	protected boolean isNewStepReqd(boolean completed) {
		return true;
	}

	@Override
	public void thresholdChanged() {
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
			AbstractResourceProvider.notifyResourceUsers(getResourceList());
		}
		super.endDowntime(down);
	}

	/**
	 * Returns true if the saved capacity differs from the present capacity
	 * @return true if the capacity has changed
	 */
	boolean isCapacityChanged() {
		return this.getCapacity(getSimTime()) != lastCapacity;
	}

	/**
	 * Loops from one capacity change to the next.
	 */
	void waitForCapacityChange() {

		// Set the present capacity
		lastCapacity = this.getCapacity(getSimTime());

		// Wait until the state is ready to change
		if (capacity.getValue() instanceof TimeSeries) {
			TimeSeries ts = (TimeSeries)capacity.getValue();
			long simTicks = getSimTicks();
			long durTicks = ts.getNextChangeAfterTicks(simTicks) - simTicks;
			this.scheduleProcessTicks(durTicks, 10, true, updateForCapacityChangeTarget, null); // FIFO
		}
		else {
			EventManager.scheduleUntil(updateForCapacityChangeTarget, capacityChangeConditional, null);
		}
	}

	/**
	 * Responds to a change in capacity.
	 */
	void updateForCapacityChange() {
		if (isTraceFlag()) trace(0, "updateForCapacityChange");

		// Select the resource users to notify
		if (getCapacity(getSimTime()) > lastCapacity) {
			if (getResourceList().isEmpty()) {
				while (isReadyToStart()) {
					startNextEntity();
				}
			}
			else {
				AbstractResourceProvider.notifyResourceUsers(getResourceList());
			}
		}

		// Wait for the next capacity change
		this.waitForCapacityChange();
	}

	// Conditional for isCapacityChanged()
	class CapacityChangeConditional extends Conditional {
		@Override
		public boolean evaluate() {
			return EntityProcessor.this.isCapacityChanged();
		}
	}
	private final Conditional capacityChangeConditional = new CapacityChangeConditional();

	// Target for updateForCapacityChange()
	class UpdateForCapacityChangeTarget extends ProcessTarget {
		@Override
		public String getDescription() {
			return EntityProcessor.this.getName() + ".updateForCapacityChange";
		}

		@Override
		public void process() {
			EntityProcessor.this.updateForCapacityChange();
		}
	}
	private final ProcessTarget updateForCapacityChangeTarget = new UpdateForCapacityChangeTarget();

	@Override
	public void updateGraphics(double simTime) {
		for (ProcessorEntry entry : entryList) {
			moveToProcessPosition(entry.entity);
		}
	}

	@Output(name = "Capacity",
	 description = "The present number of entities that can be processed simultaneously.",
	    unitType = DimensionlessUnit.class,
	    sequence = 0)
	public int getCapacity(double simTime) {
		return (int) capacity.getValue().getNextSample(simTime);
	}

	@Output(name = "UnitsInUse",
	 description = "The present number of capacity units that are being used.",
	    unitType = DimensionlessUnit.class,
	    sequence = 1)
	public int getUnitsInUse(double simTime) {
		return entryList.size();
	}

	@Output(name = "EntityList",
	 description = "The entities being processed at present.",
	    sequence = 2)
	public ArrayList<DisplayEntity> getEntityList(double simTime) {
		ArrayList<DisplayEntity> ret = new ArrayList<>(entryList.size());
		for (ProcessorEntry entry : entryList) {
			ret.add(entry.entity);
		}
		return ret;
	}

	@Output(name = "RemainingTime",
	 description = "The remaining processing time for the entities being processed at present.",
	    sequence = 3)
	public double[] getRemainingTime(double simTime) {
		double[] ret = new double[entryList.size()];
		double dt = 0.0d;
		if (isBusy()) {
			dt = simTime - getLastUpdateTime();
		}
		for (int i = 0; i < entryList.size(); i++) {
			ret[i] = EventManager.ticksToSecs(entryList.get(i).remainingTicks) - dt;
		}
		return ret;
	}

}
