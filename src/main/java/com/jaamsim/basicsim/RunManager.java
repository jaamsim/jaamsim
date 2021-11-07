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
import com.jaamsim.ui.GUIFrame;
import com.jaamsim.ui.RunProgressBox;

/**
 * Controls the execution of one or more runs of a given simulation model.
 * @author Harry King
 *
 */
public class RunManager implements RunListener {

	private final JaamSimModel simModel;
	private PrintStream outStream;  // location where the custom outputs will be written
	private FileEntity reportFile;  // main output report

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

	public ArrayList<JaamSimModel> getSimModelList() {
		synchronized (simModelList) {
			return new ArrayList<>(simModelList);
		}
	}

	public synchronized void start() {
		start(Double.POSITIVE_INFINITY);
	}

	public synchronized void start(double pauseTime) {
		Simulation simulation = simModel.getSimulation();
		int numberOfThreads = simulation.getNumberOfThreads();

		// Open the main report
		if (simulation.getPrintReport())
			reportFile = simModel.getReportFile();

		// Start a new simulation run on each thread
		simModelList.clear();
		scenarioList.clear();
		for (int i = 0; i < numberOfThreads; i++) {

			// Create a JaamSimModel for each thread
			JaamSimModel sm = simModel;
			if (i > 0) {
				sm = new JaamSimModel(simModel);
				sm.setName(String.format("%s (%s)", simModel.getName(), simModelList.size() + 1));
			}
			synchronized (simModelList) {
				simModelList.add(sm);
			}

			// Start the next simulation run for the present scenario
			startNextRun(sm, pauseTime);
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

		int scenarioNumber = simModel.getSimulation().getStartingScenarioNumber();
		simModel.setScenarioNumber(scenarioNumber);
		simModel.setReplicationNumber(1);
		simModel.reset();
	}

	public synchronized void close() {
		if (outStream != null) {
			outStream.close();
			outStream = null;
		}
		if (reportFile != null) {
			reportFile.close();
			reportFile = null;
		}
		for (JaamSimModel sm : simModelList) {
			sm.closeLogFile();
			sm.pause();
			sm.close();
			sm.clear();
		}
	}

	public Scenario getScenario(int scenarioNumber) {
		synchronized (scenarioList) {
			int i = scenarioNumber - simModel.getSimulation().getStartingScenarioNumber();
			return scenarioList.get(i);
		}
	}

	@Override
	public void runEnded(SimRun run) {
		Simulation simulation = simModel.getSimulation();
		if (RunProgressBox.hasInstance())
			GUIFrame.updateUI();

		synchronized (simModel) {
			// Print the output report
			if (simulation.getPrintReport())
				InputAgent.printReport(run.getJaamSimModel(), EventManager.simSeconds());

			// Is the scenario finished?
			Scenario scene = getScenario(run.getScenarioNumber());
			if (scene.isFinished()) {

				// Print the results
				int numOuts = simulation.getRunOutputListSize();
				if (numOuts > 0) {
					outStream = getOutStream();
					if (outStream != null) {
						if (run.getScenarioNumber() == simulation.getStartingScenarioNumber())
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
					if (outStream != null) {
						outStream.close();
						outStream = null;
					}
					if (reportFile != null) {
						reportFile.close();
						reportFile = null;
					}
					simModel.end();
					return;
				}
			}
		}

		// Start the next run
		JaamSimModel sm = run.getJaamSimModel();
		double pauseTime = simModel.getSimulation().getPauseTime();
		startNextRun(sm, pauseTime);
	}

	private void startNextRun(JaamSimModel sm, double pauseTime) {
		synchronized (scenarioList) {
			Simulation simulation = simModel.getSimulation();

			// Set the present scenario
			Scenario presentScenario = null;
			if (!scenarioList.isEmpty())
				presentScenario = scenarioList.get(scenarioList.size() - 1);

			// Start a new scenario if required
			if (presentScenario == null || !presentScenario.hasRunsToStart()) {
				if (scenarioList.size() >= simulation.getNumberOfScenarios())
					return;
				int numOuts = simulation.getRunOutputListSize();
				int scenarioNumber = scenarioList.size() + simulation.getStartingScenarioNumber();
				int numberOfReplications = simulation.getNumberOfReplications();
				presentScenario = new Scenario(numOuts, scenarioNumber, numberOfReplications, this);
				scenarioList.add(presentScenario);
			}

			// Start the next simulation run for the present scenario
			if (presentScenario.hasRunsToStart()) {
				presentScenario.startNextRun(sm, pauseTime);
				if (sm == simModel && GUIFrame.getInstance() != null) {
					GUIFrame.getInstance().initSpeedUp(0.0d);
				}
			}
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

	public double getProgress() {
		synchronized (scenarioList) {
			double ret = 0.0d;
			for (Scenario scene : scenarioList) {
				ret += scene.getProgress();
			}
			return ret / simModel.getSimulation().getNumberOfScenarios();
		}
	}

}
