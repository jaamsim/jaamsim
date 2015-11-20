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
package com.jaamsim.basicsim;

import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;

public class InitModelTarget extends ProcessTarget {
	public InitModelTarget() {}

	@Override
	public String getDescription() {
		return "SimulationInit";
	}

	@Override
	public void process() {

		// Initialise each entity
		for (int i = 0; i < Entity.getAll().size(); i++) {
			Entity.getAll().get(i).earlyInit();
		}

		// Initialise each entity a second time
		for (int i = 0; i < Entity.getAll().size(); i++) {
			Entity.getAll().get(i).lateInit();
		}

		// Start each entity
		double startTime = Simulation.getStartTime();
		for (int i = Entity.getAll().size() - 1; i >= 0; i--) {
			EventManager.scheduleSeconds(startTime, 0, false, new StartUpTarget(Entity.getAll().get(i)), null);
		}

		// Schedule the initialisation period
		if (Simulation.getInitializationTime() > 0.0) {
			double clearTime = startTime + Simulation.getInitializationTime();
			EventManager.scheduleSeconds(clearTime, 5, false, new ClearStatisticsTarget(), null);
		}

		// Schedule the end of the simulation run
		double endTime = Simulation.getEndTime();
		EventManager.scheduleSeconds(endTime, 5, false, new EndModelTarget(), null);

		// Start checking the pause condition
		Simulation.getInstance().doPauseCondition();
	}
}
