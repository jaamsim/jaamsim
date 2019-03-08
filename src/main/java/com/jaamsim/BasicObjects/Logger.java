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
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.StringProviders.StringProvListInput;
import com.jaamsim.StringProviders.StringProvider;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeListInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;

public abstract class Logger extends DisplayEntity {

	@Keyword(description = "The unit types for the outputs specified by the DataSource keyword. "
	                     + "Use DimensionlessUnit for non-numeric outputs such as strings, "
	                     + "entities, and arrays. "
	                     + "If the DataSource keyword has more entries than the UnitTypesList "
	                     + "keyword, then the last unit type in the list is applied to the "
	                     + "remaining DataSource entries.\n\n"
	                     + "It is best to leave this input blank and use only dimensionless "
	                     + "quantities and non-numeric outputs in the DataSource input.",
	         exampleList = {"DistanceUnit  SpeedUnit"})
	private final UnitTypeListInput unitTypeListInput;

	@Keyword(description = "One or more selected outputs to be logged. Each output is specified "
	                     + "by an expression.\n\n"
	                     + "It is best to include only dimensionless quantities and non-numeric "
	                     + "outputs in the DataSource input. "
	                     + "An output with dimensions can be made non-dimensional by dividing it "
	                     + "by 1 in the desired unit, e.g. '[Queue1].AverageQueueTime / 1[h]' is "
	                     + "the average queue time in hours.\n\n"
	                     + "If a number with dimensions is to be recorded, its unit type must "
	                     + "first be entered in the correct position in the input to the "
	                     + "UnitTypeList keyword.",
	         exampleList = {"{ [Queue1].QueueLengthAverage }"
	                     + " { '[Queue1].AverageQueueTime / 1[h]' }"})
	private final StringProvListInput dataSource;

	@Keyword(description = "If TRUE, log entries are recorded during the initialization period.",
	         exampleList = { "FALSE" })
	private final BooleanInput includeInitialization;

	@Keyword(description = "The time at which the log starts recording entries.",
	         exampleList = { "24.0 h" })
	private final ValueInput startTime;

	@Keyword(description = "The time at which the log stops recording entries.",
	         exampleList = { "8760.0 h" })
	private final ValueInput endTime;

	private FileEntity file;
	private double logTime;

	{
		ArrayList<Class<? extends Unit>> defList = new ArrayList<>();
		defList.add(DimensionlessUnit.class);
		unitTypeListInput = new UnitTypeListInput("UnitTypeList", KEY_INPUTS, defList);
		this.addInput(unitTypeListInput);

		dataSource = new StringProvListInput("DataSource", KEY_INPUTS,
				new ArrayList<StringProvider>());
		dataSource.setUnitType(DimensionlessUnit.class);
		dataSource.setEntity(this);
		this.addInput(dataSource);

		includeInitialization = new BooleanInput("IncludeInitialization", KEY_INPUTS, true);
		this.addInput(includeInitialization);

		startTime = new ValueInput("StartTime", KEY_INPUTS, 0.0d);
		startTime.setUnitType(TimeUnit.class);
		startTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(startTime);

		endTime = new ValueInput("EndTime", KEY_INPUTS, Double.POSITIVE_INFINITY);
		endTime.setUnitType(TimeUnit.class);
		endTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(endTime);
	}

	public Logger() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == unitTypeListInput) {
			dataSource.setUnitTypeList(unitTypeListInput.getUnitTypeList());
			return;
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		logTime = 0.0d;

		// Close the file if it is already open
		if (file != null && getSimulation().isFirstRun()) {
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

		// Print the detailed run information to the file
		if (getSimulation().isFirstRun())
			getSimulation().printReport(file, 0.0d);

		// Print run number header if multiple runs are to be performed
		if (getSimulation().isMultipleRuns()) {
			if (!getSimulation().isFirstRun()) {
				file.format("%n");
			}
			file.format("%n%s%n", getSimulation().getRunHeader());
		}

		// Print the title for each column
		// (a) Simulation time
		file.format("%n%s", "SimTime");

		// (b) Print at titles for any additional columns
		this.printColumnTitles(file);

		// (c) Print the mathematical expressions to be logged
		ArrayList<String> toks = new ArrayList<>();
		dataSource.getValueTokens(toks);
		for (String str : toks) {
			if (str.equals("{") || str.equals("}"))
				continue;
			file.format("\t%s", str);
		}

		// Print the units for each column
		// (a) Simulation time units
		String unit = Unit.getDisplayedUnit(TimeUnit.class);
		file.format("%n%s", unit);

		// (b) Print the units for any additional columns
		this.printColumnUnits(file);

		// (c) Print the units for the mathematical expressions
		for (int i=0; i<dataSource.getListSize(); i++) {
			unit = Unit.getDisplayedUnit(dataSource.getUnitType(i));
			file.format("\t%s", unit);
		}

		// Empty the output buffer
		file.flush();
	}

	/**
	 * Writes an entry to the log file.
	 */
	protected void recordLogEntry(double simTime) {

		// Skip the log entry if the log file has been closed at the end of the run duration
		if (file == null)
			return;

		// Skip the log entry if the run is still initializing
		if (!includeInitialization.getValue() && simTime < getSimulation().getInitializationTime())
			return;

		// Skip the log entry if it is outside the time range
		if (simTime < startTime.getValue() || simTime > endTime.getValue())
			return;

		// Record the time for the log entry
		logTime = simTime;

		// Write the time for the log entry
		double factor = Unit.getDisplayedUnitFactor(TimeUnit.class);
		file.format("%n%s", simTime/factor);

		// Write any additional columns for the log entry
		this.recordEntry(file, simTime);

		// Write the expression values
		for (int i=0; i<dataSource.getListSize(); i++) {
			String str;
			try {
				StringProvider samp = dataSource.getValue().get(i);
				factor = Unit.getDisplayedUnitFactor(dataSource.getUnitType(i));
				str = samp.getNextString(simTime, factor);
			}
			catch (Exception e) {
				str = e.getMessage();
			}
			file.format("\t%s", str);
		}

		// If running in real time mode, empty the file buffer after each entity is logged
		if (!InputAgent.getBatch() && getSimulation().isRealTime())
			file.flush();
	}

	protected double getStartTime() {
		return startTime.getValue();
	}

	protected double getEndTime() {
		return endTime.getValue();
	}

	protected abstract void printColumnTitles(FileEntity file);

	protected abstract void printColumnUnits(FileEntity file);

	protected abstract void recordEntry(FileEntity file, double simTime);

	@Override
	public void doEnd() {
		super.doEnd();
		file.flush();

		// Close the report file
		if (getSimulation().isLastRun()) {
			file.close();
			file = null;
		}
	}

	@Output(name = "LogTime",
	 description = "The simulation time at which the last log entry was made.",
	    unitType = TimeUnit.class)
	public double getLogTime(double simTime) {
		return logTime;
	}

}
