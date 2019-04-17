/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2019 JaamSim Software Inc.
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

import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.states.DowntimeUser;
import com.jaamsim.states.StateEntity;
import com.jaamsim.states.StateEntityListener;
import com.jaamsim.states.StateRecord;
import com.jaamsim.units.TimeUnit;

public class DowntimeEntity extends StateEntity implements StateEntityListener {

	@Keyword(description = "The calendar or working time for the first planned or unplanned "
	                     + "maintenance event. If an input is not provided, the first maintenance "
	                     + "event is determined by the input for the Interval keyword.",
	         exampleList = {"720 h", "UniformDistribution1" })
	private final SampleInput firstDowntime;

	@Keyword(description = "The object whose working time determines the occurrence of the "
	                     + "planned or unplanned maintenance events. Calendar time is used if "
	                     + "the input is left blank.",
	         exampleList = {"Object1"})
	private final EntityInput<StateEntity> iatWorkingEntity;

	@Keyword(description = "The object whose working time determines the completion of the "
	                     + "planned or unplanned maintenance activity. Calendar time is used if "
	                     + "the input is left blank.",
	         exampleList = {"Object1"})
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
	                     + "downtime event.",
	         exampleList = {"FALSE"})
	protected final BooleanInput concurrent;

	private final ArrayList<DowntimeUser> downtimeUserList;  // entities that use this downtime entity
	private boolean down;             // true for the duration of a downtime event
	private int downtimePendings;    // number of queued downtime events
	private double downtimePendingStartTime; // the simulation time in seconds at which the downtime pending started

	private double secondsForNextFailure;    // The number of working seconds required before the next downtime event
	private double secondsForNextRepair;    // The number of working seconds required before the downtime event ends

	private double startTime;        // The start time of the latest downtime event
	private double endTime;          // the end time of the latest downtime event

	private static final String STATE_DOWNTIME = "Downtime";

	{
		workingStateListInput.setHidden(true);

		firstDowntime = new SampleInput("FirstDowntime", KEY_INPUTS, null);
		firstDowntime.setUnitType(TimeUnit.class);
		this.addInput(firstDowntime);

		iatWorkingEntity = new EntityInput<>(StateEntity.class, "IntervalWorkingEntity", KEY_INPUTS, null);
		this.addInput(iatWorkingEntity);
		this.addSynonym(iatWorkingEntity, "IATWorkingEntity");

		durationWorkingEntity = new EntityInput<>(StateEntity.class, "DurationWorkingEntity", KEY_INPUTS, null);
		this.addInput(durationWorkingEntity);

		downtimeIATDistribution = new SampleInput("Interval", KEY_INPUTS, null);
		downtimeIATDistribution.setUnitType(TimeUnit.class);
		downtimeIATDistribution.setRequired(true);
		downtimeIATDistribution.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(downtimeIATDistribution);
		this.addSynonym(downtimeIATDistribution, "IAT");
		this.addSynonym(downtimeIATDistribution, "TimeBetweenFailures");

		downtimeDurationDistribution = new SampleInput("Duration", KEY_INPUTS, null);
		downtimeDurationDistribution.setUnitType(TimeUnit.class);
		downtimeDurationDistribution.setRequired(true);
		downtimeDurationDistribution.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(downtimeDurationDistribution);
		this.addSynonym(downtimeDurationDistribution, "TimeToRepair");

		concurrent = new BooleanInput("Concurrent", KEY_INPUTS, false);
		concurrent.setHidden(true);
		this.addInput(concurrent);
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
		startTime = 0;
		endTime = 0;

		if (!this.isActive())
			return;

		for (StateEntity each : getJaamSimModel().getClonesOfIterator(StateEntity.class, DowntimeUser.class)) {

			if (!each.isActive())
				continue;

			DowntimeUser du = (DowntimeUser)each;
			if (du.isDowntimeUser(this))
				downtimeUserList.add(du);
		}
	}

	@Override
	public void lateInit() {
		super.lateInit();

		// Determine the time for the first downtime event
		if (firstDowntime.getValue() == null)
			secondsForNextFailure = getNextDowntimeIAT();
		else
			secondsForNextFailure = firstDowntime.getValue().getNextSample(getSimTime());
	}

	@Override
    public void startUp() {
		super.startUp();
		checkProcessNetwork();
	}

	/**
	 * Get the name of the initial state this Entity will be initialized with.
	 * @return
	 */
	@Override
	public String getInitialState() {
		return STATE_WORKING;
	}

	/**
	 * Tests the given state name to see if it is valid for this Entity.
	 * @param state
	 * @return
	 */
	@Override
	public boolean isValidState(String state) {
		return STATE_WORKING.equals(state) || STATE_DOWNTIME.equals(state);
	}

	/**
	 * Tests the given state name to see if it is counted as working hours when in
	 * that state..
	 * @param state
	 * @return
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
	private final ProcessTarget endDowntime = new EndDowntimeTarget(this);
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
	private final ProcessTarget scheduleDowntime = new ScheduleDowntimeTarget(this);
	private final EventHandle scheduleDowntimeHandle = new EventHandle();

	/**
	 * Monitors the accumulation of time towards the start of the next maintenance activity or
	 * the completion of the present maintenance activity. This method is called whenever an
	 * entity affected by this type of maintenance changes state.
	 */
	public void checkProcessNetwork() {

		// Schedule the next downtime event
		if (!scheduleDowntimeHandle.isScheduled()) {

			// 1) Calendar time
			if (iatWorkingEntity.getValue() == null) {
				double workingSecs = this.getSimTime();
				double waitSecs = secondsForNextFailure - workingSecs;
				scheduleProcess(Math.max(waitSecs, 0.0), 5, scheduleDowntime, scheduleDowntimeHandle);

			}
			// 2) Working time
			else {
				if (iatWorkingEntity.getValue().isWorking()) {
					double workingSecs = iatWorkingEntity.getValue().getWorkingTime();
					double waitSecs = secondsForNextFailure - workingSecs;
					scheduleProcess(Math.max(waitSecs, 0.0), 5, scheduleDowntime, scheduleDowntimeHandle);
				}
			}
		}
		// the next event is already scheduled.  If the working entity has stopped working, need to cancel the event
		else {
			if (iatWorkingEntity.getValue() != null && !iatWorkingEntity.getValue().isWorking()) {
				EventManager.killEvent(scheduleDowntimeHandle);
			}
		}

		// 1) Determine when to end the current downtime event
		if (down) {

			if (durationWorkingEntity.getValue() == null) {

				if (endDowntimeHandle.isScheduled())
					return;

				// Calendar time
				double workingSecs = this.getSimTime();
				double waitSecs = secondsForNextRepair - workingSecs;
				scheduleProcess(waitSecs, 5, endDowntime, endDowntimeHandle);
				return;
			}

			// The Entity is working, schedule the end of the downtime event
			if (durationWorkingEntity.getValue().isWorking()) {

				if (endDowntimeHandle.isScheduled())
					return;

				double workingSecs = durationWorkingEntity.getValue().getWorkingTime();
				double waitSecs = secondsForNextRepair - workingSecs;
				scheduleProcess(waitSecs, 5, endDowntime, endDowntimeHandle);
			}
			// The Entity is not working, remove scheduled end of the downtime event
			else {
				EventManager.killEvent(endDowntimeHandle);
			}
		}
		// 2) Start the next downtime event if required/possible
		else {
			if (downtimePendings > 0) {

				// If all entities are ready, start the downtime event
				boolean allEntitiesCanStart = true;
				for (DowntimeUser each : downtimeUserList) {
					if (!each.canStartDowntime(this)) {
						allEntitiesCanStart = false;
						break;
					}
				}
				if (allEntitiesCanStart) {
					this.startDowntime();
				}
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
		downtimePendings++;
		if( downtimePendings == 1 )
			downtimePendingStartTime = this.getSimTime();

		// Determine the time the next downtime event is due
		// Calendar time based
		if( iatWorkingEntity.getValue() == null ) {
			secondsForNextFailure += this.getNextDowntimeIAT();
		}
		// Working time based
		else {
			secondsForNextFailure = iatWorkingEntity.getValue().getWorkingTime() + this.getNextDowntimeIAT();
		}

		// prepare all entities for the downtime event
		for (DowntimeUser each : downtimeUserList) {
			EventManager.startProcess(new PrepareForDowntimeTarget(this, each));
		}

		this.checkProcessNetwork();
	}

	/**
	 * When enough working hours have been accumulated by WorkingEntity, trigger all entities in downtimeUserList to perform downtime
	 */
	private void startDowntime() {
		setDown(true);

		startTime = this.getSimTime();
		downtimePendings--;

		// Determine the time when the downtime event will be over
		double downDuration = this.getDowntimeDuration();

		// Calendar time based
		if( durationWorkingEntity.getValue() == null ) {
			secondsForNextRepair = this.getSimTime() + downDuration;
		}
		// Working time based
		else {
			secondsForNextRepair = durationWorkingEntity.getValue().getWorkingTime() + downDuration;
		}

		endTime = startTime + downDuration;

		// Loop through all objects that this object is watching and trigger them to stop working.
		for (DowntimeUser each : downtimeUserList) {
			each.startDowntime(this);
		}

		this.checkProcessNetwork();
	}

	private void setDown(boolean b) {
		down = b;
		if (down)
			setPresentState(STATE_DOWNTIME);
		else
			setPresentState(STATE_WORKING);
	}

	final void endDowntime() {
		setDown(false);

		// Loop through all objects that this object is watching and try to restart them.
		for (DowntimeUser each : downtimeUserList) {
			each.endDowntime(this);
		}

		this.checkProcessNetwork();
	}

	/**
	 * Return the time in seconds of the next downtime IAT
	 */
	private double getNextDowntimeIAT() {
		return downtimeIATDistribution.getValue().getNextSample(getSimTime());
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
		return downtimeDurationDistribution.getValue().getNextSample(getSimTime());
	}

	public SampleProvider getDowntimeDurationDistribution() {
		return downtimeDurationDistribution.getValue();
	}

	public boolean isDown() {
		return down;
	}

	public boolean downtimePending() {
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

	/**
	 * Return the amount of time in seconds (from the current time) that the next downtime event is due
	 * @return
	 */
	public double getTimeUntilNextEvent() {

		// 1) Calendar time
		if( iatWorkingEntity.getValue() == null ) {
			double workingSecs = this.getSimTime();
			double waitSecs = secondsForNextFailure - workingSecs;
			return waitSecs;
		}
		// 2) Working time
		else {
			if (iatWorkingEntity.getValue().isWorking()) {
				double workingSecs = iatWorkingEntity.getValue().getWorkingTime();
				double waitSecs = secondsForNextFailure - workingSecs;
				return waitSecs;
			}
		}

		return Double.POSITIVE_INFINITY;
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

	// ******************************************************************************************************
	// OUTPUTS
	// ******************************************************************************************************

	@Output(name = "StartTime",
	 description = "The time that the most recent downtime event started.",
	    unitType = TimeUnit.class,
	    sequence = 1)
	public double getStartTime(double simTime) {
		return startTime;
	}

	@Output(name = "EndTime",
	 description = "The time that the most recent downtime event finished or will finish.",
	    unitType = TimeUnit.class,
	    sequence = 2)
	public double getEndTime(double simTime) {
		return endTime;
	}

	@Output(name = "NextStartTime",
	 description = "The time at which the next downtime event will begin. "
	             + "If downtime is based on the working time for an entity, then the next start "
	             + "time is estimated assuming that it will work continuously until the downtime "
	             + "event occurs.",
	    unitType = TimeUnit.class,
	    sequence = 3)
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
	    sequence = 4)
	public double getCalculatedDowntimeRatio(double simTime) {
		if (downtimeDurationDistribution.getValue() == null
				|| downtimeIATDistribution.getValue() == null)
			return Double.NaN;
		double dur = downtimeDurationDistribution.getValue().getMeanValue(simTime);
		double iat = downtimeIATDistribution.getValue().getMeanValue(simTime);
		return dur/iat;
	}

	@Output(name = "Availability",
	 description = "The fraction of calendar time (excluding the initialisation period) during "
	             + "which this type of downtime did not occur.",
	    sequence = 5)
	public double getAvailability(double simTime) {
		double total = simTime;
		if (simTime > getSimulation().getInitializationTime())
			total -= getSimulation().getInitializationTime();
		double down = this.getTimeInState(simTime, STATE_DOWNTIME);
		return 1.0d - down/total;
	}

}
