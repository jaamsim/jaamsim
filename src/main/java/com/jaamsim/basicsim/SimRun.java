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

import java.util.ArrayList;

/**
 * An individual run for a simulation model.
 * @author Harry King
 *
 */
public class SimRun implements RunListener {

	private final int scenarioNumber;     // scenario number
	private final int replicationNumber;  // replication number
	private JaamSimModel simModel;        // simulation model to be executed
	private final RunListener listener;   // listens for the end of the run
	private ArrayList<Double> runOutputValues;
	private ArrayList<String> runOutputStrings;

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

		// Notify the listener
		listener.runEnded(this);
	}

	public ArrayList<Double> getRunOutputValues() {
		return runOutputValues;
	}

	public ArrayList<String> getRunOutputStrings() {
		return runOutputStrings;
	}

	public double getProgress() {
		double simTime = simModel.getSimTime();
		return simModel.getSimulation().getProgress(simTime);
	}

}
