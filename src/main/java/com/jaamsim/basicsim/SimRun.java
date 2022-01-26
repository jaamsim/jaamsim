/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2021-2022 JaamSim Software Inc.
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

import java.util.ArrayList;

import com.jaamsim.ui.LogBox;

/**
 * An individual run for a simulation model.
 * @author Harry King
 *
 */
public class SimRun implements RunListener {

	private final int scenarioNumber;     // scenario number
	private final int replicationNumber;  // replication number
	private JaamSimModel simModel;        // simulation model to be executed
	private final RunListener listener;   // notifies the Scenario that the run has ended
	private ArrayList<Double> runOutputValues;
	private ArrayList<String> runOutputStrings;
	private ArrayList<String> runParameterStrings;
	private boolean errorFlag;

	/**
	 * Constructs a SimRun object for the given scenario and replications numbers.
	 * @param scene - scenario number for the run
	 * @param rep - replication number for the run
	 * @param l - listens for the end of the run
	 */
	public SimRun(int scene, int rep, RunListener l) {
		scenarioNumber = scene;
		replicationNumber = rep;
		listener = l;
		runOutputValues = new ArrayList<>();
		runOutputStrings = new ArrayList<>();
		runParameterStrings = new ArrayList<>();
	}

	/**
	 * Sets the JaamSimModel to be executed for the run.
	 * The model must be configured already and may have been used for a previous run.
	 * @param model - pre-configured simulation model
	 */
	public void setJaamSimModel(JaamSimModel model) {
		simModel = model;
	}

	public JaamSimModel getJaamSimModel() {
		return simModel;
	}

	public int getScenarioNumber() {
		return scenarioNumber;
	}

	public int getReplicationNumber() {
		return replicationNumber;
	}

	public boolean isError() {
		return errorFlag;
	}

	/**
	 * Starts the simulation run on a new thread.
	 */
	public void start(double pauseTime) {

		// Reset the scenario and replication numbers
		simModel.setScenarioNumber(scenarioNumber);
		simModel.setReplicationNumber(replicationNumber);

		// Clear the model prior to the next run
		simModel.getEventManager().clear();
		simModel.killGeneratedEntities();

		// Start the run
		simModel.setRunListener(this);
		simModel.start(pauseTime);
	}

	@Override
	public void runEnded(SimRun run) {

		// Save the RunOutputList values for the run
		double simTime = simModel.getSimTime();
		runOutputValues = simModel.getSimulation().getRunOutputValues(simTime);
		runOutputStrings = simModel.getSimulation().getRunOutputStrings(simTime);
		runParameterStrings = simModel.getSimulation().getRunParameterStrings(simTime);

		// Notify the listener
		listener.runEnded(this);
	}

	@Override
	public void handleError(Throwable t) {
		String msg = String.format("Runtime error in replication %s of scenario %s:",
				replicationNumber, scenarioNumber);
		LogBox.logLine(msg);
		System.err.println(msg);
		LogBox.logException(t);
		double simTime = simModel.getSimTime();
		runOutputStrings = new ArrayList<>(1);
		runOutputStrings.add(t.getMessage());
		runParameterStrings = simModel.getSimulation().getRunParameterStrings(simTime);
		errorFlag = true;
		listener.runEnded(this);
	}

	public ArrayList<Double> getRunOutputValues() {
		return runOutputValues;
	}

	public ArrayList<String> getRunOutputStrings() {
		return runOutputStrings;
	}

	public ArrayList<String> getRunParameterStrings() {
		return runParameterStrings;
	}

	public double getProgress() {
		Simulation simulation = simModel.getSimulation();
		if (simulation == null)
			return 0.0d;
		double simTime = simModel.getSimTime();
		return simulation.getProgress(simTime);
	}

}
