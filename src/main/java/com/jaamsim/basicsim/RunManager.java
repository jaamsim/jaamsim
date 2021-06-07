/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2021 JaamSim Software Inc.
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

import java.io.PrintStream;

import com.jaamsim.events.EventManager;
import com.jaamsim.input.InputAgent;

/**
 * Controls the execution of one or more runs of a given simulation model.
 * @author Harry King
 *
 */
public class RunManager implements RunListener {

	private final JaamSimModel simModel;

	public RunManager(JaamSimModel sm) {
		simModel = sm;
	}

	public JaamSimModel getJaamSimModel() {
		return simModel;
	}

	public void start(double pauseTime) {
		simModel.start(pauseTime);
	}

	public void pause() {
		simModel.pause();
	}

	public void resume(double pauseTime) {
		simModel.resume(pauseTime);
	}

	public void reset() {
		simModel.reset();
	}

	public void close() {
		simModel.closeLogFile();
		simModel.pause();
		simModel.close();
		simModel.clear();
	}

	@Override
	public void runEnded() {

		// Print the output report
		if (simModel.getSimulation().getPrintReport())
			InputAgent.printReport(simModel, EventManager.simSeconds());

		// Print the selected outputs
		if (simModel.getSimulation().getRunOutputList().getValue() != null) {
			PrintStream outStream = simModel.getOutStream();
			if (simModel.isFirstRun()) {
				InputAgent.printRunOutputHeaders(simModel, outStream);
			}
			InputAgent.printRunOutputs(simModel, outStream, EventManager.simSeconds());
		}
	}

}
