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
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

import com.jaamsim.Samples.SampleInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.EnumInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.states.DowntimeUser;
import com.jaamsim.states.StateEntity;
import com.jaamsim.states.StateEntityListener;
import com.jaamsim.states.StateRecord;
import com.jaamsim.units.TimeUnit;

public class DowntimeEntity extends StateEntity implements StateEntityListener {

	public enum DowntimeTypes {
		IMMEDIATE,
		FORCED,
		OPPORTUNISTIC }

	@Keyword(description = "The simulation time for the first downtime event.  " +
	                       "The value may be a constant or a probability distribution.  " +
	                       "If a value is not specified, the Interval keyword is sampled " +
	                       "to determine the simulation time of the first downtime event.",
	         exampleList = {"720 h", "UniformDistribution1" })
	private final SampleInput firstDowntime;

	@Keyword(description = "The object for which to track working time accumulated in order to schedule a downtime event in objects that have this" +
	                       "DowntimeEntity defined in their 'DowntimeEntities' keyword.  If this keyword is not set, calendar time will be used to" +
	                       "determine the IAT.",
	         exampleList = { "Object1" })
	private final EntityInput<StateEntity> iatWorkingEntity;

	@Keyword(description = "The object for which to track working time accumulated in order to complete a downtime event in objects that have this" +
	                       "DowntimeEntity defined in their 'DowntimeEntities' keyword.  If this keyword is not set, calendar time will be used to" +
	                       "determine when the downtime will be over.",
	example = "DowntimeEntity1 DurationWorkingEntity { Object1 }")
	private final EntityInput<StateEntity> durationWorkingEntity;

	@Keyword(description = "A SampleProvider for the duration of breakdowns.",
			 exampleList = {"8 h ", "DurationTimeSeries", "DurationDistribution" })
	private final SampleInput downtimeDurationDistribution;

	@Keyword(description = "A SampleProvider for the time between breakdowns.",
			 exampleList = {"168 h", "IntervalTimeSeries", "IntervalDistribution" })
	private final SampleInput downtimeIATDistribution;

	@Keyword(description = "The severity level for the downtime events. The value must be one of the following:\n " +
             "- IMMEDIATE\n" +
             "- FORCED\n" +
             "- OPPORTUNISTIC",
             example = "DowntimeEntity1 Type { FORCED }")
	private final EnumInput<DowntimeTypes> type;

	@Keyword(description = "If TRUE, the downtime event will occur in parallel with a present downtime event.",
	         example = "DowntimeEntity1 Concurrent { FALSE }")
	protected final BooleanInput concurrent;

	private final ArrayList<DowntimeUser> modelEntityList;  // A list of model entities that have this downtime entity in its DowntimeEntities keyword
	private boolean down;             // true for the duration of a downtime event
	private int downtimePendings;    // number of queued downtime events

	private double secondsForNextFailure;    // The number of working seconds required before the next downtime event
	private double secondsForNextRepair;    // The number of working seconds required before the downtime event ends

	private double startTime;        // The start time of the latest downtime event
	private double endTime;          // the end time of the latest downtime event

	{
		firstDowntime = new SampleInput("FirstDowntime", "Key Inputs", null);
		firstDowntime.setUnitType(TimeUnit.class);
		this.addInput(firstDowntime);

		iatWorkingEntity = new EntityInput<>(StateEntity.class, "IntervalWorkingEntity", "Key Inputs", null);
		this.addInput(iatWorkingEntity);
		this.addSynonym(iatWorkingEntity, "IATWorkingEntity");

		durationWorkingEntity = new EntityInput<>(StateEntity.class, "DurationWorkingEntity", "Key Inputs", null);
		this.addInput(durationWorkingEntity);

		downtimeIATDistribution = new SampleInput("Interval", "Key Inputs", null);
		downtimeIATDistribution.setUnitType( TimeUnit.class );
		this.addInput(downtimeIATDistribution);
		this.addSynonym(downtimeIATDistribution, "IAT");
		this.addSynonym(downtimeIATDistribution, "TimeBetweenFailures");

		downtimeDurationDistribution = new SampleInput("Duration", "Key Inputs", null);
		downtimeDurationDistribution.setUnitType(TimeUnit.class);
		this.addInput(downtimeDurationDistribution);
		this.addSynonym(downtimeDurationDistribution, "Duration");

		type = new EnumInput<> (DowntimeTypes.class, "Type","Key Inputs",null);
		type.setRequired(true);
		this.addInput(type);

		concurrent = new BooleanInput("Concurrent", "Key Inputs", false);
		concurrent.setHidden(true);
		this.addInput(concurrent);

		this.addSynonym(downtimeDurationDistribution, "TimeToRepair");
	}

	public DowntimeEntity(){
		modelEntityList = new ArrayList<>();
	}

