/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2024 JaamSim Software Inc.
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
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Samples.SampleListInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.InterfaceEntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.resourceObjects.AbstractResourceProvider;
import com.jaamsim.resourceObjects.ResourceProvider;
import com.jaamsim.resourceObjects.ResourceUser;
import com.jaamsim.resourceObjects.ResourceUserDelegate;
import com.jaamsim.states.DowntimeUser;
import com.jaamsim.states.StateEntity;
import com.jaamsim.states.StateEntityListener;
import com.jaamsim.states.StateRecord;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

public class DowntimeEntity extends StateEntity implements StateEntityListener, ResourceUser {

	@Keyword(description = "The calendar or working time for the first planned or unplanned "
	                     + "maintenance event. If an input is not provided, the first maintenance "
	                     + "event is determined by the input for the Interval keyword.",
	         exampleList = {"720 h", "UniformDistribution1" })
	private final SampleInput firstDowntime;

	@Keyword(description = "The object whose working time determines the occurrence of the "
	                     + "planned or unplanned maintenance events. Calendar time is used if "
	                     + "the input is left blank.")
	private final EntityInput<StateEntity> iatWorkingEntity;

	@Keyword(description = "The object whose working time determines the completion of the "
	                     + "planned or unplanned maintenance activity. Calendar time is used if "
	                     + "the input is left blank.")
	private final EntityInput<StateEntity> durationWorkingEntity;

	@Keyword(description = "The calendar or working time between the start of the last planned or "
	                     + "unplanned maintenance activity and the start of the next maintenance "
	                     + "activity.",
	         exampleList = {"168 h", "IntervalValueSequence", "IntervalDistribution" })
	private final SampleInput downtimeIATDistribution;

	@Keyword(description = "The calendar or working time required to complete the planned or "
	                     + "unplanned maintenance activity.",
	         exampleList = {"8 h ", "DurationValueSequence", "DurationDistribution" })
	private final SampleInput downtimeDurationDistribution;

	@Keyword(description = "If TRUE, the downtime event can occur in parallel with another "
	                     + "downtime event.")
	protected final BooleanProvInput concurrent;

	@Keyword(description = "The maximum number of downtime activities that are allowed to become "
	                     + "backlogged. "
	                     + "Once this limit is reached, any further downtime activities "
	                     + "are discarded.",
	         exampleList = {"1"})
	protected final SampleInput maxDowntimesPending;

	@Keyword(description = "The total time from the scheduled start time that the downtime event "
	                     + "should be completed within. "
	                     + "For example, if the scheduled start time is 100 h and the completion "
	                     + "time limit is 48 h, the event will be recorded as late in the "
	                     + "'LateEvents' output if it is not completed by 148h.",
	         exampleList = {"48 h"})
	protected final SampleInput completionTimeLimit;

	@Keyword(description = "Resources required to perform the maintenance process. "
	                     + "If any of the resources are not available at the start of downtime, "
	                     + "the maintenance duration will be delayed until the resources can be "
	                     + "seized. "
	                     + "All the resource units must be available to be seized before any one "
	                     + "unit is seized.",
	         exampleList = {"Resource1 Resource2"})
	protected final InterfaceEntityListInput<ResourceProvider> resourceList;

	@Keyword(description = "The number of units to seize from the Resources specified by the "
	                     + "'ResourceList' keyword. "
	                     + "The last value in the list is used if the number of resources is "
	                     + "greater than the number of values. "
	                     + "Only an integer number of resource units can be seized. "
	                     + "A decimal value will be truncated to an integer.",
	         exampleList = {"2 1", "{ 2 } { 1 }", "{ DiscreteDistribution1 } { 'this.obj.attrib1 + 1' }"})
	private final SampleListInput numberOfUnitsList;

	private final ArrayList<DowntimeUser> downtimeUserList;  // entities that use this downtime entity
	private boolean down;             // true for the duration of a downtime event
	private int downtimePendings;    // number of queued downtime events
	private double downtimePendingStartTime; // the simulation time in seconds at which the downtime pending started

	private double secondsForNextFailure;    // The number of working seconds required before the next downtime event
	private double secondsForNextRepair;    // The number of working seconds required before the downtime event ends

