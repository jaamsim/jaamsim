/*
 * JaamSim Discrete Event Simulation
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
package com.jaamsim.ProcessFlow;

import java.util.ArrayList;

import com.jaamsim.BasicObjects.DowntimeEntity;
import com.jaamsim.Thresholds.Threshold;
import com.jaamsim.Thresholds.ThresholdUser;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Color4d;
import com.jaamsim.states.DowntimeUser;
import com.jaamsim.states.StateEntity;

public abstract class StateUserEntity extends StateEntity implements ThresholdUser, DowntimeUser {

	@Keyword(description = "A list of thresholds that must be satisfied for the object to "
	                     + "operate. Operation is stopped immediately when one of the thresholds "
	                     + "closes. If a threshold closes part way though processing an entity, "
	                     + "the work is considered to be partly done and the remainder is "
	                     + "completed once the threshold re-opens.",
	         exampleList = {"ExpressionThreshold1 TimeSeriesThreshold1 SignalThreshold1"})
	protected final EntityListInput<Threshold> immediateThresholdList;

	@Keyword(description = "A list of thresholds that must be satisfied for the object to "
	                     + "operate. Operation is stopped immediately when one of the thresholds "
	                     + "closes. If a threshold closes part way though processing an entity, "
	                     + "the work is interrupted and the entity is released.",
	         exampleList = {"ExpressionThreshold1 TimeSeriesThreshold1 SignalThreshold1"})
	protected final EntityListInput<Threshold> immediateReleaseThresholdList;

	@Keyword(description = "A list of thresholds that must be satisfied for the object to "
	                     + "operate. If a threshold closes part way though processing an entity, "
	                     + "the remaining work is completed and the entity is released before the "
	                     + "object is closed.",
	         exampleList = {"ExpressionThreshold1 TimeSeriesThreshold1 SignalThreshold1"})
	protected final EntityListInput<Threshold> operatingThresholdList;

	@Keyword(description = "A list of thresholds that must be satisfied for the object to "
	                     + "operate. If a threshold closes part way though processing an entity, "
	                     + "the remaining work is completed, but the entity cannot be released "
	                     + "until the threshold re-opens.",
	         exampleList = {"ExpressionThreshold1 TimeSeriesThreshold1 SignalThreshold1"})
	protected final EntityListInput<Threshold> releaseThresholdList;

	@Keyword(description = "A list of DowntimeEntities representing planned maintenance that "
	                     + "must be performed immediately, interrupting any work underway at "
	                     + "present.",
	         exampleList = {"DowntimeEntity1 DowntimeEntity2 DowntimeEntity3"})
	protected final EntityListInput<DowntimeEntity> immediateMaintenanceList;

	@Keyword(description = "A list of DowntimeEntities representing planned maintenance that "
	                     + "must begin as soon as task underway at present is finished.",
	         exampleList = {"DowntimeEntity1 DowntimeEntity2 DowntimeEntity3"})
	protected final EntityListInput<DowntimeEntity> forcedMaintenanceList;

	@Keyword(description = "A list of DowntimeEntities representing planned maintenance that "
	                     + "can wait until task underway at present is finished and the queue "
	                     + "of tasks is empty.",
	         exampleList = {"DowntimeEntity1 DowntimeEntity2 DowntimeEntity3"})
	protected final EntityListInput<DowntimeEntity> opportunisticMaintenanceList;

	@Keyword(description = "A list of DowntimeEntities representing unplanned maintenance that "
	                     + "must be performed immediately, interrupting any work underway at "
	                     + "present.",
	         exampleList = {"DowntimeEntity1 DowntimeEntity2 DowntimeEntity3"})
	protected final EntityListInput<DowntimeEntity> immediateBreakdownList;

	@Keyword(description = "A list of DowntimeEntities representing unplanned maintenance that "
	                     + "must begin as soon as task underway at present is finished.",
	         exampleList = {"DowntimeEntity1 DowntimeEntity2 DowntimeEntity3"})
	protected final EntityListInput<DowntimeEntity> forcedBreakdownList;

	@Keyword(description = "A list of DowntimeEntities representing unplanned maintenance that "
	                     + "can wait until task underway at present is finished and the queue "
	                     + "of tasks is empty.",
	         exampleList = {"DowntimeEntity1 DowntimeEntity2 DowntimeEntity3"})
	protected final EntityListInput<DowntimeEntity> opportunisticBreakdownList;

	protected static final String STATE_MAINTENANCE = "Maintenance";
	protected static final String STATE_BREAKDOWN = "Breakdown";
	protected static final String STATE_STOPPED = "Stopped";
	protected static final String STATE_BLOCKED = "Blocked";

	protected static final Color4d COL_MAINTENANCE = ColourInput.RED;
	protected static final Color4d COL_BREAKDOWN = ColourInput.RED;
	protected static final Color4d COL_STOPPED = ColourInput.getColorWithName("gray25");
	protected static final Color4d COL_BLOCKED = ColourInput.getColorWithName("gray25");

	{
		immediateThresholdList = new EntityListInput<>(Threshold.class, "ImmediateThresholdList", THRESHOLDS, new ArrayList<Threshold>());
		this.addInput(immediateThresholdList);

		immediateReleaseThresholdList = new EntityListInput<>(Threshold.class, "ImmediateReleaseThresholdList", THRESHOLDS, new ArrayList<Threshold>());
		this.addInput(immediateReleaseThresholdList);

		operatingThresholdList = new EntityListInput<>(Threshold.class, "OperatingThresholdList", THRESHOLDS, new ArrayList<Threshold>());
		this.addInput(operatingThresholdList);

		releaseThresholdList = new EntityListInput<>(Threshold.class, "ReleaseThresholdList", THRESHOLDS, new ArrayList<Threshold>());
		releaseThresholdList.setHidden(true);
		this.addInput(releaseThresholdList);

		immediateMaintenanceList =  new EntityListInput<>(DowntimeEntity.class,
				"ImmediateMaintenanceList", MAINTENANCE, new ArrayList<DowntimeEntity>());
		this.addInput(immediateMaintenanceList);

		forcedMaintenanceList =  new EntityListInput<>(DowntimeEntity.class,
				"ForcedMaintenanceList", MAINTENANCE, new ArrayList<DowntimeEntity>());
		this.addInput(forcedMaintenanceList);

		opportunisticMaintenanceList =  new EntityListInput<>(DowntimeEntity.class,
				"OpportunisticMaintenanceList", MAINTENANCE, new ArrayList<DowntimeEntity>());
		this.addInput(opportunisticMaintenanceList);

		immediateBreakdownList =  new EntityListInput<>(DowntimeEntity.class,
				"ImmediateBreakdownList", MAINTENANCE, new ArrayList<DowntimeEntity>());
		this.addInput(immediateBreakdownList);

		forcedBreakdownList =  new EntityListInput<>(DowntimeEntity.class,
				"ForcedBreakdownList", MAINTENANCE, new ArrayList<DowntimeEntity>());
		this.addInput(forcedBreakdownList);

		opportunisticBreakdownList =  new EntityListInput<>(DowntimeEntity.class,
				"OpportunisticBreakdownList", MAINTENANCE, new ArrayList<DowntimeEntity>());
		this.addInput(opportunisticBreakdownList);
	}

	public StateUserEntity() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		initStates();
	}

	@Override
	public void startUp() {
		super.startUp();
		setPresentState();
	}

	public void initStates() {
		this.addState(STATE_IDLE);
		this.addState(STATE_WORKING);
		this.addState(STATE_MAINTENANCE);
		this.addState(STATE_BREAKDOWN);
		this.addState(STATE_STOPPED);
	}

	@Override
	public boolean isValidState(String state) {
		return true;
	}

	// ********************************************************************************************
	// THRESHOLDS
	// ********************************************************************************************

	@Override
	public ArrayList<Threshold> getThresholds() {
		ArrayList<Threshold> ret = new ArrayList<>(operatingThresholdList.getValue());
		ret.addAll(releaseThresholdList.getValue());
		ret.addAll(immediateThresholdList.getValue());
		ret.addAll(immediateReleaseThresholdList.getValue());
		return ret;
	}

	public boolean isImmediateThresholdClosure() {
		for (Threshold thresh : immediateThresholdList.getValue()) {
			if (!thresh.isOpen())
				return true;
		}
		return false;
	}

	public boolean isImmediateReleaseThresholdClosure() {
		for (Threshold thresh : immediateReleaseThresholdList.getValue()) {
			if (!thresh.isOpen())
				return true;
		}
		return false;
	}

	public boolean isOperatingThresholdClosure() {
		for (Threshold thr : operatingThresholdList.getValue()) {
			if (!thr.isOpen())
				return true;
		}
		return false;
	}

	public boolean isReleaseThresholdClosure() {
		for (Threshold thr : releaseThresholdList.getValue()) {
			if (!thr.isOpen())
				return true;
		}
		return false;
	}

	// ********************************************************************************************
	// PRESENT STATE
	// ********************************************************************************************

	public abstract boolean isBusy();

	/**
	 * Tests whether all the thresholds are open.
	 * @return true if all the thresholds are open.
	 */
	public boolean isOpen() {
		return !isImmediateThresholdClosure() && !isImmediateReleaseThresholdClosure()
				&& !isOperatingThresholdClosure() && !isReleaseThresholdClosure();
	}

	public boolean isMaintenance() {
		for (DowntimeEntity de : immediateMaintenanceList.getValue()) {
			if (de.isDown())
				return true;
		}
		for (DowntimeEntity de : forcedMaintenanceList.getValue()) {
			if (de.isDown())
				return true;
		}
		for (DowntimeEntity de : opportunisticMaintenanceList.getValue()) {
			if (de.isDown())
				return true;
		}
		return false;
	}

	public boolean isImmediateMaintenance() {
		for (DowntimeEntity de : immediateMaintenanceList.getValue()) {
			if (de.isDown())
				return true;
		}
		return false;
	}

	public boolean isBreakdown() {
		for (DowntimeEntity de : immediateBreakdownList.getValue()) {
			if (de.isDown())
				return true;
		}
		for (DowntimeEntity de : forcedBreakdownList.getValue()) {
			if (de.isDown())
				return true;
		}
		for (DowntimeEntity de : opportunisticBreakdownList.getValue()) {
			if (de.isDown())
				return true;
		}
		return false;
	}

	public boolean isImmediateBreakdown() {
		for (DowntimeEntity de : immediateBreakdownList.getValue()) {
			if (de.isDown())
				return true;
		}
		return false;
	}

	public boolean isAvailable() {
		return isOpen() && !isMaintenance() && !isBreakdown() && isActive();
	}

	/**
	 * Tests whether the entity is available for work.
	 * <p>
	 * There are three mutually exclusive states: Busy, Idle, and UnableToWork. The UnableToWork
	 * condition is divided into three separate states: Maintenance, Breakdown, and Stopped.
	 * @return true if the LinkedService is available for work
	 */
	public boolean isIdle() {
		return !isBusy() && isAvailable();
	}

	/**
	 * Tests whether something is preventing work from being performed.
	 * <p>
	 * There are three mutually exclusive states: Busy, Idle, and UnableToWork. The UnableToWork
	 * condition is divided into three separate states: Maintenance, Breakdown, and Stopped.
	 * @return true if something is preventing work from being performed
	 */
	public boolean isUnableToWork() {
		return !isBusy() && !isAvailable();
	}

	public void setPresentState() {

		// Inactive
		if (!this.isActive()) {
			this.setPresentState(STATE_INACTIVE);
			return;
		}

		// Working (Busy)
		if (this.isBusy()) {
			this.setPresentState(STATE_WORKING);
			return;
		}

		// Not working because of maintenance or a closure (UnableToWork)
		if (this.isMaintenance()) {
			this.setPresentState(STATE_MAINTENANCE);
			return;
		}
		if (this.isBreakdown()) {
			this.setPresentState(STATE_BREAKDOWN);
			return;
		}
		if (!this.isOpen()) {
			this.setPresentState(STATE_STOPPED);
			return;
		}

		// Not working because there is nothing to do (Idle)
		this.setPresentState(STATE_IDLE);
		return;
	}

	protected Color4d getColourForPresentState() {

		// Inactive
		if (!this.isActive()) {
			return COL_INACTIVE;
		}

		// Working (Busy)
		if (this.isBusy()) {
			return COL_WORKING;
		}

		// Not working because of maintenance or a closure (UnableToWork)
		if (this.isMaintenance()) {
			return COL_MAINTENANCE;
		}
		if (this.isBreakdown()) {
			return COL_BREAKDOWN;
		}
		if (!this.isOpen()) {
			return COL_STOPPED;
		}

		// Not working because there is nothing to do (Idle)
		return COL_IDLE;
	}

	// ********************************************************************************************
	// MAINTENANCE AND BREAKDOWNS
	// ********************************************************************************************

	@Override
	public boolean isDowntimeUser(DowntimeEntity down) {
		return immediateMaintenanceList.getValue().contains(down)
				|| immediateBreakdownList.getValue().contains(down)
		        || forcedMaintenanceList.getValue().contains(down)
				|| forcedBreakdownList.getValue().contains(down)
		        || opportunisticMaintenanceList.getValue().contains(down)
				|| opportunisticBreakdownList.getValue().contains(down);
	}

	public boolean isImmediateDowntime(DowntimeEntity down) {
		return immediateMaintenanceList.getValue().contains(down)
				|| immediateBreakdownList.getValue().contains(down);
	}

	public boolean isForcedDowntime(DowntimeEntity down) {
		return forcedMaintenanceList.getValue().contains(down)
				|| forcedBreakdownList.getValue().contains(down);
	}

	public boolean isOpportunisticDowntime(DowntimeEntity down) {
		return opportunisticMaintenanceList.getValue().contains(down)
				|| opportunisticBreakdownList.getValue().contains(down);
	}

	public double getTimeInState_Idle(double simTime) {
		return getTimeInState(simTime, STATE_IDLE);
	}

	public double getTimeInState_Working(double simTime) {
		return getTimeInState(simTime, STATE_WORKING);
	}

	public double getTimeInState_Maintenance(double simTime) {
		return getTimeInState(simTime, STATE_MAINTENANCE);
	}

	public double getTimeInState_Breakdown(double simTime) {
		return getTimeInState(simTime, STATE_BREAKDOWN);
	}

	public double getTimeInState_Stopped(double simTime) {
		return getTimeInState(simTime, STATE_STOPPED);
	}

	// ********************************************************************************************
	// OUTPUTS
	// ********************************************************************************************

	@Output(name = "Open",
	 description = "Returns TRUE if all the thresholds specified by the OperatingThresholdList, "
	             + "ImmediateThresholdList, and ImmediateReleaseThresholdList keywords are open.",
	    sequence = 1)
	public boolean getOpen(double simTime) {
		return isOpen();
	}

	@Output(name = "Working",
	 description = "Returns TRUE if work is being performed.",
	    sequence = 2)
	public boolean isBusy(double simTime) {
		return isBusy();
	}

	@Output(name = "Maintenance",
	 description = "Returns TRUE if maintenance is being performed.",
	    sequence = 3)
	public boolean isMaintenance(double simTime) {
		return isMaintenance();
	}

	@Output(name = "Breakdown",
	 description = "Returns TRUE if a breakdown is being repaired.",
	    sequence = 4)
	public boolean isBreakdown(double simTime) {
		return isBreakdown();
	}

	@Output(name = "Utilisation",
	 description = "The fraction of calendar time (excluding the initialisation period) that "
	             + "this object is in the Working state. Includes any completed cycles.",
	  reportable = true,
	    sequence = 5)
	public double getUtilisation(double simTime) {
		double total = this.getTotalTime(simTime);
		double working = getTimeInState_Working(simTime);
		return working/total;
	}

	@Output(name = "Commitment",
	 description = "The fraction of calendar time (excluding the initialisation period) that "
	             + "this object is in any state other than Idle. Includes any completed cycles.",
	  reportable = true,
	    sequence = 6)
	public double getCommitment(double simTime) {
		double total = this.getTotalTime(simTime);
		double idle = getTimeInState_Idle(simTime);
		return 1.0d - idle/total;
	}

	@Output(name = "Availability",
	 description = "The fraction of calendar time (excluding the initialisation period) that "
	             + "this object is in any state other than Maintenance or Breakdown. "
	             + "Includes any completed cycles.",
	  reportable = true,
	    sequence = 7)
	public double getAvailability(double simTime) {
		double total = this.getTotalTime(simTime);
		double maintenance = getTimeInState_Maintenance(simTime);
		double breakdown = getTimeInState_Breakdown(simTime);
		return 1.0d - (maintenance + breakdown)/total;
	}

	@Output(name = "Reliability",
	 description = "The ratio of Working time to the sum of Working time and Breakdown time. "
	             + "All times exclude the initialisation period and include any completed cycles.",
	  reportable = true,
	    sequence = 8)
	public double getReliability(double simTime) {
		double working = getTimeInState_Working(simTime);
		double breakdown = getTimeInState_Breakdown(simTime);
		return working / (working + breakdown);
	}

	@Output(name = "NextMaintenanceTime",
	 description = "The estimated time at which the next maintenance activity will start.",
	  reportable = false,
	    sequence = 9)
	public double getNextMaintenanceTime(double simTime) {
		double ret = Double.POSITIVE_INFINITY;
		for (DowntimeEntity down : immediateMaintenanceList.getValue()) {
			ret = Math.min(ret, down.getNextStartTime(simTime));
		}
		for (DowntimeEntity down : forcedMaintenanceList.getValue()) {
			ret = Math.min(ret, down.getNextStartTime(simTime));
		}
		for (DowntimeEntity down : opportunisticMaintenanceList.getValue()) {
			ret = Math.min(ret, down.getNextStartTime(simTime));
		}
		return ret;
	}

	@Output(name = "NextBreakdownTime",
	 description = "The estimated time at which the next breakdown will occur.",
	  reportable = false,
	    sequence = 10)
	public double getNextBreakdownTime(double simTime) {
		double ret = Double.POSITIVE_INFINITY;
		for (DowntimeEntity down : immediateBreakdownList.getValue()) {
			ret = Math.min(ret, down.getNextStartTime(simTime));
		}
		for (DowntimeEntity down : forcedBreakdownList.getValue()) {
			ret = Math.min(ret, down.getNextStartTime(simTime));
		}
		for (DowntimeEntity down : opportunisticBreakdownList.getValue()) {
			ret = Math.min(ret, down.getNextStartTime(simTime));
		}
		return ret;
	}

}
