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
import com.sandwell.JavaSimulation.Simulation;

public class InitModelTarget extends ProcessTarget {
	public InitModelTarget() {}

	@Override
	public String getDescription() {
		return "SimulationInit";
	}

	@Override
	public void process() {
		for (int i = 0; i < Entity.getAll().size(); i++) {
			Entity.getAll().get(i).earlyInit();
		}

		long startTick = Entity.calculateDelayLength(Simulation.getStartHours());
		for (int i = Entity.getAll().size() - 1; i >= 0; i--) {
			EventManager.scheduleTicks(startTick, 0, false, new StartUpTarget(Entity.getAll().get(i)), null);
		}

		long endTick = Entity.calculateDelayLength(Simulation.getEndHours());
		EventManager.scheduleTicks(endTick, Entity.PRIO_DEFAULT, false, new EndModelTarget(), null);
	}
}