	@Override
	public void validate()
	throws InputErrorException {
		super.validate();
		if( downtimeIATDistribution.getValue() == null && downtimeDurationDistribution.getValue() != null )
			throw new InputErrorException("When DowntimeDurationDistribution is set, DowntimeIATDistribution must also be set.");

		if( downtimeIATDistribution.getValue() != null && downtimeDurationDistribution.getValue() == null )
			throw new InputErrorException("When DowntimeIATDistribution is set, DowntimeDurationDistribution must also be set.");
	}

	@Override
	public void earlyInit(){
		super.earlyInit();

		down = false;
		modelEntityList.clear();
		downtimePendings = 0;
		startTime = 0;
		endTime = 0;

		if (!this.isActive())
			return;

		for (StateEntity each : Entity.getClonesOfIterator(StateEntity.class, DowntimeUser.class)) {

			if( ! each.isActive() )
				continue;

			DowntimeUser du = (DowntimeUser)each;
			if( du.getMaintenanceEntities().contains(this) || du.getBreakdownEntities().contains(this) )
				modelEntityList.add(du);
		}
	}

	@Override
	public void lateInit() {
		super.lateInit();

		// Determine the time for the first downtime event
		if( firstDowntime.getValue() == null )
			secondsForNextFailure = getNextDowntimeIAT();
		else
			secondsForNextFailure = firstDowntime.getValue().getNextSample(getSimTime());
	}

	@Override
    public void startUp() {
		super.startUp();

		if (!this.isActive())
			return;

		checkProcessNetwork();
	}

	public DowntimeTypes getType() {
		return type.getValue();
	}

	/**
	 * Get the name of the initial state this Entity will be initialized with.
	 * @return
	 */
	@Override
	public String getInitialState() {
		return "Working";
	}

	/**
	 * Tests the given state name to see if it is valid for this Entity.
	 * @param state
	 * @return
	 */
	@Override
	public boolean isValidState(String state) {
		return "Working".equals(state) || "Downtime".equals(state);
	}

	/**
	 * Tests the given state name to see if it is counted as working hours when in
	 * that state..
	 * @param state
	 * @return
	 */
	@Override
	public boolean isValidWorkingState(String state) {
		return "Working".equals(state);
	}

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

	public void checkProcessNetwork() {

		// Schedule the next downtime event
		if( ! scheduleDowntimeHandle.isScheduled() ) {

			// 1) Calendar time
			if( iatWorkingEntity.getValue() == null ) {
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
			if( iatWorkingEntity.getValue() != null && ! iatWorkingEntity.getValue().isWorking() ) {
				EventManager.killEvent(scheduleDowntimeHandle);
			}
		}

		// 1) Determine when to end the current downtime event
		if (down) {

			if( durationWorkingEntity.getValue() == null ) {

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
			if( downtimePendings > 0 ) {

				// If all entities can start the downtime
				boolean allEntitiesCanStart = true;

				// If immediate option is selected, don't have to wait for entities to get ready
				for( DowntimeUser each : modelEntityList ) {
					if( ! each.canStartDowntime(this) ) {
						allEntitiesCanStart = false;
						break;
					}
				}

				if( allEntitiesCanStart ) {
					this.startDowntime();
				}
			}
		}
	}

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
		for (DowntimeUser each : modelEntityList) {
			EventManager.startProcess(new PrepareForDowntimeTarget(this, each));
		}

		this.checkProcessNetwork();
	}

	/**
	 * When enough working hours have been accumulated by WorkingEntity, trigger all entities in modelEntityList to perform downtime
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
		for (DowntimeUser each : modelEntityList) {
			each.startDowntime(this);
		}

		this.checkProcessNetwork();
	}

	private void setDown(boolean b) {
		down = b;
		if (down)
			setPresentState("Downtime");
		else
			setPresentState("Working");
	}

	private void endDowntime() {
		setDown(false);

		// Loop through all objects that this object is watching and try to restart them.
		for( DowntimeUser each : modelEntityList ) {
			each.endDowntime(this);
		}

		this.checkProcessNetwork();
	}

	/**
	 * Return the time in seconds of the next downtime IAT
	 */
	private double getNextDowntimeIAT() {
		if (downtimeIATDistribution.getValue() == null)
			return 10e10d;

		return downtimeIATDistribution.getValue().getNextSample(getSimTime());
	}

	/**
	 * Return the time in seconds of the next downtime duration
	 */
	private double getDowntimeDuration() {
		// If a distribution was specified, then select a duration randomly from the distribution
		if (downtimeDurationDistribution.getValue() == null)
			return 0.0d;

		return downtimeDurationDistribution.getValue().getNextSample(getSimTime());
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

		if( modelEntityList.contains(ent) )
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

	@Output(name = "StartTime",
			description = "The time that the current event started.",
			unitType = TimeUnit.class)
	public double getStartTime( double simTime ) {
		return startTime;
	}

	@Output(name = "EndTime",
			description = "The time that the current event will finish.",
			unitType = TimeUnit.class)
	public double getEndTime( double simTime ) {
		return endTime;
	}
}
