/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2021-2025 JaamSim Software Inc.
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

import java.io.File;
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
public class RunManager {

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
		Simulation simulation = simModel.getSimulation();

		// Open the main report
		if (simulation.getPrintReport())
			reportFile = getReportFile();

		// Start a new simulation run on each thread
		simModelList.clear();
		scenarioList.clear();
		for (int i = 0; i < getNumberOfThreads(); i++) {
			//System.out.format("Thread %s:%n", i);

			// Create a JaamSimModel for each thread
			JaamSimModel sm = simModel;
			if (i > 0) {
				try {
					String nextName = String.format("%s(%s)", simModel.getName(), simModelList.size() + 1);
					sm = new JaamSimModel(simModel, nextName);
					//System.out.format("JaamSimModel %s created%n", sm);
				}
				catch (Exception e) {
					pause();
					GUIFrame.invokeErrorDialog("Runtime Error",
							"The following runtime error has occurred while starting the model "
							+ "on multiple threads:",
							e.getMessage(),
							"More information about the error can be found in the Log Viewer.");
					Log.logException(e);
					return;
				}
			}
			//System.out.format("hasRunsToStart=%s%n", hasRunsToStart());
			if (!hasRunsToStart())
				return;
			synchronized (simModelList) {
				simModelList.add(sm);
				//System.out.format("simModelList=%s%n", simModelList);
			}

			// Start the next simulation run for the present scenario
			startNextRun(sm);
		}
	}

	public synchronized void pause() {
		for (JaamSimModel sm : simModelList) {
			sm.pause();
		}
	}

	public synchronized void resume() {
		for (JaamSimModel sm : simModelList) {
			sm.resume();
		}
	}

	public synchronized void reset() {
		simModelList.remove(simModel);
		close();
		simModelList.clear();
		scenarioList.clear();

		simModel.setScenarioNumber(getStartingScenarioNumber());
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

	public boolean hasRunsToStart() {
		synchronized (scenarioList) {
			return scenarioList.size() < getNumberOfScenarios()
					|| scenarioList.get(scenarioList.size() - 1).hasRunsToStart();
		}
	}

	public void runEnded(SimRun run) {
		Simulation simulation = simModel.getSimulation();
		if (RunProgressBox.hasInstance())
			GUIFrame.updateUI();

		synchronized (simModel) {
			// Print the output report
			if (reportFile != null)
				InputAgent.printReport(run.getJaamSimModel(), EventManager.simSeconds(), reportFile);

			// Is the scenario finished?
			Scenario scene = run.getScenario();
			if (scene.isFinished()) {

				// Print the results
				int numOuts = simulation.getRunOutputListSize();
				if (numOuts > 0) {
					outStream = getOutStream();
					if (outStream != null) {
						int replications = scene.getRunsCompleted().size();
						boolean labels = simulation.getPrintRunLabels();
						boolean reps = simulation.getPrintReplications();
						boolean bool = simulation.getPrintConfidenceIntervals();

						// Print the column headers
						if (scene.getScenarioNumber() == getStartingScenarioNumber())
							InputAgent.printRunOutputHeaders(simModel, labels, reps, bool, outStream);

						// Print the output lines for the scenario
						InputAgent.printScenarioOutputs(scene, labels, reps, bool, outStream);

						// Print a blank line after the scenario if the replications are shown
						if (reps && replications > 1 &&
								scene.getScenarioNumber() < getEndingScenarioNumber()) {
							outStream.println();
						}
					}
				}

				// Exit if this is the last scenario
				if (scene.getScenarioNumber() == getEndingScenarioNumber()) {
					if (outStream != null) {
						outStream.close();
						outStream = null;
					}
					if (reportFile != null) {
						reportFile.close();
						reportFile = null;
					}
					// Close warning/error trace file
					Log.logLine("Made it to do end at");
					simModel.closeLogFile();

					// Always terminate the run when in batch mode
					if (simModel.isBatchRun() || simulation.getExitAtStop()) {
						GUIFrame.shutdown(0);
					}

					// Are there any runs with errors
					ArrayList<SimRun> errorRuns = getErrorRuns();
					if (GUIFrame.getInstance() != null && !errorRuns.isEmpty()) {
						StringBuilder sb = new StringBuilder();
						for (SimRun r : errorRuns) {
							sb.append(String.format("replication %s of scenario %s%n",
									r.getReplicationNumber(), r.getScenario().getScenarioNumber()));
						}
						GUIFrame.invokeErrorDialog("Runtime Error",
								"Runtime errors occured in the following simulation runs:",
								sb.toString(),
								"More information can be found in the Log Viewer.");
					}
					return;
				}
			}
		}

		// Start the next run
		JaamSimModel sm = run.getJaamSimModel();
		startNextRun(sm);
	}

	private void startNextRun(JaamSimModel sm) {
		synchronized (scenarioList) {
			Simulation simulation = simModel.getSimulation();

			// Set the present scenario
			Scenario presentScenario = null;
			if (!scenarioList.isEmpty())
				presentScenario = scenarioList.get(scenarioList.size() - 1);

			// Start a new scenario if required
			if (presentScenario == null || !presentScenario.hasRunsToStart()) {
				if (scenarioList.size() >= getNumberOfScenarios())
					return;
				int numOuts = simulation.getRunOutputListSize();
				int scenarioNumber = scenarioList.size() + getStartingScenarioNumber();
				int numberOfReplications = getNumberOfReplications();
				presentScenario = new Scenario(numOuts, scenarioNumber, numberOfReplications, this);
				scenarioList.add(presentScenario);
				//System.out.format("Scenario %s started%n", presentScenario.getScenarioNumber());
			}

			// Start the next simulation run for the present scenario
			if (presentScenario.hasRunsToStart()) {
				presentScenario.startNextRun(sm);
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

	public FileEntity getReportFile() {
		if (reportFile == null) {
			String fileName = simModel.getReportFileName(".rep");
			if (fileName == null)
				throw new ErrorException("Cannot create the report file");
			File f = new File(fileName);
			if (f.exists() && !f.delete())
				throw new ErrorException("Cannot delete the existing report file %s", f);
			reportFile = new FileEntity(simModel, f);
		}
		return reportFile;
	}

	public double getProgress() {
		synchronized (scenarioList) {
			double ret = 0.0d;
			for (Scenario scene : scenarioList) {
				ret += scene.getProgress();
			}
			return ret / getNumberOfScenarios();
		}
	}

	public boolean isRunning() {
		synchronized (simModelList) {
			for (JaamSimModel sm : simModelList) {
				if (sm.isRunning()) {
					return true;
				}
			}
		}
		return false;
	}

	public int getStartingScenarioNumber() {
		return simModel.getSimulation().getStartingScenarioNumber();
	}

	public int getEndingScenarioNumber() {
		return simModel.getSimulation().getEndingScenarioNumber();
	}

	public int getNumberOfScenarios() {
		return simModel.getSimulation().getNumberOfScenarios();
	}

	public int getNumberOfReplications() {
		return simModel.getSimulation().getNumberOfReplications();
	}

	public int getNumberOfRuns() {
		return simModel.getSimulation().getNumberOfRuns();
	}

	public int getNumberOfThreads() {
		return simModel.getSimulation().getNumberOfThreads();
	}

	public ArrayList<SimRun> getErrorRuns() {
		synchronized (scenarioList) {
			ArrayList<SimRun> ret = new ArrayList<>();
			for (Scenario scene : scenarioList) {
				ret.addAll(scene.getErrorRuns());
			}
			return ret;
		}
	}

}
