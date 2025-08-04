/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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

import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;

class PauseModelTarget extends ProcessTarget {
	final JaamSimModel simModel;
	final Conditional condition;

	PauseModelTarget(JaamSimModel model) {
		simModel = model;
		condition = new PauseModelCondition(model);
	}

	@Override
	public String getDescription() {
		return "Simulation.pause";
	}

	@Override
	public void process() {
		simModel.event_pause();
	}

	private class PauseModelCondition extends Conditional {
		final JaamSimModel simModel;

		PauseModelCondition(JaamSimModel model) {
			simModel = model;
		}

		@Override
		public boolean evaluate() {
			double simTime = EventManager.simSeconds();
			return simModel.getSimulation().isPauseConditionSatisfied(simTime);
		}
	}
}
