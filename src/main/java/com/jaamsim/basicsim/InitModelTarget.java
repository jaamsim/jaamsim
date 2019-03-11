/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019 JaamSim Software Inc.
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
package com.jaamsim.basicsim;

import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;

public class InitModelTarget extends ProcessTarget {

	final Simulation simulation;

	public InitModelTarget(Simulation sim) {
		simulation = sim;
	}

	@Override
	public String getDescription() {
		return "SimulationInit";
	}

	@Override
	public void process() {
		JaamSimModel simModel = simulation.getJaamSimModel();

		// Initialise each entity
		simModel.earlyInit();
		simModel.lateInit();

		// Start each entity
		double startTime = simulation.getStartTime();
		for (Entity each : simModel.getEntities()) {
			if (!each.isActive())
				continue;
			EventManager.scheduleSeconds(startTime, 0, true, new StartUpTarget(each), null);
		}

		// Schedule the initialisation period
		if (simulation.getInitializationTime() > 0.0) {
			double clearTime = startTime + simulation.getInitializationTime();
			EventManager.scheduleSeconds(clearTime, 5, false, new ClearStatisticsTarget(simulation), null);
		}

		// Schedule the end of the simulation run
		double endTime = simulation.getEndTime();
		EventManager.scheduleSeconds(endTime, 5, false, new EndModelTarget(simulation), null);

		// Start checking the pause condition
		simModel.doPauseCondition();
	}
}
