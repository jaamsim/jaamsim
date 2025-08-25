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

/**
 * An individual run for a simulation model.
 * @author Harry King
 *
 */
public class SimRun implements RunListener {
	private final Scenario scen;
	private final int replicationNumber;
	private JaamSimModel simModel;        // simulation model to be executed
	private ArrayList<Double> runOutputValues;
	private ArrayList<String> runOutputStrings;
	private ArrayList<String> runParameterStrings;
	private String errorMessage;

	/**
	 * Constructs a SimRun object for the given scenario and replications numbers.
	 * @param scene - scenario number for the run
	 * @param rep - replication number for the run
	 * @param l - listens for the end of the run
	 */
	public SimRun(int rep, Scenario s) {
		replicationNumber = rep;
		scen = s;
	}

	public JaamSimModel getJaamSimModel() {
		return simModel;
	}

	public Scenario getScenario() {
		return scen;
	}

	public int getReplicationNumber() {
		return replicationNumber;
	}

	public boolean isError() {
		return errorMessage != null;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * Starts the simulation model run on a new thread. The model must be configured
	 * already and may have been used for a previous run.
	 *
	 * @param sm - pre-configured simulation model
	 */
	public void start(JaamSimModel sm) {
		simModel = sm;
		// Reset the scenario and replication numbers
		simModel.setScenarioNumber(scen.getScenarioNumber());
		simModel.setReplicationNumber(getReplicationNumber());

		// Start the run
		simModel.start(this);
	}

	@Override
	public void runEnded() {
		double simTime = simModel.getSimTime();
		runOutputValues = simModel.getSimulation().getRunOutputValues(simTime);
		runOutputStrings = simModel.getSimulation().getRunOutputStrings(simTime);
		runParameterStrings = simModel.getSimulation().getRunParameterStrings(simTime);
		scen.runEnded(this);
	}

	@Override
	public void handleError(Throwable t) {
		double simTime = simModel.getSimTime();
		runParameterStrings = simModel.getSimulation().getRunParameterStrings(simTime);
		errorMessage = t.getMessage();
		if (errorMessage == null)
			errorMessage = "";
		scen.runEnded(this);
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
