/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.DirInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.states.StateEntity;
import com.jaamsim.states.StateRecord;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityTarget;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.GUIFrame;

public class ReportGenerator extends DisplayEntity {

	@Keyword(description = "The directory in which to place the output report.\n" +
			"Defaults to the directory containing the configuration file for the run.",
			example = "ReportGenerator1 ReportDirectory { 'c:\reports\' }")
	private final DirInput reportDirectory;

	@Keyword(description = "The initialization period for the simulation run.\n" +
			"The model will run for the initialization period and then clear " +
			"the statistics and execute for the specified run duration. The " +
			"total length of the simulation run will be the sum of the inputs " +
			"for InitializationDuration and RunDurationList.",
			example = "ReportGenerator1 InitializationDuration { 720 h }")
	private final ValueInput initializationDuration;

	@Keyword(description = "A list of run durations over which statistics will be recorded.\n" +
			"If a list of N durations are provided, then N separate report files will be generated. " +
			"The total length of the simulation run will be the sum of the inputs " +
			"for InitializationDuration and RunDurationList.",
			example = "ReportGenerator1 RunDurationList { 1  1  1  y }")
	private final ValueListInput runDurationList;

	private int reportNumber;
	private double reportStartTime;
	private double reportEndTime;

	private final ProcessTarget performInitialisation = new PerformInitialisationTarget(this);
	private final ProcessTarget performRunEnd = new PerformRunEndTarget(this);

	{
		attributeDefinitionList.setHidden(true);

		reportDirectory = new DirInput("ReportDirectory", "Key Inputs", null);
		this.addInput(reportDirectory);

		initializationDuration = new ValueInput("InitializationDuration", "Key Inputs", 0.0);
		initializationDuration.setUnitType(TimeUnit.class);
		initializationDuration.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(initializationDuration);

		DoubleVector def = new DoubleVector();
		def.add(31536000.0d);
		runDurationList = new ValueListInput("RunDurationList", "Key Inputs", def);
		runDurationList.setUnitType(TimeUnit.class);
		runDurationList.setValidRange(1e-15d, Double.POSITIVE_INFINITY);
		this.addInput(runDurationList);
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == reportDirectory) {
			InputAgent.setReportDirectory(reportDirectory.getDir());
			return;
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		reportNumber = 1;
		reportStartTime = initializationDuration.getValue();
		reportEndTime = reportStartTime + runDurationList.getValue().get(0);
	}

	@Override
	public void startUp() {
		super.startUp();

		// Wait for the end of initialisation
		double dt = initializationDuration.getValue();
		this.scheduleProcess(dt, 5, performInitialisation);

		// Wait for the first report
		dt += runDurationList.getValue().get(0);
		this.scheduleProcess(dt, 5, performRunEnd);
	}

	private static class PerformInitialisationTarget extends EntityTarget<ReportGenerator> {
		public PerformInitialisationTarget(ReportGenerator gen) {
			super(gen, "performInitialisation");
		}

		@Override
		public void process() {
			ent.performInitialisation();
		}
	}

	void performInitialisation() {

		// Reset the statistics for each entity in the model
		for (Entity ent : Entity.getAll()) {
			ent.clearStatistics();
		}

		// Reset state statistics
		for ( StateEntity each : Entity.getClonesOfIterator(StateEntity.class) ) {
			each.clearReportStats();
		}
	}

	private static class PerformRunEndTarget extends EntityTarget<ReportGenerator> {
		public PerformRunEndTarget(ReportGenerator gen) {
			super(gen, "performRunEnd");
		}

		@Override
		public void process() {
			ent.performRunEnd();
		}
	}

	void performRunEnd() {

		// Print the output report
		this.printReport(this.getSimTime());

		// Stop if no more reports are to be printed
		if (reportNumber == runDurationList.getValue().size()) {

			// Terminate the run if in batch mode
			if (InputAgent.getBatch()) {
				InputAgent.closeLogFile();
				GUIFrame.shutdown(0);
			}

			// Otherwise, just pause the run
			EventManager.current().pause();
			return;
		}

		// Re-initialise the statistics
		this.performInitialisation();

		// Schedule the next report
		reportNumber++;
		reportStartTime = this.getSimTime();
		double dt = runDurationList.getValue().get(reportNumber-1);
		reportEndTime = reportStartTime + dt;
		this.scheduleProcess(dt, 5, performRunEnd);
	}

