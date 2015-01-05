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
import com.jaamsim.input.InputAgent;
import com.jaamsim.ui.GUIFrame;

class EndModelTarget extends ProcessTarget {
	EndModelTarget() {}

	@Override
	public String getDescription() {
		return "SimulationEnd";
	}

	@Override
	public void process() {
		EventManager.current().pause();
		for (int i = 0; i < Entity.getAll().size(); i++) {
			Entity.getAll().get(i).doEnd();
		}

		InputAgent.logMessage("Made it to do end at");
		// close warning/error trace file
		InputAgent.closeLogFile();

		if (Simulation.getExitAtStop() || InputAgent.getBatch())
			GUIFrame.shutdown(0);

		EventManager.current().pause();
	}
}