	private int numberStarted;       // Number of downtime events that have been started
	private int numberCompleted;     // Number of downtime events that have been completed
	private double startTime;        // The start time of the latest downtime event
	private double endTime;          // the end time of the latest downtime event
	private double downDuration;     // repair time for the latest downtime event

	private static final String STATE_DOWNTIME = "Downtime";

	private int numLateEvents;    // Number of events that did not finish within the completion time limit
	private double targetCompletionTime; // the time that the latest downtime event should be completed

	private double totalLateTime;  // Total time after completion time limit that the downtime took to complete

	private ResourceUserDelegate resUserDelegate;
	private int[] seizedUnits = new int[0];

	{
		workingStateListInput.setHidden(true);

		firstDowntime = new SampleInput("FirstDowntime", KEY_INPUTS, Double.NaN);
		firstDowntime.setUnitType(TimeUnit.class);
		firstDowntime.setOutput(true);
		this.addInput(firstDowntime);

		iatWorkingEntity = new EntityInput<>(StateEntity.class, "IntervalWorkingEntity", KEY_INPUTS, null);
		this.addInput(iatWorkingEntity);
		this.addSynonym(iatWorkingEntity, "IATWorkingEntity");

		durationWorkingEntity = new EntityInput<>(StateEntity.class, "DurationWorkingEntity", KEY_INPUTS, null);
		this.addInput(durationWorkingEntity);

		downtimeIATDistribution = new SampleInput("Interval", KEY_INPUTS, Double.NaN);
		downtimeIATDistribution.setUnitType(TimeUnit.class);
		downtimeIATDistribution.setRequired(true);
		downtimeIATDistribution.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		downtimeIATDistribution.setOutput(true);
		this.addInput(downtimeIATDistribution);
		this.addSynonym(downtimeIATDistribution, "IAT");
		this.addSynonym(downtimeIATDistribution, "TimeBetweenFailures");

		downtimeDurationDistribution = new SampleInput("Duration", KEY_INPUTS, Double.NaN);
		downtimeDurationDistribution.setUnitType(TimeUnit.class);
		downtimeDurationDistribution.setRequired(true);
		downtimeDurationDistribution.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		downtimeDurationDistribution.setOutput(true);
		this.addInput(downtimeDurationDistribution);
		this.addSynonym(downtimeDurationDistribution, "TimeToRepair");

		concurrent = new BooleanProvInput("Concurrent", KEY_INPUTS, false);
		this.addInput(concurrent);

		maxDowntimesPending = new SampleInput("MaxDowntimesPending", "Key Inputs", Double.POSITIVE_INFINITY);
		maxDowntimesPending.setValidRange(1, Double.POSITIVE_INFINITY);
		maxDowntimesPending.setIntegerValue(true);
		maxDowntimesPending.setOutput(true);
		this.addInput(maxDowntimesPending);

		completionTimeLimit = new SampleInput("CompletionTimeLimit", KEY_INPUTS, Double.POSITIVE_INFINITY);
		completionTimeLimit.setUnitType(TimeUnit.class);
		completionTimeLimit.setOutput(true);
		this.addInput(completionTimeLimit);

		ArrayList<ResourceProvider> resDef = new ArrayList<>();
		resourceList = new InterfaceEntityListInput<>(ResourceProvider.class, "ResourceList", KEY_INPUTS, resDef);
		this.addInput(resourceList);

		numberOfUnitsList = new SampleListInput("NumberOfUnits", KEY_INPUTS, 1);
		numberOfUnitsList.setValidRange(0, Double.POSITIVE_INFINITY);
		numberOfUnitsList.setDimensionless(true);
		numberOfUnitsList.setUnitType(DimensionlessUnit.class);
		numberOfUnitsList.setIntegerValue(true);
		this.addInput(numberOfUnitsList);
	}

	public DowntimeEntity(){
		downtimeUserList = new ArrayList<>();
	}

