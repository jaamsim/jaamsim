/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2015-2022 JaamSim Software Inc.
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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.StringProviders.StringProvListInput;
import com.jaamsim.StringProviders.StringProvider;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.basicsim.ObserverEntity;
import com.jaamsim.basicsim.SubjectEntity;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.IntegerListInput;
import com.jaamsim.input.InterfaceEntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeListInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.states.StateEntity;
import com.jaamsim.states.StateEntityListener;
import com.jaamsim.states.StateRecord;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

public class ExpressionLogger extends Logger implements StateEntityListener, ObserverEntity {

	@Keyword(description = "A fixed interval at which entries will be written to the log file. "
	                     + "This input is optional if state tracing or value tracing is "
	                     + "specified.",
	         exampleList = { "24.0 h" })
	private final ValueInput interval;

	@Keyword(description = "A list of entities whose states will be traced. "
	                     + "An entry in the log file is made every time one of the entities "
	                     + "changes state. "
	                     + "Each entity's state is included automatically in the log file.",
	         exampleList = { "Server1 ExpressionThreshold1" })
	private final EntityListInput<StateEntity> stateTraceList;

	@Keyword(description = "Not Used")
	private final UnitTypeListInput valueUnitTypeList;

	@Keyword(description = "One or more sources of data whose values will be traced. "
	                     + "An entry in the log file is made every time one of the data sources "
	                     + "changes value. "
	                     + "Each data source's value is included automatically in the log file.\n\n"
	                     + "It is best to include only dimensionless quantities and non-numeric "
	                     + "outputs in the ValueTraceList input. "
	                     + "An output with dimensions can be made non-dimensional by dividing it "
	                     + "by 1 in the desired unit, e.g. '[Queue1].AverageQueueTime / 1[h]' is "
	                     + "the average queue time in hours."
	                     + "A dimensional number will be displayed along with its unit. "
	                     + "The 'format' function can be used if a fixed number of decimal places "
	                     + "is required.",
	         exampleList = {"{ [Queue1].QueueLengthAverage }"
	                     + " { '[Queue1].AverageQueueTime / 1[h]' }"})
	private final StringProvListInput valueTraceList;

	@Keyword(description = "Not Used")
	private final IntegerListInput valuePrecisionList;

	@Keyword(description = "An optional list of objects to monitor.\n\n"
	                     + "If the WatchList input is provided, the ExpressionLogger evaluates "
	                     + "its ValueTraceList expression inputs ONLY when triggered by an object "
	                     + "in its WatchList. "
	                     + "This is much more efficient than the default behaviour which "
	                     + "evaluates these expressions at every event time.\n\n"
	                     + "Care must be taken to ensure that the WatchList input includes every "
	                     + "object that can trigger a change in a ValueTraceList expression. "
	                     + "Normally, the WatchList should include every object that is referenced "
	                     + "directly or indirectly by these expressions. "
	                     + "The VerfiyWatchList input can be used to ensure that the WatchList "
	                     + "includes all the necessary objects.",
	         exampleList = {"Object1  Object2"})
	protected final InterfaceEntityListInput<SubjectEntity> watchList;

	@Keyword(description = "Allows the user to verify that the input to the 'WatchList' keyword "
	                     + "includes all the objects that can trigger a change in a "
	                     + "ValueTraceList expression. "
	                     + "When set to TRUE, the ExpressionThreshold uses both the normal logic "
	                     + "and the WatchList logic to test the ValueTraceList expressions. "
	                     + "An error message is generated if a ValueTraceList expression changes "
	                     + "its value without being triggered by a WatchList object.",
	         exampleList = { "TRUE" })
	private final BooleanInput verifyWatchList;

	@Keyword(description = "A logical condition that determines whether to record a log entry "
	                     + "that is triggered by a change to one of the objects in the "
	                     + "'WatchList'. "
	                     + "An entry in the 'ValueTraceList' is not required to trigger this type "
	                     + "of log entry. "
	                     + "The output 'WatchedEntity' can be used in the expression to represent "
	                     + "the 'WatchList' entity that changed.",
	         exampleList = { "'[Queue1].QueueLength > 3'" })
	private final ExpressionInput watchListCondition;

	private final ArrayList<String> lastValueList = new ArrayList<>();
	private Entity watchedEntity;  // last subject entity that triggered a log entry

