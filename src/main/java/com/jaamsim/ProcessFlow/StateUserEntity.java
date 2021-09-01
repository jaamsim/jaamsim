/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016-2021 JaamSim Software Inc.
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
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.states.DowntimeUser;
import com.jaamsim.units.TimeUnit;

public abstract class StateUserEntity extends AbstractStateUserEntity implements ThresholdUser, DowntimeUser {

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
	                     + "the remaining work is completed, but the entity is retained "
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

	@Override
	public boolean isSetup() {
		return false;
	}

	@Override
	public boolean isSetdown() {
		return false;
	}

	/**
	 * Tests whether all the thresholds are open.
	 * @return true if all the thresholds are open.
	 */
	public boolean isOpen() {
		return !isStopped();
	}

	/**
	 * Returns whether all work in progress has been completed.
	 * @return true if there is no work in progress
	 */
	public boolean isFinished() {
		return true;
	}

	/**
	 * Returns whether work must stop if a ReleaseThreshold closes.
	 * @return true if work stops for a ReleaseThreshold closure
	 */
	public boolean isReadyToRelease() {
		return true;
	}

	@Override
	public boolean isStopped() {
		return isImmediateThresholdClosure() || isImmediateReleaseThresholdClosure()
				|| (isOperatingThresholdClosure() && isFinished())
				|| (isReleaseThresholdClosure() && isReadyToRelease());
	}

	@Override
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

	@Override
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

	public boolean isForcedDowntimePending() {
		for (DowntimeEntity de : forcedMaintenanceList.getValue()) {
			if (de.isDowntimePending())
				return true;
		}
		for (DowntimeEntity de : forcedBreakdownList.getValue()) {
			if (de.isDowntimePending())
				return true;
		}
		return false;
	}

	public boolean isImmediateDowntimePending() {
		for (DowntimeEntity de : immediateMaintenanceList.getValue()) {
			if (de.isDowntimePending())
				return true;
		}
		for (DowntimeEntity de : immediateBreakdownList.getValue()) {
			if (de.isDowntimePending())
				return true;
		}
		return false;
	}

	/**
	 * Returns whether the caller can be started.
	 * @return true if the caller can be started
	 */
	public boolean isAbleToRestart() {
		return isAvailable() && !isForcedDowntimePending() && !isImmediateDowntimePending();
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

	@Override
	public boolean canStartDowntime(DowntimeEntity down) {

		// Downtime can start when any work in progress has been interrupted and there are no
		// other maintenance or breakdown activities that are being performed. It is okay to start
		// downtime when one or more thresholds are closed.
		return !isBusy() && (down.isConcurrent() || !isMaintenance() && !isBreakdown());
	}

	@Override
	public void prepareForDowntime(DowntimeEntity down) {
		if (isTraceFlag()) trace(0, "prepareForDowntime(%s) - type=%s, busy=%s",
				down, getDowntimeType(down), isBusy());
	}

	@Override
	public void startDowntime(DowntimeEntity down) {
		if (isTraceFlag()) trace(0, "startDowntime(%s)", down);
		setPresentState();
	}

	@Override
	public void endDowntime(DowntimeEntity down) {
		if (isTraceFlag()) trace(0, "endDowntime(%s)", down);
		setPresentState();
	}

	public String getDowntimeType(DowntimeEntity down) {
		if (isImmediateDowntime(down))
			return "Immediate";
		if (isForcedDowntime(down))
			return "Forced";
		if (isOpportunisticDowntime(down))
			return "Opportunistic";
		return "Unknown";
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

	@Output(name = "NextMaintenanceTime",
	 description = "The estimated time at which the next maintenance activity will start.",
	    unitType = TimeUnit.class,
	  reportable = false,
	    sequence = 2)
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
	    unitType = TimeUnit.class,
	  reportable = false,
	    sequence = 3)
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
