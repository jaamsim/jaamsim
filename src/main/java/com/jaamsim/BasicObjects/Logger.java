/*
 * JaamSim Discrete Event Simulation
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

import java.io.File;
import java.util.ArrayList;

import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.StringProviders.StringProvListInput;
import com.jaamsim.StringProviders.StringProvider;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeListInput;
import com.jaamsim.units.TimeUnit;

public abstract class Logger extends DisplayEntity {

	@Keyword(description = "Not Used")
	private final UnitTypeListInput unitTypeListInput;

	@Keyword(description = "One or more selected outputs to be logged. Each output is specified "
	                     + "by an expression.\n\n"
	                     + "It is best to include only dimensionless quantities and non-numeric "
	                     + "outputs in the DataSource input. "
	                     + "An output with dimensions can be made non-dimensional by dividing it "
	                     + "by 1 in the desired unit, e.g. '[Queue1].AverageQueueTime / 1[h]' is "
	                     + "the average queue time in hours. "
	                     + "A dimensional number will be displayed along with its unit. "
	                     + "The 'format' function can be used if a fixed number of decimal places "
	                     + "is required.",
	         exampleList = {"{ [Queue1].QueueLengthAverage }"
	                     + " { '[Queue1].AverageQueueTime / 1[h]' }"})
	private final StringProvListInput dataSource;

	@Keyword(description = "If TRUE, an individual .log file will be created for each simulation "
	                     + "run and the suffix '-sNrM' will be added to each .log file name, "
	                     + "where N and M are the scenario and replication numbers for the run. "
	                     + "If FALSE, a single .log file will be created that contains the "
	                     + "outputs for all the simulation runs. "
	                     + "This input is ignored if multiple runs are to be executed and the "
	                     + "NumberOfThreads input is greater than one, in which case individual "
	                     + ".log files will be created.")
	private final BooleanProvInput separateFiles;

	@Keyword(description = "If TRUE, log entries are recorded during the initialization period.")
	private final BooleanProvInput includeInitialization;

	@Keyword(description = "The time at which the log starts recording entries.",
	         exampleList = { "24.0 h" })
	private final SampleInput startTime;

	@Keyword(description = "The time at which the log stops recording entries.",
	         exampleList = { "8760.0 h" })
	private final SampleInput endTime;

	private FileEntity file;
	private double logTime;
	private DisplayEntity logEntity;

	{
		active.setHidden(false);

		unitTypeListInput = new UnitTypeListInput("UnitTypeList", KEY_INPUTS, null);
		unitTypeListInput.setHidden(true);
		this.addInput(unitTypeListInput);

		dataSource = new StringProvListInput("DataSource", KEY_INPUTS,
				new ArrayList<StringProvider>());
		this.addInput(dataSource);

		separateFiles = new BooleanProvInput("SeparateFiles", KEY_INPUTS, false);
		this.addInput(separateFiles);

		includeInitialization = new BooleanProvInput("IncludeInitialization", KEY_INPUTS, true);
		this.addInput(includeInitialization);

		startTime = new SampleInput("StartTime", KEY_INPUTS, 0.0d);
		startTime.setUnitType(TimeUnit.class);
		startTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		startTime.setOutput(true);
		this.addInput(startTime);

		endTime = new SampleInput("EndTime", KEY_INPUTS, Double.POSITIVE_INFINITY);
		endTime.setUnitType(TimeUnit.class);
		endTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		endTime.setOutput(true);
		this.addInput(endTime);
	}

	public Logger() {}

	@Override
	public void earlyInit() {
		super.earlyInit();

		unitTypeListInput.reset();  // Delete an unnecessary input

		logTime = 0.0d;
		logEntity = null;

		// Close the file if it is already open
		JaamSimModel simModel = getJaamSimModel();
		if (file != null && (simModel.isFirstRun() || isSeparateFiles(0.0d))) {
			file.close();
			file = null;
		}

		if (!isActive())
			return;

		// Create the report file
		if (file == null) {
			StringBuilder sb = new StringBuilder();
			sb.append("-").append(this.getName());
			if (isSeparateFiles(0.0d)) {
				sb.append("-s").append(simModel.getScenarioNumber());
				sb.append("r").append(simModel.getReplicationNumber());
			}
			sb.append(".log");
			String fileName = simModel.getReportFileName(sb.toString());
			if (fileName == null)
				return;
			File f = new File(fileName);
			if (f.exists() && !f.delete())
				error("Cannot delete the existing log file %s", f);
			file = new FileEntity(f);
		}

		// Print the detailed run information to the file
		if (getJaamSimModel().isFirstRun() || isSeparateFiles(0.0d))
			InputAgent.printReport(getSimulation(), file, 0.0d);

		// Print run number header if multiple runs are to be performed
		if (getJaamSimModel().isMultipleRuns() && !isSeparateFiles(0.0d)) {
			if (!getJaamSimModel().isFirstRun()) {
				file.format("%n");
			}
			file.format("%n%s%n", getJaamSimModel().getRunHeader());
		}

		// Print the title for each column
		// (a) Simulation time
		String unit = getJaamSimModel().getDisplayedUnit(TimeUnit.class);
		file.format("%nthis.SimTime/1[%s]", unit);

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

		// Empty the output buffer
		file.flush();
	}

	private boolean isSeparateFiles(double simTime) {
		int numThreads = getJaamSimModel().getSimulation().getNumberOfThreads();
		return separateFiles.getNextBoolean(this, simTime) || numThreads > 1;
	}

	private boolean isIncludeInitialization(double simTime) {
		return includeInitialization.getNextBoolean(this, simTime);
	}

	public void scheduleLogEntry() {
		if (recordLogEntryHandle.isScheduled())
			return;
		EventManager.scheduleTicks(0L, 99, true, recordLogEntryTarget, recordLogEntryHandle);
	}

	private final EventHandle recordLogEntryHandle = new EventHandle();

	private final EntityTarget<Logger> recordLogEntryTarget = new EntityTarget<Logger>(this, "recordLogEntry") {
		@Override
		public void process() {
			recordLogEntry(ent.getSimTime(), null);
		}
	};

	/**
	 * Writes an entry to the log file.
	 */
	protected void recordLogEntry(double simTime, DisplayEntity ent) {

		if (!isActive())
			return;

		// Skip the log entry if the log file has been closed at the end of the run duration
		if (file == null)
			return;

		// Skip the log entry if the run is still initializing
		if (!isIncludeInitialization(simTime) && simTime < getSimulation().getInitializationTime())
			return;

		// Skip the log entry if it is outside the time range
		if (simTime < getStartTime(simTime) || simTime > getEndTime(simTime))
			return;

		// Record the time for the log entry
		logTime = simTime;
		logEntity = ent;

		// Write the time for the log entry
		double factor = getJaamSimModel().getDisplayedUnitFactor(TimeUnit.class);
		file.format("%n%s", simTime/factor);

		// Write any additional columns for the log entry
		this.recordEntry(file, simTime, ent);

		// Write the expression values
		for (int i=0; i<dataSource.getListSize(); i++) {
			String str;
			try {
				str = dataSource.getNextString(i, this, simTime);
			}
			catch (Exception e) {
				str = e.getMessage();
			}
			file.format("\t%s", str);
		}

		// If running in real time mode, empty the file buffer after each entity is logged
		if (!getJaamSimModel().isBatchRun() && getJaamSimModel().isRealTime())
			file.flush();
	}

	protected double getStartTime(double simTime) {
		return startTime.getNextSample(this, simTime);
	}

	protected double getEndTime(double simTime) {
		return endTime.getNextSample(this, simTime);
	}

	protected abstract void printColumnTitles(FileEntity file);

	protected abstract void recordEntry(FileEntity file, double simTime, DisplayEntity ent);

	@Override
	public void doEnd() {
		super.doEnd();

		// Write the last log entry if one is scheduled
		if (recordLogEntryHandle.isScheduled()) {
			EventManager.killEvent(recordLogEntryHandle);
			recordLogEntry(getSimTime(), null);
		}

		// Flush the log file's print buffer
		if (file == null)
			return;
		file.flush();

		// Close the report file
		if (getJaamSimModel().isLastRun() || isSeparateFiles(getSimTime())) {
			file.close();
			file = null;
		}
	}

	@Override
	public void close() {
		super.close();
		if (file == null)
			return;
		file.flush();
		file.close();
		file = null;
	}

	@Output(name = "LogTime",
	 description = "The simulation time at which the last log entry was made.",
	    unitType = TimeUnit.class)
	public double getLogTime(double simTime) {
		return logTime;
	}

	@Output(name = "LogEntity",
	 description = "The entity that triggered the last log entry.")
	public DisplayEntity getLogEntity(double simTime) {
		return logEntity;
	}

}