	@Override
	public void earlyInit(){
		super.earlyInit();

		down = false;
		downtimeUserList.clear();
		downtimePendings = 0;
		downtimePendingStartTime = 0.0;
		numberStarted = 0;
		numberCompleted = 0;
		startTime = 0;
		endTime = 0;
		numLateEvents = 0;
		totalLateTime = 0;

		resUserDelegate = new ResourceUserDelegate(resourceList.getValue());
		seizedUnits = new int[0];

		if (!this.isActive())
			return;

		for (StateEntity each : getJaamSimModel().getClonesOfIterator(StateEntity.class, DowntimeUser.class)) {

			if (!each.isActive())
				continue;

			DowntimeUser du = (DowntimeUser)each;
			if (du.isDowntimeUser(this))
				registerDowntimeUser(du);
		}
	}

	@Override
	public void lateInit() {
		super.lateInit();

		// Determine the time for the first downtime event
		if (firstDowntime.isDefault())
			secondsForNextFailure = getNextDowntimeIAT();
		else
			secondsForNextFailure = firstDowntime.getNextSample(this, getSimTime());
	}

	public void registerDowntimeUser(DowntimeUser du) {
		if (!isActive() || downtimeUserList.contains(du))
			return;
		downtimeUserList.add(du);
	}

	public void unregisterDowntimeUser(DowntimeUser du) {
		downtimeUserList.remove(du);
	}

	@Override
    public void startUp() {
		super.startUp();
		checkProcessNetwork();
	}

	@Override
	public void clearStatistics() {
		super.clearStatistics();
		numberStarted = 0;
		numberCompleted = 0;
		numLateEvents = 0;
		totalLateTime = 0;
	}

	/**
	 * Get the name of the initial state this Entity will be initialized with.
	 */
	@Override
	public String getInitialState() {
		return STATE_WORKING;
	}

	/**
	 * Tests the given state name to see if it is valid for this Entity.
	 * @param state
	 */
	@Override
	public boolean isValidState(String state) {
		return STATE_WORKING.equals(state) || STATE_DOWNTIME.equals(state);
	}

	/**
	 * Tests the given state name to see if it is counted as working hours when in
	 * that state..
	 * @param state
	 */
	@Override
	public boolean isValidWorkingState(String state) {
		return STATE_WORKING.equals(state);
	}

	/**
	 * EndDowntimeTarget
	 */
	private static class EndDowntimeTarget extends EntityTarget<DowntimeEntity> {
		EndDowntimeTarget(DowntimeEntity ent) {
			super(ent, "endDowntime");
		}

		@Override
		public void process() {
			ent.endDowntime();
		}
	}
	private final ProcessTarget endDowntimeTarget = new EndDowntimeTarget(this);
	private final EventHandle endDowntimeHandle = new EventHandle();

	/**
	 * ScheduleDowntimeTarget
	 */
	private static class ScheduleDowntimeTarget extends EntityTarget<DowntimeEntity> {
		ScheduleDowntimeTarget(DowntimeEntity ent) {
			super(ent, "scheduleDowntime");
		}

		@Override
		public void process() {
			ent.scheduleDowntime();
		}
	}
	private final ProcessTarget scheduleDowntimeTarget = new ScheduleDowntimeTarget(this);
	private final EventHandle scheduleDowntimeHandle = new EventHandle();

