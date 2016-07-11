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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleListInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.StringProviders.StringProvListInput;
import com.jaamsim.StringProviders.StringProvider;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
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

public class ExpressionLogger extends DisplayEntity implements StateEntityListener {
	private FileEntity file;

	@Keyword(description = "An fixed interval at which entries will be written to the log file. "
			+ "This input is optional if state tracing or value tracing is specified.",
	         exampleList = { "24.0 h" })
	private final ValueInput interval;

	@Keyword(description = "The unit types for the quantities being logged. "
			+ "Use DimensionlessUnit for text entries.",
	         exampleList = {"DistanceUnit  SpeedUnit"})
	private final UnitTypeListInput unitTypeListInput;

	@Keyword(description = "One or more sources of data to be logged. "
			+ "Each source is specified by an Expression. Also acceptable are: "
			+ "a constant value, a Probability Distribution, TimeSeries, or a "
			+ "Calculation Object.",
	         exampleList = {"{ [Entity1].Output1 } { [Entity2].Output2 }"})
	protected final StringProvListInput dataSource;

	@Keyword(description = "If TRUE, entries are logged during the initialization period.",
	         exampleList = { "FALSE" })
	private final BooleanInput includeInitialization;

	@Keyword(description = "The time for the first log entry.",
	         exampleList = { "24.0 h" })
	private final ValueInput startTime;

	@Keyword(description = "The latest time at which to make an entry in the log.",
	         exampleList = { "8760.0 h" })
	private final ValueInput endTime;

	@Keyword(description = "A list of entities whose states will be traced. "
			+ "An entry in the log file is made every time one of the entities changes state. "
			+ "Each entity's state is written automatically to the log file - it is not necessary "
			+ "to add an expression to the DataSource keyword's input.",
	         exampleList = { "Server1 ExperssionThreshold1" })
	private final EntityListInput<StateEntity> stateTraceList;

