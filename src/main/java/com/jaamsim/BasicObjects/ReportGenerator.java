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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.DirInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.states.StateEntity;
import com.jaamsim.units.TimeUnit;

public class ReportGenerator extends DisplayEntity {

	@Keyword(description = "The directory in which to place the output report.\n" +
			"Defaults to the directory containing the configuration file for the run.",
			example = "ReportGenerator1 ReportDirectory { 'c:\reports\' }")
	private final DirInput reportDirectory;

	private double reportStartTime;
	private double reportEndTime;

	private final ProcessTarget performInitialisation = new PerformInitialisationTarget(this);

	{
		attributeDefinitionList.setHidden(true);

		reportDirectory = new DirInput("ReportDirectory", "Key Inputs", null);
		this.addInput(reportDirectory);
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
		reportStartTime = Simulation.getInitializationTime();
		reportEndTime = reportStartTime + Simulation.getRunDuration();
	}

	@Override
	public void startUp() {
		super.startUp();

		// Wait for the end of initialisation
		double dt = Simulation.getInitializationTime();
		this.scheduleProcess(dt, 5, performInitialisation);
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

	@Override
	public void doEnd() {

		// Create the report file
		StringBuilder tmp = new StringBuilder("");
		tmp.append(InputAgent.getReportFileName(InputAgent.getRunName()));
		tmp.append(".rep");
		FileEntity file = new FileEntity(tmp.toString());

		// Identify the classes that were used in the model
		ArrayList<Class<? extends Entity>> newClasses = new ArrayList<>();
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
				ent.printReport(file, this.getSimTime());
			}
		}

		// Close the report file
		file.close();
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
