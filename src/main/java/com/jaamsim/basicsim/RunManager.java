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

import java.io.FileNotFoundException;
import java.io.PrintStream;

import com.jaamsim.events.EventManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;

/**
 * Controls the execution of one or more runs of a given simulation model.
 * @author Harry King
 *
 */
public class RunManager implements RunListener {

	private final JaamSimModel simModel;
	private PrintStream outStream;  // location where the custom outputs will be written
	private int scenarioNumber;    // labels each scenario when multiple scenarios are being made
	private int replicationNumber;
	private Scenario presentScenario;

	public RunManager(JaamSimModel sm) {
		simModel = sm;
	}

	public JaamSimModel getJaamSimModel() {
		return simModel;
	}

	public void start(double pauseTime) {
		scenarioNumber = simModel.getSimulation().getStartingScenarioNumber();
		replicationNumber = 1;
		simModel.setScenarioNumber(scenarioNumber);
		simModel.setReplicationNumber(replicationNumber);
		simModel.start(pauseTime);
	}

	public void pause() {
		simModel.pause();
	}

	public void resume(double pauseTime) {
		simModel.resume(pauseTime);
	}

	public void reset() {
		if (outStream != null) {
			outStream.close();
			outStream = null;
		}
		scenarioNumber = simModel.getSimulation().getStartingScenarioNumber();
		replicationNumber = 1;
		simModel.setScenarioNumber(scenarioNumber);
		simModel.setReplicationNumber(replicationNumber);
		simModel.reset();
	}

	public void close() {
		if (outStream != null) {
			outStream.close();
			outStream = null;
		}
		simModel.closeLogFile();
		simModel.pause();
		simModel.close();
		simModel.clear();
	}

	@Override
	public void runEnded(SimRun run) {
		Simulation simulation = simModel.getSimulation();

		// Print the output report
		if (simulation.getPrintReport())
			InputAgent.printReport(simModel, EventManager.simSeconds());

		// Print the selected outputs
		if (simulation.getRunOutputList().getValue() != null) {
			outStream = getOutStream();

			// Column headings
			if (simModel.isFirstRun()) {
				InputAgent.printRunOutputHeaders(simModel, outStream);
			}

			// Print the replication's outputs
			int numberOfReplications = simulation.getNumberOfReplications();
			if (simulation.getPrintReplications() || numberOfReplications == 1)
				InputAgent.printRunOutputs(simModel, outStream, EventManager.simSeconds());

			if (numberOfReplications > 1) {

				// Start a new Scenario
				if (replicationNumber == 1) {
					int numOuts = simulation.getRunOutputList().getListSize();
					presentScenario = new Scenario(numOuts, scenarioNumber);
				}

				// Record the replication's outputs
				presentScenario.recordRun(simModel);

				// Print the scenario's outputs
				if (replicationNumber == numberOfReplications) {
					boolean labels = simulation.getPrintRunLabels();
					boolean reps = simulation.getPrintReplications();
					boolean bool = simulation.getPrintConfidenceIntervals();
					InputAgent.printScenarioOutputs(presentScenario, labels, reps, bool, outStream);
					if (simulation.getPrintReplications() && !simModel.isLastRun()) {
						outStream.println();
					}
				}
			}
		}

		// Close the print stream for the selected outputs
		if (simModel.isLastRun()) {
			simModel.end();
			if (outStream != null) {
				outStream.close();
				outStream = null;
			}
			return;
		}

		// Clear the model prior to the next run
		simModel.getEventManager().clear();
		simModel.killGeneratedEntities();

		// Increment the run number
		if (replicationNumber < simModel.getSimulation().getNumberOfReplications()) {
			replicationNumber++;
		}
		else {
			replicationNumber = 1;
			scenarioNumber++;
		}
		simModel.setScenarioNumber(scenarioNumber);
		simModel.setReplicationNumber(replicationNumber);

		// Start the next run
		new Thread(new Runnable() {
			@Override
			public void run() {
				double pauseTime = simModel.getSimulation().getPauseTime();
				simModel.startRun(pauseTime);
			}
		}).start();
	}

	public PrintStream getOutStream() {
		if (outStream == null) {

			// Select either standard out or a file for the outputs
			outStream = System.out;
			if (!simModel.isScriptMode()) {
				String fileName = simModel.getReportFileName(".dat");
				if (fileName == null)
					throw new ErrorException("Cannot create the run output file");
				try {
					outStream = new PrintStream(fileName);
				}
				catch (FileNotFoundException e) {
					throw new InputErrorException(
							"FileNotFoundException thrown trying to open PrintStream: " + e );
				}
				catch (SecurityException e) {
					throw new InputErrorException(
							"SecurityException thrown trying to open PrintStream: " + e );
				}
			}
		}
		return outStream;
	}

}
