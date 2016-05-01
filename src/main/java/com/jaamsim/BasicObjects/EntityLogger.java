/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Output;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;

public class EntityLogger extends LinkedComponent {
	private FileEntity file;
	private double logTime;

	{
		stateAssignment.setHidden(true);
		defaultEntity.setHidden(true);
	}

	public EntityLogger() {}

	@Override
	public void earlyInit() {
		super.earlyInit();

		logTime = 0.0d;

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
				file.format("%n");
			file.format("%s%n%n", Simulation.getRunHeader());
		}

		// Print the header information to the file
		Simulation.getInstance().printReport(file, 0.0d);
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);

		// Log the entity's outputs
		file.format("%n");
		logTime = this.getSimTime();
		double factor = Unit.getDisplayedUnitFactor(TimeUnit.class);
		String unitString = Unit.getDisplayedUnit(TimeUnit.class);
		file.format("%s\t%s\t%s\t%s%n", ent.getName(), "SimTime", logTime/factor, unitString);
		ent.printReport(file, logTime);

		// If running in real time mode, empty the file buffer after each entity is logged
		if (!InputAgent.getBatch() && Simulation.isRealTime())
			file.flush();

		// Send the entity to the next element in the chain
		this.sendToNextComponent(ent);
	}

	@Override
	public void doEnd() {
		super.doEnd();
		file.flush();
	}

	@Output(name = "LogTime",
	 description = "The simulation time at which the last entity was logged.",
	    unitType = TimeUnit.class)
	public double getLogTime(double simTime) {
		return logTime;
	}
}
