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

import com.jaamsim.events.EventManager;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.DirInput;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityTarget;
import com.sandwell.JavaSimulation.FileEntity;
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
			"for InitializationDuration and RunDuration.",
			example = "ReportGenerator1 InitializationDuration { 720 h }")
	private final ValueInput initializationDuration;

	@Keyword(description = "The duration over which all statistics will be recorded.\n" +
			"The total length of the simulation run will be the sum of the inputs " +
			"for InitializationDuration and RunDuration.",
			example = "ReportGenerator1 RunDuration { 8760 h }")
	private final ValueInput runDuration;

	{
		attributeDefinitionList.setHidden(true);

		reportDirectory = new DirInput("ReportDirectory", "Key Inputs", null);
		this.addInput(reportDirectory);

		initializationDuration = new ValueInput("InitializationDuration", "Key Inputs", 0.0);
		initializationDuration.setUnitType(TimeUnit.class);
		initializationDuration.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(initializationDuration);

		runDuration = new ValueInput("RunDuration", "Key Inputs", 31536000.0d);
		runDuration.setUnitType(TimeUnit.class);
		runDuration.setValidRange(1e-15d, Double.POSITIVE_INFINITY);
		this.addInput(runDuration);
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
	public void startUp() {
		super.startUp();

		// Wait for the end of initialisation
		double dt = initializationDuration.getValue();
		this.scheduleProcess(dt, 5, new PerformInitialisationTarget(this, "performInitialisation"));

		// Wait for the end of the run
		dt += runDuration.getValue();
		this.scheduleProcess(dt, 5, new PerformRunEndTarget(this, "performRunEnd"));
	}

	private static class PerformInitialisationTarget extends EntityTarget<ReportGenerator> {
		public PerformInitialisationTarget(ReportGenerator gen, String method) {
			super(gen, method);
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
	}

	private static class PerformRunEndTarget extends EntityTarget<ReportGenerator> {
		public PerformRunEndTarget(ReportGenerator gen, String method) {
			super(gen, method);
		}

		@Override
		public void process() {
			ent.performRunEnd();
		}
	}

	void performRunEnd() {

		// Print the output report
		this.printReport(this.getSimTime());

		// Close warning/error trace file
		InputAgent.closeLogFile();

		// Stop the run if in batch mode
		if (InputAgent.getBatch())
			GUIFrame.shutdown(0);

		// Otherwise, just pause the run
		EventManager.current().pause();
	}

	private void printReport(double simTime) {

		// Create the report file
		String fileName = InputAgent.getReportFileName(InputAgent.getRunName() + ".rep");
		FileEntity file = new FileEntity(fileName);

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

				// Loop through the outputs for this instance
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
		}

		// Close the report file
		file.flush();
		file.close();
	}

}