	{
		interval = new ValueInput("Interval", KEY_INPUTS, null);
		interval.setUnitType(TimeUnit.class);
		interval.setValidRange(1.0e-10, Double.POSITIVE_INFINITY);
		this.addInput(interval);

		stateTraceList = new EntityListInput<>(StateEntity.class, "StateTraceList", KEY_INPUTS,
				new ArrayList<StateEntity>());
		this.addInput(stateTraceList);

		valueUnitTypeList = new UnitTypeListInput("ValueUnitTypeList", KEY_INPUTS,	null);
		valueUnitTypeList.setHidden(true);
		this.addInput(valueUnitTypeList);

		valueTraceList = new StringProvListInput("ValueTraceList", KEY_INPUTS,
				new ArrayList<StringProvider>());
		valueTraceList.setCallback(unitLastValueListCallback);
		this.addInput(valueTraceList);

		valuePrecisionList = new IntegerListInput("ValuePrecisionList", KEY_INPUTS, null);
		valuePrecisionList.setHidden(true);
		this.addInput(valuePrecisionList);

		watchList = new InterfaceEntityListInput<>(SubjectEntity.class, "WatchList", KEY_INPUTS, new ArrayList<>());
		watchList.setIncludeSelf(false);
		watchList.setUnique(true);
		this.addInput(watchList);

		verifyWatchList = new BooleanInput("VerifyWatchList", KEY_INPUTS, false);
		this.addInput(verifyWatchList);

		watchListCondition = new ExpressionInput("WatchListCondition", KEY_INPUTS, null);
		watchListCondition.setUnitType(DimensionlessUnit.class);
		watchListCondition.setResultType(ExpResType.NUMBER);
		watchListCondition.setDefaultText(BooleanInput.FALSE);
		this.addInput(watchListCondition);
	}

	public ExpressionLogger() {}

