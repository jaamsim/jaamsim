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
package com.jaamsim.basicsim;

import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.ui.GUIFrame;

public class PauseModelTarget extends ProcessTarget {

	public PauseModelTarget() {}

	@Override
	public String getDescription() {
		return "SimulationPaused";
	}

	@Override
	public void process() {

		// If specified, terminate the simulation run
		if (Simulation.getExitAtPauseCondition()) {
			Simulation.end();
			GUIFrame.shutdown(0);
		}

		// Pause the simulation run
		EventManager.current().pause();

		// When the run is resumed, continue to check the pause condition
		Simulation.getInstance().doPauseCondition();
	}

}
