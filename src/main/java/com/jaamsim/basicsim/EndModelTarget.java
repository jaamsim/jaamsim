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

import com.jaamsim.events.ProcessTarget;

class EndModelTarget extends ProcessTarget {

	final JaamSimModel simModel;

	EndModelTarget(JaamSimModel model) {
		simModel = model;
	}

	@Override
	public String getDescription() {
		return "Simulation.endRun";
	}

	@Override
	public void process() {
		simModel.endRun();
	}
}