	/**
	 * Monitors the accumulation of time towards the start of the next maintenance activity or
	 * the completion of the present maintenance activity. This method is called whenever an
	 * entity affected by this type of maintenance changes state.
	 */
	public void checkProcessNetwork() {

		// Schedule the next downtime event
		StateEntity iatWorkingEnt = iatWorkingEntity.getValue();
		if (!scheduleDowntimeHandle.isScheduled()) {
			if (iatWorkingEnt == null || iatWorkingEnt.isWorkingState()) {
				double workingSecs = getSimTime();
				if (iatWorkingEnt != null)
					workingSecs = iatWorkingEnt.getWorkingTime();
				double waitSecs = Math.max(secondsForNextFailure - workingSecs, 0.0d);
				scheduleProcess(waitSecs, 5, scheduleDowntimeTarget, scheduleDowntimeHandle);
			}
		}
		// the next event is already scheduled.  If the working entity has stopped working, need to cancel the event
		else {
			if (iatWorkingEnt != null && !iatWorkingEnt.isWorkingState()) {
				EventManager.killEvent(scheduleDowntimeHandle);
			}
		}

		// Seize resources
		if (isWaitingForResources()) {
			double simTime = getSimTime();
			int[] nums = numberOfUnitsList.getNextIntegers(this, simTime, resUserDelegate.getListSize());
			if (!resUserDelegate.canSeizeResources(simTime, nums, this))
				return;
			resUserDelegate.seizeResources(nums, this);
			seizedUnits = nums;

			// Determine the time when the downtime event will be over
			StateEntity durWorkingEnt = durationWorkingEntity.getValue();
			secondsForNextRepair = simTime + downDuration;
			if (durWorkingEnt != null)
				secondsForNextRepair = durWorkingEnt.getWorkingTime() + downDuration;

			endTime = simTime + downDuration;
		}

		// 1) Determine when to end the current downtime event
		if (down) {
			StateEntity durWorkingEnt = durationWorkingEntity.getValue();
			if (durWorkingEnt == null || durWorkingEnt.isWorkingState()) {
				if (endDowntimeHandle.isScheduled())
					return;
				double workingSecs = this.getSimTime();
				if (durWorkingEnt != null)
					workingSecs = durWorkingEnt.getWorkingTime();
				double waitSecs = secondsForNextRepair - workingSecs;
				scheduleProcess(waitSecs, 5, endDowntimeTarget, endDowntimeHandle);
				return;
			}

			// The Entity is not working, remove scheduled end of the downtime event
			if (durWorkingEnt != null && !durWorkingEnt.isWorkingState()) {
				EventManager.killEvent(endDowntimeHandle);
			}
		}

		// 2) Start the next downtime event if required/possible
		else {
			if (downtimePendings > 0 && canStartDowntime()) {
				startDowntime();
			}
		}
	}

	// PrepareForDowntimeTarget
	private static final class PrepareForDowntimeTarget extends ProcessTarget {
		private final DowntimeEntity ent;
		private final DowntimeUser user;

		public PrepareForDowntimeTarget(DowntimeEntity e, DowntimeUser u) {
			ent = e;
			user = u;
		}

		@Override
		public void process() {
			user.prepareForDowntime(ent);
		}

		@Override
		public String getDescription() {
			return user.getName() + ".prepareForDowntime";
		}
	}

	public void scheduleDowntime() {
		double simTime = getSimTime();
		if (downtimePendings == getMaxDowntimesPending(simTime))
			return;

		downtimePendings++;
		if( downtimePendings == 1 )
			downtimePendingStartTime = simTime;

		targetCompletionTime = simTime + completionTimeLimit.getNextSample(this, simTime);

		// Determine the time the next downtime event is due
		// Calendar time based
		StateEntity iatWorkingEnt = iatWorkingEntity.getValue();
		if (iatWorkingEnt == null) {
			secondsForNextFailure += this.getNextDowntimeIAT();
		}
		// Working time based
		else {
			secondsForNextFailure = iatWorkingEnt.getWorkingTime() + getNextDowntimeIAT();
		}

		// prepare all entities for the downtime event
		for (DowntimeUser each : downtimeUserList) {
			EventManager.startProcess(new PrepareForDowntimeTarget(this, each));
		}

		this.checkProcessNetwork();
	}

