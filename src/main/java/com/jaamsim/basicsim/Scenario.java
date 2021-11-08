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

/**
 * A set of simulation runs that are replications of a given model.
 * @author Harry King
 *
 */
public class Scenario implements RunListener {

	private final int scenarioNumber;
	private final int replications;  // number of replications to be performed
	private final RunListener listener;  // notifies the RunManager that the run has ended

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

	public int getNumberOfReplications() {
		return replications;
	}

	public ArrayList<SimRun> getRunsCompleted() {
		synchronized (this) {
			return runsCompleted;
		}
	}

	public void recordRun(SimRun run) {
		if (run.getRunOutputValues().size() != runStatistics.size())
			throw new ErrorException("List sizes do not match");

		for (int i = 0; i < run.getRunOutputValues().size(); i++) {
			double val = run.getRunOutputValues().get(i);
			if (Double.isNaN(val))
				continue;
			runStatistics.get(i).addValue(val);
		}
	}

	public String[] getParameters() {
		if (runsCompleted.isEmpty())
			return new String[0];

		// Start with the parameters for the first run
		int n = runsCompleted.get(0).getRunParameterStrings().size();
		String[] ret = runsCompleted.get(0).getRunParameterStrings().toArray(new String[n]);

		// Ensure that each run has the same parameter values
		for (SimRun run : runsCompleted) {
			for (int i = 0; i < run.getRunParameterStrings().size(); i++) {
				if (!ret[i].equals(run.getRunParameterStrings().get(i))) {
					ret[i] = "*";
				}
			}
		}
		return ret;
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

	public boolean hasRunsToStart() {
		synchronized (this) {
			return !runsToStart.isEmpty();
		}
	}

	public void startNextRun(JaamSimModel simModel, double pauseTime) {
		synchronized (this) {
			if (runsToStart.isEmpty())
				return;
			SimRun run = runsToStart.remove(0);
			runsInProgress.add(run);
			run.setJaamSimModel(simModel);
			run.start(pauseTime);
		}
	}

	public boolean isFinished() {
		synchronized (this) {
			return runsToStart.isEmpty() && runsInProgress.isEmpty();
		}
	}

	@Override
	public void runEnded(SimRun run) {
		recordRun(run);
		synchronized (this) {
			runsInProgress.remove(run);
			runsCompleted.add(run);
		}
		listener.runEnded(run);
	}

	public double getProgress() {
		synchronized (this) {
			double ret = runsCompleted.size();
			for (SimRun run : runsInProgress) {
				ret += run.getProgress();
			}
			return ret / replications;
		}
	}

}
