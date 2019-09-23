/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2015-2019 JaamSim Software Inc.
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

import com.jaamsim.StringProviders.StringProvListInput;
import com.jaamsim.StringProviders.StringProvider;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.IntegerListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.UnitTypeListInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.states.StateEntity;
import com.jaamsim.states.StateEntityListener;
import com.jaamsim.states.StateRecord;
import com.jaamsim.units.TimeUnit;

public class ExpressionLogger extends Logger implements StateEntityListener {

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

	private final ArrayList<String> lastValueList = new ArrayList<>();

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
		this.addInput(valueTraceList);

		valuePrecisionList = new IntegerListInput("ValuePrecisionList", KEY_INPUTS, null);
		valuePrecisionList.setHidden(true);
		this.addInput(valuePrecisionList);
	}

	public ExpressionLogger() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == valueTraceList) {
			lastValueList.clear();
			for (int i=0; i<valueTraceList.getListSize(); i++) {
				lastValueList.add("");
			}
			return;
		}
	}

	@Override
	public void startUp() {
		super.startUp();

		// Start tracing the expression values
		if (valueTraceList.getListSize() > 0) {
			for (int i=0; i<valueTraceList.getListSize(); i++) {
				String str = valueTraceList.getValue().get(i).getNextString(getSimTime());
				lastValueList.set(i, str);
			}
			this.doValueTrace();
		}

		// Start log entries at fixed intervals
		if (interval.getValue() != null)
			this.scheduleProcess(getStartTime(), 5, endActionTarget);
	}

	@Override
	protected void printColumnTitles(FileEntity file) {

		// Traced entities
		for (StateEntity ent : stateTraceList.getValue()) {
			file.format("\t%s", ent.getName());
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
		this.recordLogEntry(simTime);

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
	protected void recordEntry(FileEntity file, double simTime) {

		// Write the state values
		for (StateEntity ent : stateTraceList.getValue()) {
			file.format("\t%s", ent.getPresentState(simTime));
		}

		try {
			// Write the traced expression values
			for (int i=0; i<valueTraceList.getListSize(); i++) {
				String str = valueTraceList.getValue().get(i).getNextString(simTime);
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
		this.recordLogEntry(getSimTime());
	}

	/**
	 * Returns true if any of the traced expressions have changed their values.
	 */
	final boolean valueChanged() {
		boolean ret = false;
		double simTime = getSimTime();
		try {
			for (int i=0; i<valueTraceList.getListSize(); i++) {
				String str = valueTraceList.getValue().get(i).getNextString(simTime);
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
		this.recordLogEntry(simTime);

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
			doValueTrace();
		}
	}
	private final ProcessTarget doValueTrace = new DoValueTraceTarget();

}
