/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.StringProviders.StringProvListInput;
import com.jaamsim.StringProviders.StringProvider;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.UnitTypeListInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class ExpressionLogger extends DisplayEntity {
	private FileEntity file;

	@Keyword(description = "The unit types for the quantities being logged. "
			+ "Use DimensionlessUnit for text entries.",
	         exampleList = {"DistanceUnit  SpeedUnit"})
	private final UnitTypeListInput unitTypeListInput;

	@Keyword(description = "One or more sources of data to be logged.\n"
			+ "Each source is specified by an Expression. Also acceptable are: "
			+ "a constant value, a Probability Distribution, TimeSeries, or a "
			+ "Calculation Object.",
	         exampleList = {"{ [Entity1].Output1 } { [Entity2].Output2 }"})
	protected final StringProvListInput dataSource;

	@Keyword(description = "The interval between entries in the log file.",
	         exampleList = { "24.0 h" })
	private final ValueInput interval;

	@Keyword(description = "If TRUE, entries are logged during the initialization period.",
	         exampleList = { "FALSE" })
	private final BooleanInput includeInitialization;

	@Keyword(description = "The time for the first log entry.",
	         exampleList = { "24.0 h" })
	private final ValueInput startTime;

	@Keyword(description = "The latest time at which to make an entry in the log.",
	         exampleList = { "8760.0 h" })
	private final ValueInput endTime;

	{
		unitTypeListInput = new UnitTypeListInput("UnitTypeList", "Key Inputs", null);
		unitTypeListInput.setRequired(true);
		this.addInput(unitTypeListInput);

		dataSource = new StringProvListInput("DataSource", "Key Inputs", null);
		dataSource.setUnitType(UserSpecifiedUnit.class);
		dataSource.setEntity(this);
		dataSource.setRequired(true);
		this.addInput(dataSource);

		interval = new ValueInput("Interval", "Key Inputs", null);
		interval.setUnitType(TimeUnit.class);
		interval.setValidRange(1.0e-10, Double.POSITIVE_INFINITY);
		interval.setRequired(true);
		this.addInput(interval);

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
	}

	public ExpressionLogger() {}

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

		// Close the file if it is already open
		if (file != null) {
			file.close();
			file = null;
		}

		// Create the report file
		StringBuilder tmp = new StringBuilder(InputAgent.getReportFileName(InputAgent.getRunName()));
		tmp.append("-").append(this.getName());
		tmp.append(".log");
		file = new FileEntity(tmp.toString());

		// Write the header line
		file.format("%n%s", "SimTime");
		ArrayList<String> toks = new ArrayList<>();
		dataSource.getValueTokens(toks);
		for (String str : toks) {
			if (str.equals("{") || str.equals("}"))
				continue;
			file.format("\t%s", str);
		}

		// Write the units line
		String unit = Unit.getDisplayedUnit(TimeUnit.class);
		file.format("%n%s", unit);
		for (int i=0; i<dataSource.getListSize(); i++) {
			unit = Unit.getDisplayedUnit(dataSource.getUnitType(i));
			file.format("\t%s", unit);
		}

		// Empty the output buffer
		file.flush();

	}

	@Override
	public void startUp() {
		super.startUp();

		this.scheduleProcess(startTime.getValue(), 5, endActionTarget);
	}

	private void startAction() {

		// Schedule the next time an entry in the log file will be written
		this.scheduleProcess(interval.getValue(), 5, endActionTarget);
	}

	private void endAction() {

		// Stop the log if the end time has been reached
		double simTime = getSimTime();
		if (simTime > endTime.getValue())
			return;

		// Skip the log entry if the run is still initializing
		if (!includeInitialization.getValue() && simTime < Simulation.getInitializationTime()) {
			this.startAction();
			return;
		}

		// Write the time for the log entry
		double factor = Unit.getDisplayedUnitFactor(TimeUnit.class);
		file.format("%n%s", simTime/factor);

		// Write the entry in the log file
		try {
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

}
