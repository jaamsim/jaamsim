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
import java.util.ArrayList;

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
	private Scenario presentScenario;

	private final ArrayList<JaamSimModel> simModelList;
	private final ArrayList<Scenario> scenarioList;

	public RunManager(JaamSimModel sm) {
		simModel = sm;
		simModelList = new ArrayList<>();
		scenarioList = new ArrayList<>();
	}

	public JaamSimModel getJaamSimModel() {
		return simModel;
	}

	public synchronized void start(double pauseTime) {
		Simulation simulation = simModel.getSimulation();
		int numOuts = simulation.getRunOutputListSize();
		int numberOfReplications = simulation.getNumberOfReplications();
		int numberOfThreads = simulation.getNumberOfThreads();

		// Start a new simulation run on each thread
		simModelList.clear();
		scenarioList.clear();
		scenarioNumber = simulation.getStartingScenarioNumber();
		while (simModelList.size() < numberOfThreads) {

			// Create a JaamSimModel for each thread
			JaamSimModel sm = simModel;
			if (simModelList.size() > 0) {
				sm = new JaamSimModel(simModel);
				sm.setName(String.format("%s (%s)", simModel.getName(), simModelList.size() + 1));
			}
			simModelList.add(sm);

			// Create a new Scenario when required
			if (presentScenario == null || !presentScenario.hasRunsToStart()) {
				if (presentScenario != null)
					scenarioNumber++;
				presentScenario = new Scenario(numOuts, scenarioNumber, numberOfReplications, this);
				scenarioList.add(presentScenario);
			}

			// Start the next simulation run for the present scenario
			presentScenario.startNextRun(sm, pauseTime);
		}
	}

	public synchronized void pause() {
		for (JaamSimModel sm : simModelList) {
			sm.pause();
		}
	}

	public synchronized void resume(double pauseTime) {
		for (JaamSimModel sm : simModelList) {
			sm.resume(pauseTime);
		}
	}

	public synchronized void reset() {
		simModelList.remove(simModel);
		close();
		simModelList.clear();
		scenarioList.clear();

		presentScenario = null;
		scenarioNumber = simModel.getSimulation().getStartingScenarioNumber();
		simModel.setScenarioNumber(scenarioNumber);
		simModel.setReplicationNumber(1);
		simModel.reset();
	}

	public synchronized void close() {
		if (outStream != null) {
			outStream.close();
			outStream = null;
		}
		for (JaamSimModel sm : simModelList) {
			sm.closeLogFile();
			sm.pause();
			sm.close();
			sm.clear();
		}
	}

	@Override
	public synchronized void runEnded(SimRun run) {
		Simulation simulation = simModel.getSimulation();

		// Print the output report
		if (simulation.getPrintReport())
			InputAgent.printReport(run.getJaamSimModel(), EventManager.simSeconds());

		// Is the scenario finished?
		int i = run.getScenarioNumber() - simulation.getStartingScenarioNumber();
		Scenario scene = scenarioList.get(i);
		if (scene.isFinished()) {

			// Print the results
			int numOuts = simulation.getRunOutputListSize();
			if (numOuts > 0) {
				outStream = getOutStream();
				if (outStream != null) {
					if (i == 0)
						InputAgent.printRunOutputHeaders(simModel, outStream);
					boolean labels = simulation.getPrintRunLabels();
					boolean reps = simulation.getPrintReplications();
					boolean bool = simulation.getPrintConfidenceIntervals();
					InputAgent.printScenarioOutputs(scene, labels, reps, bool, outStream);
					if (simulation.getPrintReplications()
							&& run.getScenarioNumber() < simulation.getEndingScenarioNumber()) {
						outStream.println();
					}
				}
			}

			// Exit if this is the last scenario
			if (run.getScenarioNumber() == simulation.getEndingScenarioNumber()) {
				simModel.end();
				if (outStream != null) {
					outStream.close();
					outStream = null;
				}
				return;
			}
		}

		// Start a new Scenario
		if (!presentScenario.hasRunsToStart()
				&& scenarioNumber < simulation.getEndingScenarioNumber()) {
			scenarioNumber++;
			int numOuts = simulation.getRunOutputListSize();
			int numberOfReplications = simulation.getNumberOfReplications();
			presentScenario = new Scenario(numOuts, scenarioNumber, numberOfReplications, this);
			scenarioList.add(presentScenario);
		}

		// Start the next run
		if (presentScenario.hasRunsToStart()) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					JaamSimModel sm = run.getJaamSimModel();
					double pauseTime = simModel.getSimulation().getPauseTime();
					presentScenario.startNextRun(sm, pauseTime);
				}
			}).start();
		}
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