	static final InputCallback unitLastValueListCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((ExpressionLogger)ent).updateLastValueList();
		}
	};

	void updateLastValueList() {
		lastValueList.clear();
		for (int i=0; i<valueTraceList.getListSize(); i++) {
			lastValueList.add("");
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		valueUnitTypeList.reset();
		valuePrecisionList.reset();
	}

	@Override
	public void lateInit() {
		super.lateInit();
		ObserverEntity.registerWithSubjects(this, getWatchList());
	}

	@Override
	public void startUp() {
		super.startUp();

		// Start tracing the expression values
		if (valueTraceList.getListSize() > 0) {
			for (int i=0; i<valueTraceList.getListSize(); i++) {
				String str = valueTraceList.getNextString(i, this, getSimTime());
				lastValueList.set(i, str);
			}

			// If there is no WatchList, the open/close expressions are tested after every event
			if (!isWatchList() || isVerifyWatchList())
				doValueTrace();
		}

		// Start log entries at fixed intervals
		if (interval.getValue() != null)
			this.scheduleProcess(getStartTime(), 5, endActionTarget);
	}

	@Override
	public ArrayList<SubjectEntity> getWatchList() {
		return watchList.getValue();
	}

	public boolean isVerifyWatchList() {
		return verifyWatchList.getValue();
	}

	public boolean isWatchList() {
		return !getWatchList().isEmpty();
	}

	@Override
	public void observerUpdate(SubjectEntity subj) {
		if (recordLogEntryHandle.isScheduled())
			return;
		DisplayEntity ent = (DisplayEntity) subj;
		EventManager.scheduleTicks(0L, 11, true, new RecordLogEntryTarget(ent), recordLogEntryHandle);
	}

	private final EventHandle recordLogEntryHandle = new EventHandle();

	private class RecordLogEntryTarget extends ProcessTarget {
		DisplayEntity ent;

		public RecordLogEntryTarget(DisplayEntity ent) {
			this.ent = ent;
		}

		@Override
		public void process() {
			if (valueChanged() || testWatchListCondition(ent)) {
				watchedEntity = ent;
				recordLogEntry(getSimTime(), ent);
			}
		}

		@Override
		public String getDescription() {
			return "recordLogEntry";
		}
	}

	private boolean testWatchListCondition(Entity ent) {
		if (watchListCondition.isDefault())
			return false;

		// Temporarily set the watched entity so that the expression can be evaluated
		Entity lastEnt = watchedEntity;
		watchedEntity = ent;

		// Evaluate the open condition (0 = false, non-zero = true)
		boolean ret = watchListCondition.getNextResult(this, getSimTime()).value != 0;

		// Reset the original watched entity
		watchedEntity = lastEnt;

		return ret;
	}

	@Override
	protected void printColumnTitles(FileEntity file) {

		// Traced entities
		for (StateEntity ent : stateTraceList.getValue()) {
			file.format("\t[%s].State", ent.getName());
		}

		// Traced values
		ArrayList<String> valToks = new ArrayList<>();
		valueTraceList.getValueTokens(valToks);
		for (String str : valToks) {
			if (str.equals("{") || str.equals("}"))
				continue;
			file.format("\t%s", str);
		}
	}

	private void startAction() {

		// Schedule the next time an entry in the log file will be written
		this.scheduleProcess(interval.getValue(), 5, endActionTarget);
	}

	final void endAction() {

		// Stop the log if the end time has been reached
		double simTime = getSimTime();
		if (simTime > getEndTime())
			return;

		// Record the entry in the log
		this.recordLogEntry(simTime, null);

		// Get ready for the next entry
		this.startAction();
	}

	protected final ProcessTarget endActionTarget = new EndActionTarget(this);

	private static class EndActionTarget extends EntityTarget<ExpressionLogger> {
		EndActionTarget(ExpressionLogger ent) {
			super(ent, "endAction");
		}

		@Override
		public void process() {
			ent.endAction();
		}
	}

	@Override
	protected void recordEntry(FileEntity file, double simTime, DisplayEntity dEnt) {

		// Write the state values
		for (StateEntity ent : stateTraceList.getValue()) {
			file.format("\t%s", ent.getPresentState(simTime));
		}

		try {
			// Write the traced expression values
			for (int i=0; i<valueTraceList.getListSize(); i++) {
				String str = valueTraceList.getNextString(i, this, simTime);
				file.format("\t%s", str);
			}
		}
		catch (Exception e) {
			error(e.getMessage());
		}
	}

	@Override
	public boolean isWatching(StateEntity ent) {
		return stateTraceList.getValue().contains(ent);
	}

	@Override
	public void updateForStateChange(StateEntity ent, StateRecord prev, StateRecord next) {
		this.recordLogEntry(getSimTime(), ent);
	}

	/**
	 * Returns true if any of the traced expressions have changed their values.
	 */
	final boolean valueChanged() {
		boolean ret = false;
		double simTime = getSimTime();
		try {
			for (int i=0; i<valueTraceList.getListSize(); i++) {
				String str = valueTraceList.getNextString(i, this, simTime);
				if (!str.equals(lastValueList.get(i))) {
					lastValueList.set(i, str);
					ret = true;
				}
			}
		}
		catch (Exception e) {
			error(e.getMessage());
		}
		return ret;
	}

	/**
	 * Writes a record to the log file whenever one of the traced expressions changes its value.
	 */
	void doValueTrace() {

		// Stop tracing if the end time has been reached
		double simTime = getSimTime();
		if (simTime > getEndTime())
			return;

		// Record the entry in the log
		this.recordLogEntry(simTime, null);

		// Wait for the next value change
		EventManager.scheduleUntil(doValueTrace, valueChanged, null);
	}

	static class ValueChangedConditional extends Conditional {
		private final ExpressionLogger ent;

		ValueChangedConditional(ExpressionLogger ent) {
			this.ent = ent;
		}
		@Override
		public boolean evaluate() {
			return ent.valueChanged();
		}
	}
	private final Conditional valueChanged = new ValueChangedConditional(this);

	class DoValueTraceTarget extends ProcessTarget {
		@Override
		public String getDescription() {
			return ExpressionLogger.this.getName() + ".doValueTrace";
		}

		@Override
		public void process() {
			if (isVerifyWatchList())
				error(ERR_WATCHLIST);
			doValueTrace();
		}
	}
	private final ProcessTarget doValueTrace = new DoValueTraceTarget();

	@Output(name = "WatchedEntity",
	 description = "The entity in the WatchList input that triggered the most recent log entry.",
	    sequence = 1)
	public Entity getWatchedEntity(double simTime) {
		return watchedEntity;
	}

}
