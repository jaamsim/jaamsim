/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2015 JaamSim Software Inc.
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

import com.jaamsim.Samples.SampleListInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.IntegerListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.UnitTypeListInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.states.StateEntity;
import com.jaamsim.states.StateEntityListener;
import com.jaamsim.states.StateRecord;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class ExpressionLogger extends Logger implements StateEntityListener {

	@Keyword(description = "A fixed interval at which entries will be written to the log file. "
			+ "This input is optional if state tracing or value tracing is specified.",
	         exampleList = { "24.0 h" })
	private final ValueInput interval;

	@Keyword(description = "A list of entities whose states will be traced. "
			+ "An entry in the log file is made every time one of the entities changes state. "
			+ "Each entity's state is written automatically to the log file - it is not necessary "
			+ "to add an expression to the DataSource keyword's input.",
	         exampleList = { "Server1 ExpressionThreshold1" })
	private final EntityListInput<StateEntity> stateTraceList;

	@Keyword(description = "The unit types for the values being traced.",
	         exampleList = {"DistanceUnit  SpeedUnit"})
	private final UnitTypeListInput valueUnitTypeList;

	@Keyword(description = "One or more sources of data whose values will be traced. An entry in "
			+ "the log file is made every time one of the data sources changes value. "
			+ "Each data source's value is written automatically to the log file - it is not "
			+ "necessary to add an expression to the DataSource keyword's input.\n\n"
			+ "Each source is specified by an Expression. Also acceptable are: "
			+ "a constant value, a Probability Distribution, TimeSeries, or a Calculation Object.",
	         exampleList = {"{ [Entity1].Output1 } { [Entity2].Output2 }"})
	private final SampleListInput valueTraceList;

	@Keyword(description = "The number of decimal places to show for each value in valueTraceList."
			+ "  If only one number is given, then that number of decimal places is used for all values.",
	         exampleList = "1 1")
	private final IntegerListInput valuePrecisionList;

	private final ArrayList<Double> lastValueList = new ArrayList<>();

	{
		interval = new ValueInput("Interval", "Key Inputs", null);
		interval.setUnitType(TimeUnit.class);
		interval.setValidRange(1.0e-10, Double.POSITIVE_INFINITY);
		this.addInput(interval);

		stateTraceList = new EntityListInput<>(StateEntity.class, "StateTraceList", "Tracing",
				new ArrayList<StateEntity>());
		this.addInput(stateTraceList);

		ArrayList<Class<? extends Unit>> valDefList = new ArrayList<>();
		valueUnitTypeList = new UnitTypeListInput("ValueUnitTypeList", "Tracing",
				valDefList);
		this.addInput(valueUnitTypeList);

		valueTraceList = new SampleListInput("ValueTraceList", "Tracing",
				new ArrayList<SampleProvider>());
		valueTraceList.setUnitType(UserSpecifiedUnit.class);
		valueTraceList.setEntity(this);
		this.addInput(valueTraceList);

		valuePrecisionList = new IntegerListInput("ValuePrecisionList", "Tracing", new IntegerVector());
		this.addInput(valuePrecisionList);
	}

	public ExpressionLogger() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == valueUnitTypeList) {
			valueTraceList.setUnitTypeList(valueUnitTypeList.getUnitTypeList());
			return;
		}

		if (in == valueTraceList) {
			lastValueList.clear();
			for (int i=0; i<valueTraceList.getListSize(); i++) {
				lastValueList.add(Double.NaN);
			}
			return;
		}
	}

	@Override
	public void validate() {
		super.validate();
		if (valuePrecisionList.getValue().size() > 1) {
			if (valuePrecisionList.getValue().size() != valueTraceList.getValue().size()) {
				throw new InputErrorException( "There must be the same number of entries in ValueTraceList and ValuePrecisionList" );
			}
		}
	}

	@Override
	public void startUp() {
		super.startUp();

		// Start tracing the expression values
		if (valueTraceList.getListSize() > 0)
			this.doValueTrace();

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

	@Override
	protected void printColumnUnits(FileEntity file) {

		// Traced entities
		for (int i=0; i<stateTraceList.getValue().size(); i++) {
			file.format("\tState");
		}

		// Traced values
		for (int i=0; i<valueTraceList.getListSize(); i++) {
			String unit = Unit.getDisplayedUnit(valueTraceList.getUnitType(i));
			file.format("\t%s", unit);
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
				double val = valueTraceList.getValue().get(i).getNextSample(simTime);
				double factor = Unit.getDisplayedUnitFactor(valueTraceList.getUnitType(i));

				if (valuePrecisionList.getValue().size() == 1) {
					int precision = valuePrecisionList.getValue().get(0);
					file.format("\t%."+precision+"f", val/factor );
				}
				else if (valuePrecisionList.getValue().size() > 1) {
					int precision = valuePrecisionList.getValue().get(i);
					file.format("\t%."+precision+"f", val/factor );
				}
				else {
					file.format("\t%s", val/factor);
				}

				// Update the last recorded values for the traced expressions
				lastValueList.set(i, val);
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
				double val = valueTraceList.getValue().get(i).getNextSample(simTime);
				if (val != lastValueList.get(i)) {
					lastValueList.set(i, val);
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
