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

import com.jaamsim.Statistics.SampleStatistics;
import com.jaamsim.StringProviders.StringProvider;

/**
 * A set of simulation runs that are replications of a given model.
 * @author Harry King
 *
 */
public class Scenario implements RunListener {

	private final int scenarioNumber;
	private final int replications;  // number of replications to be performed
	private final RunListener listener;

	private final ArrayList<SimRun> runsToStart;
	private final ArrayList<SimRun> runsInProgress;
	private final ArrayList<SimRun> runsCompleted;

	private final ArrayList<SampleStatistics> runStatistics;

	public Scenario(int numOuts, int scene, int numReps, RunListener l) {
		scenarioNumber = scene;
		replications = numReps;
		listener = l;

		runsToStart = new ArrayList<>(replications);
		runsInProgress = new ArrayList<>(replications);
		runsCompleted = new ArrayList<>(replications);
		for (int i = 1; i <= replications; i++) {
			runsToStart.add(new SimRun(scenarioNumber, i, this));
		}

		runStatistics = new ArrayList<>(numOuts);
		for (int i = 0; i < numOuts; i++) {
			runStatistics.add(new SampleStatistics());
		}
	}

	public int getScenarioNumber() {
		return scenarioNumber;
	}

	public void recordRun(JaamSimModel simModel) {
		ArrayList<StringProvider> spList = simModel.getSimulation().getRunOutputList().getValue();
		if (spList.size() != runStatistics.size())
			throw new ErrorException("List sizes do not match");

		double simTime = simModel.getSimTime();
		for (int i = 0; i < runStatistics.size(); i++) {
			double val = spList.get(i).getNextValue(simTime);
			if (Double.isNaN(val))
				continue;
			runStatistics.get(i).addValue(val);
		}
	}

	public double[] getMeanValues() {
		double[] ret = new double[runStatistics.size()];
		for (int i = 0; i < runStatistics.size(); i++) {
			ret[i] = runStatistics.get(i).getMean();
		}
		return ret;
	}

	public double[] getConfidenceIntervals() {
		double[] ret = new double[runStatistics.size()];
		for (int i = 0; i < runStatistics.size(); i++) {
			ret[i] = runStatistics.get(i).getConfidenceInterval95();
		}
		return ret;
	}

	public synchronized boolean hasRunsToStart() {
		return !runsToStart.isEmpty();
	}

	public synchronized void startNextRun(JaamSimModel simModel, double pauseTime) {
		if (runsToStart.isEmpty())
			return;
		SimRun run = runsToStart.remove(0);
		runsInProgress.add(run);
		run.setJaamSimModel(simModel);
		run.start(pauseTime);
	}

	public synchronized boolean isFinished() {
		return runsToStart.isEmpty() && runsInProgress.isEmpty();
	}

	@Override
	public synchronized void runEnded(SimRun run) {
		runsInProgress.remove(run);
		runsCompleted.add(run);
		listener.runEnded(run);
	}

}
