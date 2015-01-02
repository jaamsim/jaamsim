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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Output;
import com.jaamsim.units.TimeUnit;

public class EntityLogger extends LinkedComponent {
	private FileEntity file;
	private double logTime;

	{
		stateAssignment.setHidden(true);
		testEntity.setHidden(true);
	}

	public EntityLogger() {}

	@Override
	public void earlyInit() {
		super.earlyInit();

		logTime = 0.0d;

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
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);

		// Log the entity's outputs
		file.format("%n");
		logTime = this.getSimTime();
		ent.printReport(file, logTime);

		// Empty the output buffer
		file.flush();

		// Send the entity to the next element in the chain
		this.sendToNextComponent(ent);
	}

	@Output(name = "LogTime",
	 description = "The simulation time at which the last entity was logged.",
	    unitType = TimeUnit.class)
	public double getLogTime(double simTime) {
		return logTime;
	}
}