	@Keyword(description = "The unit types for the values being traced. ",
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

		ArrayList<Class<? extends Unit>> defList = new ArrayList<>();
		unitTypeListInput = new UnitTypeListInput("UnitTypeList", "Key Inputs", defList);
		unitTypeListInput.setDefaultText("None");
		this.addInput(unitTypeListInput);

		dataSource = new StringProvListInput("DataSource", "Key Inputs",
				new ArrayList<StringProvider>());
		dataSource.setUnitType(UserSpecifiedUnit.class);
		dataSource.setEntity(this);
		dataSource.setDefaultText("None");
		this.addInput(dataSource);

		includeInitialization = new BooleanInput("IncludeInitialization", "Key Inputs", true);
		this.addInput(includeInitialization);

		startTime = new ValueInput("StartTime", "Key Inputs", 0.0d);
		startTime.setUnitType(TimeUnit.class);
		startTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(startTime);

		endTime = new ValueInput("EndTime", "Key Inputs", Double.POSITIVE_INFINITY);
		endTime.setUnitType(TimeUnit.class);
		endTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(endTime);

		stateTraceList = new EntityListInput<>(StateEntity.class, "StateTraceList", "Tracing",
				new ArrayList<StateEntity>());
		this.addInput(stateTraceList);

		ArrayList<Class<? extends Unit>> valDefList = new ArrayList<>();
		valueUnitTypeList = new UnitTypeListInput("ValueUnitTypeList", "Tracing",
				valDefList);
		valueUnitTypeList.setDefaultText("None");
		this.addInput(valueUnitTypeList);

		valueTraceList = new SampleListInput("ValueTraceList", "Tracing",
				new ArrayList<SampleProvider>());
		valueTraceList.setUnitType(UserSpecifiedUnit.class);
		valueTraceList.setEntity(this);
		valueTraceList.setDefaultText("None");
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

		if (in == unitTypeListInput) {
			dataSource.setUnitTypeList(unitTypeListInput.getUnitTypeList());
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
	public void earlyInit() {
		super.earlyInit();

		// Close the file if it is already open
		if (file != null && Simulation.isFirstRun()) {
			file.close();
			file = null;
		}

		// Create the report file
		if (file == null) {
			StringBuilder tmp = new StringBuilder(InputAgent.getReportFileName(InputAgent.getRunName()));
			tmp.append("-").append(this.getName());
			tmp.append(".log");
			file = new FileEntity(tmp.toString());
		}
	}

	@Override
	public void startUp() {
		super.startUp();

		// Print run number header if multiple runs are to be performed
		if (Simulation.isMultipleRuns()) {
			if (!Simulation.isFirstRun())
				file.format("%n%n");
			file.format("%s%n", Simulation.getRunHeader());
		}

		// WRITE THE HEADER LINE
		// a) Simulation time
		file.format("%n%s", "SimTime");

		// b) Traced entities
		for (StateEntity ent : stateTraceList.getValue()) {
			file.format("\t%s", ent.getName());
		}

		// c) Traced values
		ArrayList<String> valToks = new ArrayList<>();
		valueTraceList.getValueTokens(valToks);
		for (String str : valToks) {
			if (str.equals("{") || str.equals("}"))
				continue;
			file.format("\t%s", str);
		}

		// d) Logged expressions
		ArrayList<String> toks = new ArrayList<>();
		dataSource.getValueTokens(toks);
		for (String str : toks) {
			if (str.equals("{") || str.equals("}"))
				continue;
			file.format("\t%s", str);
		}

		// WRITE THE UNITS LINE
		// a) Simulation time units
		String unit = Unit.getDisplayedUnit(TimeUnit.class);
		file.format("%n%s", unit);

		// b) Traced entities
		for (int i=0; i<stateTraceList.getValue().size(); i++) {
			file.format("\tState");
		}

		// c) Traced values
		for (int i=0; i<valueTraceList.getListSize(); i++) {
			unit = Unit.getDisplayedUnit(valueTraceList.getUnitType(i));
			file.format("\t%s", unit);
		}

		// d) Logged expressions
		for (int i=0; i<dataSource.getListSize(); i++) {
			unit = Unit.getDisplayedUnit(dataSource.getUnitType(i));
			file.format("\t%s", unit);
		}

		// Empty the output buffer
		file.flush();

		// Start tracing the expression values
		if (valueTraceList.getListSize() > 0)
			this.doValueTrace();

		// Start log entries at fixed intervals
		if (interval.getValue() != null)
			this.scheduleProcess(startTime.getValue(), 5, endActionTarget);
	}

	private void startAction() {

		// Schedule the next time an entry in the log file will be written
		this.scheduleProcess(interval.getValue(), 5, endActionTarget);
	}

	final void endAction() {

		// Stop the log if the end time has been reached
		double simTime = getSimTime();
		if (simTime > endTime.getValue())
			return;

		// Record the entry in the log
		this.recordEntry(simTime);

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

	/**
	 * Writes an entry to the log file.
	 */
	private void recordEntry(double simTime) {

		// Skip the log entry if the run is still initializing
		if (!includeInitialization.getValue() && simTime < Simulation.getInitializationTime())
			return;

		// Skip the log entry if it is outside the time range
		if (simTime < startTime.getValue() || simTime > endTime.getValue())
			return;

		// Write the time for the log entry
		double factor = Unit.getDisplayedUnitFactor(TimeUnit.class);
		file.format("%n%s", simTime/factor);

		// Write the state values
		for (StateEntity ent : stateTraceList.getValue()) {
			file.format("\t%s", ent.getPresentState(simTime));
		}

		try {
			// Write the traced expression values
			for (int i=0; i<valueTraceList.getListSize(); i++) {
				double val = valueTraceList.getValue().get(i).getNextSample(simTime);
				factor = Unit.getDisplayedUnitFactor(valueTraceList.getUnitType(i));

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

			// Write the expression values
			for (int i=0; i<dataSource.getListSize(); i++) {
				StringProvider samp = dataSource.getValue().get(i);
				factor = Unit.getDisplayedUnitFactor(dataSource.getUnitType(i));
				file.format("\t%s", samp.getNextString(simTime, "%s", factor));
			}
		}
		catch (Exception e) {
			error(e.getMessage());
		}

		// Empty the output buffer
		file.flush();
	}

	@Override
	public boolean isWatching(StateEntity ent) {
		return stateTraceList.getValue().contains(ent);
	}

	@Override
	public void updateForStateChange(StateEntity ent, StateRecord prev, StateRecord next) {
		this.recordEntry(getSimTime());
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
		if (simTime > endTime.getValue())
			return;

		// Record the entry in the log
		this.recordEntry(simTime);

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
