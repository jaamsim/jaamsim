/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2024 JaamSim Software Inc.
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

	final JaamSimModel simModel;

	public InitModelTarget(JaamSimModel model) {
		simModel = model;
	}

	@Override
	public String getDescription() {
		return "Simulation.init";
	}

	@Override
	public void process() {
		//System.out.format("%ninit%n");
		Simulation simulation = simModel.getSimulation();

		simModel.hasStarted.set(true);
		// Initialise each entity
		simModel.earlyInit();
		simModel.lateInit();

		// Start each entity
		simModel.startUp();

		// Schedule the initialisation period
		if (simulation.getInitializationTime() > 0.0) {
			double clearTime = simulation.getStartTime() + simulation.getInitializationTime();
			EventManager.scheduleSeconds(clearTime, 5, false, new ClearStatisticsTarget(simModel), null);
		}

		// Schedule the end of the simulation run
		double endTime = simulation.getEndTime();
		EventManager.scheduleSeconds(endTime, 5, false, new EndModelTarget(simModel), null);

		// Start checking the pause condition
		simModel.doPauseCondition();
	}
}