	public boolean canStartDowntime() {
		for (DowntimeUser each : downtimeUserList) {
			if (!each.canStartDowntime(this)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * When enough working hours have been accumulated by WorkingEntity, trigger all entities in downtimeUserList to perform downtime
	 */
	private void startDowntime() {
		setDown(true);

		startTime = this.getSimTime();
		downtimePendings--;
		numberStarted++;

		// Determine the time when the downtime event will be over
		downDuration = getDowntimeDuration();
		StateEntity durWorkingEnt = durationWorkingEntity.getValue();
		secondsForNextRepair = getSimTime() + downDuration;
		if (durWorkingEnt != null)
			secondsForNextRepair = durWorkingEnt.getWorkingTime() + downDuration;

		endTime = startTime + downDuration;

		// Loop through all objects that this object is watching and trigger them to stop working.
		for (DowntimeUser each : downtimeUserList) {
			each.startDowntime(this);
		}

		this.checkProcessNetwork();
	}

	private void setDown(boolean b) {
		if (isTraceFlag()) this.trace(1, "setDown(%s)", b);
		down = b;
		if (down)
			setPresentState(STATE_DOWNTIME);
		else
			setPresentState(STATE_WORKING);
	}

	final void endDowntime() {
		setDown(false);

		numberCompleted++;

		// Release resources
		resUserDelegate.releaseResources(seizedUnits, this);
		seizedUnits = new int[0];

		// Loop through all objects that this object is watching and try to restart them.
		for (DowntimeUser each : downtimeUserList) {
			each.endDowntime(this);
		}

		// Notify any resource users that are waiting for these Resources
		AbstractResourceProvider.notifyResourceUsers(resUserDelegate.getResourceList());

		// If this event was late, increment counter
		if(this.getSimTime() > targetCompletionTime ) {
			numLateEvents++;
			totalLateTime += (this.getSimTime() - targetCompletionTime);
		}

		this.checkProcessNetwork();
	}

	/**
	 * Return the time in seconds of the next downtime IAT
	 */
	private double getNextDowntimeIAT() {
		return downtimeIATDistribution.getNextSample(this, getSimTime());
	}

	/**
	 * Return the expected time in seconds of the first downtime
	 */
	public double getExpectedFirstDowntime() {
		return firstDowntime.getValue().getMeanValue( getSimTime() );
	}

	/**
	 * Return the expected time in seconds of the downtime IAT
	 */
	public double getExpectedDowntimeIAT() {
		return downtimeIATDistribution.getValue().getMeanValue( getSimTime() );
	}

	/**
	 * Return the expected time in seconds of the downtime duration
	 */
	public double getExpectedDowntimeDuration() {
		return downtimeDurationDistribution.getValue().getMeanValue( getSimTime() );
	}

	/**
	 * Return the time in seconds of the next downtime duration
	 */
	private double getDowntimeDuration() {
		return downtimeDurationDistribution.getNextSample(this, getSimTime());
	}

	public SampleProvider getDowntimeDurationDistribution() {
		return downtimeDurationDistribution.getValue();
	}

	public boolean isDown() {
		return down;
	}

	/**
	 * Returns whether the downtime event is ready to begin.
	 * @return true if downtime can begin
	 */
	public boolean isDowntimePending() {
		return downtimePendings > 0;
	}

	@Override
	public boolean isWatching(StateEntity ent) {
		if (!this.isActive())
			return false;

		if (iatWorkingEntity.getValue() == ent)
			return true;

		if (durationWorkingEntity.getValue() == ent)
			return true;

		if (downtimeUserList.contains(ent))
			return true;

		return false;
	}

	@Override
	public void updateForStateChange(StateEntity ent, StateRecord prev, StateRecord next) {
		this.checkProcessNetwork();
	}

	public double getEndTime() {
		return endTime;
	}

	public double getDowntimePendingStartTime() {
		return downtimePendingStartTime;
	}

	public ArrayList<DowntimeUser> getDowntimeUserList() {
		return downtimeUserList;
	}

	public boolean isConcurrent(double simTime) {
		return concurrent.getNextBoolean(this, simTime);
	}

	public int getMaxDowntimesPending(double simTime) {
		return (int) maxDowntimesPending.getNextSample(this, simTime);
	}

	public boolean isWaitingForResources() {
		return down && !resUserDelegate.isEmpty() && seizedUnits.length == 0;
	}

	@Override
	public boolean requiresResource(ResourceProvider res) {
		return resUserDelegate.requiresResource(res);
	}

	@Override
	public boolean hasWaitingEntity() {
		return isWaitingForResources();
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public double getWaitTime() {
		double ret = 0.0d;
		if (isWaitingForResources())
			ret = getSimTime() - startTime;
		return ret;
	}

	@Override
	public boolean isReadyToStart() {
		double simTime = getSimTime();
		int[] nums = numberOfUnitsList.getNextIntegers(this, simTime, resUserDelegate.getListSize());
		return resUserDelegate.canSeizeResources(simTime, nums, this);
	}

	@Override
	public void startNextEntity() {
		checkProcessNetwork();
	}

	@Override
	public boolean hasStrictResource() {
		return resUserDelegate.hasStrictResource();
	}

	// ******************************************************************************************************
	// OUTPUTS
	// ******************************************************************************************************

	@Output(name = "UserList",
	 description = "The objects that experience breakdowns or maintenance caused by this "
	             + "DowntimeEntity.",
	    sequence = 0)
	public ArrayList<DowntimeUser> getUserList(double simTime) {
		return downtimeUserList;
	}

	@Output(name = "NumberPending",
	 description = "The number of downtime events that are backlogged. "
	             + "If two or more downtime events are pending they will be performed one after "
	             + "another.",
	    sequence = 1)
	public int getNumberPending(double simTime) {
		return downtimePendings;
	}

	@Output(name = "NumberStarted",
	 description = "The number of downtime events that have been started, including ones that "
	             + "have been completed. "
	             + "Excludes downtimes that were started during the initialization period.",
	    sequence = 2)
	public int getNumberStarted(double simTime) {
		return numberStarted;
	}

	@Output(name = "NumberCompleted",
	 description = "The number of downtime events that have been completed. "
	             + "Excludes downtimes that were completed during the initialization period.",
	    sequence = 3)
	public int getNumberCompleted(double simTime) {
		return numberCompleted;
	}

	@Output(name = "StartTime",
	 description = "The time that the most recent downtime event started.",
	    unitType = TimeUnit.class,
	    sequence = 4)
	public double getStartTime(double simTime) {
		return startTime;
	}

	@Output(name = "EndTime",
	 description = "The time that the most recent downtime event finished or will finish.",
	    unitType = TimeUnit.class,
	    sequence = 5)
	public double getEndTime(double simTime) {
		return endTime;
	}

	@Output(name = "NextStartTime",
	 description = "The time at which the next downtime event will begin. "
	             + "If downtime is based on the working time for an entity, then the next start "
	             + "time is estimated assuming that it will work continuously until the downtime "
	             + "event occurs.",
	    unitType = TimeUnit.class,
	    sequence = 6)
	public double getNextStartTime(double simTime) {
		StateEntity ent = iatWorkingEntity.getValue();

		// 1) Calendar time
		if (ent == null) {
			return secondsForNextFailure;
		}

		// 2) Working time
		if (isDown())
			return endTime + (secondsForNextFailure - ent.getWorkingTime(simTime));
		return simTime + (secondsForNextFailure - ent.getWorkingTime(simTime));
	}

	@Output(name = "CalculatedDowntimeRatio",
	 description = "The value calculated directly from model inputs for:\n"
	             + "(avg. downtime duration)/(avg. downtime interval)",
	    sequence = 7)
	public double getCalculatedDowntimeRatio(double simTime) {
		if (downtimeDurationDistribution.isDefault()
				|| downtimeIATDistribution.isDefault())
			return Double.NaN;
		double dur = downtimeDurationDistribution.getValue().getMeanValue(simTime);
		double iat = downtimeIATDistribution.getValue().getMeanValue(simTime);
		return dur/iat;
	}

	@Output(name = "Availability",
	 description = "The fraction of calendar time (excluding the initialisation period) during "
	             + "which this type of downtime did not occur.",
	    sequence = 8)
	public double getAvailability(double simTime) {
		double total = simTime;
		if (simTime > getSimulation().getInitializationTime())
			total -= getSimulation().getInitializationTime();
		double down = this.getTimeInState(simTime, STATE_DOWNTIME);
		return 1.0d - down/total;
	}

	@Output(name = "LateEvents",
	 description = "Number of events that did not finish within the Completion Time limit.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 9)
	public int getLateEvents(double simTime) {
		return numLateEvents;
	}

	@Output(name = "TotalLateTime",
	 description = "Total hours after completion time limit that the downtime took to complete.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 10)
	public double getTotalLateTime(double simTime) {
		return totalLateTime;
	}
}