	private void printReport(double simTime) {

		// Create the report file
		StringBuilder tmp = new StringBuilder("");
		tmp.append(InputAgent.getReportFileName(InputAgent.getRunName()));
		if (runDurationList.getValue().size() > 1) {
			tmp.append("-").append(reportNumber);
		}
		tmp.append(".rep");
		FileEntity file = new FileEntity(tmp.toString());

		// Identify the classes that were used in the model
		ArrayList<Class<? extends Entity>> newClasses = new ArrayList<Class<? extends Entity>>();
		for (Entity ent : Entity.getAll()) {
			if (ent.testFlag(Entity.FLAG_GENERATED))
				continue;
			if (!newClasses.contains(ent.getClass()))
				newClasses.add(ent.getClass());
		}

		// Loop through the classes and identify the instances
		for (Class<? extends Entity> newClass : newClasses) {
			for (Entity ent : Entity.getAll()) {
				if (ent.testFlag(Entity.FLAG_GENERATED))
					continue;
				if (ent.getClass() != newClass)
					continue;

				// Print the outputs
				ReportGenerator.printOutputs(file, ent, simTime);

				// Print the states
				if ( !(ent instanceof StateEntity) )
					continue;
				ReportGenerator.printStates(file, (StateEntity)ent);
			}
		}

		// Close the report file
		file.close();
	}

	public static void printOutputs(FileEntity file, Entity ent, double simTime) {

		// Loop through the outputs
		boolean blankLine = false;
		ArrayList<OutputHandle> handles = OutputHandle.getOutputHandleList(ent);
		for (OutputHandle o : handles) {

			// Should this output appear in the report?
			if (!o.isReportable())
				continue;

			// Add a blank line before each new entity
			if (!blankLine) {
				file.format("%n");
				blankLine = true;
			}

			// Is there a preferred unit in which to display the output?
			Class<? extends Unit> ut = o.getUnitType();
			String unitString = Unit.getSIUnit(ut);
			double factor = 1.0;
			Unit u = Unit.getPreferredUnit(ut);
			if (u != null) {
				unitString = u.getInputName();
				factor = u.getConversionFactorToSI();
			}

			// Is the output a number?
			String s;
			if (o.isNumericValue())
				s = String.valueOf(o.getValueAsDouble(simTime, Double.NaN)/factor);
			else {
				unitString = Unit.getSIUnit(ut);  // lists of doubles are not converted to preferred units yet
				s = o.getValue(simTime, o.getReturnType()).toString();
			}

			// Does the output require a unit to be shown?
			if (ut == Unit.class || ut == DimensionlessUnit.class) {
				file.format("%s\tOutput[%s]\t%s%n",
						ent.getName(), o.getName(), s);
			}
			else {
				file.format("%s\tOutput[%s, %s]\t%s%n",
						ent.getName(), o.getName(), unitString, s);
			}
		}
	}

	public static void printStates(FileEntity file, StateEntity ent) {

		long totalTicks = 0;
		long workingTicks = 0;

		// Loop through the states
		for (StateRecord st : ent.getStateRecs()) {
			long ticks = ent.getTicksInState(st);
			if (ticks == 0)
				continue;

			double hours = ticks / Simulation.getSimTimeFactor();
			file.format("%s\tStateTime[%s, h]\t%f\n", ent.getName(), st.name, hours);

			totalTicks += ticks;
			if(st.working)
				workingTicks += ticks;
		}

		file.format("%s\tStateTime[%s, h]\t%f\n", ent.getName(), "TotalTime", totalTicks / Simulation.getSimTimeFactor());
		file.format("%s\tStateTime[%s, h]\t%f\n", ent.getName(), "WorkingTime", workingTicks / Simulation.getSimTimeFactor());
	}

	@Output(name = "ReportNumber",
	 description = "The index for the present report.",
	    unitType = DimensionlessUnit.class,
	  reportable = true)
	public int getReportNumber(double simTime) {
		return reportNumber;
	}

	@Output(name = "ReportStartTime",
	 description = "The time at which statistics began to be collected for the present report.",
	    unitType = TimeUnit.class,
	  reportable = true)
	public double getReportStartTime(double simTime) {
		return reportStartTime;
	}

	@Output(name = "ReportEndTime",
	 description = "The time at which statistics finished being collected for the present report.",
	    unitType = TimeUnit.class,
	  reportable = true)
	public double getReportEndTime(double simTime) {
		return reportEndTime;
	}

}
