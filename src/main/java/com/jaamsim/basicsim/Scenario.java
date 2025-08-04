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

import java.util.ArrayList;

import com.jaamsim.Statistics.SampleStatistics;

/**
 * A set of simulation runs that are replications of a given model.
 * @author Harry King
 *
 */
public class Scenario {

	private final int scenarioNumber;
	private final int replications;  // number of replications to be performed
	private final RunManager runmanager;  // notifies the RunManager that the run has ended

	private final ArrayList<SimRun> runsToStart;
	private final ArrayList<SimRun> runsInProgress;
	private final ArrayList<SimRun> runsCompleted;

	private final ArrayList<SampleStatistics> runStatistics;

	public Scenario(int numOuts, int scene, int numReps, RunManager r) {
		scenarioNumber = scene;
		replications = numReps;
		runmanager = r;

		runsToStart = new ArrayList<>(replications);
		runsInProgress = new ArrayList<>(replications);
		runsCompleted = new ArrayList<>(replications);
		for (int i = 1; i <= replications; i++) {
			runsToStart.add(new SimRun(i, this));
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

	private void recordRun(SimRun run) {
		if (run.isError())
			return;

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

	public ArrayList<SampleStatistics> getRunStatistics() {
		return runStatistics;
	}

	public boolean hasRunsToStart() {
		synchronized (this) {
			return !runsToStart.isEmpty();
		}
	}

	public void startNextRun(JaamSimModel simModel) {
		synchronized (this) {
			if (runsToStart.isEmpty())
				return;
			SimRun run = runsToStart.remove(0);
			runsInProgress.add(run);
			run.start(simModel);
			//System.out.format("Replication %s of Scenario %s started%n",
			//		run.getReplicationNumber(), run.getScenarioNumber());
		}
	}

	public boolean isFinished() {
		synchronized (this) {
			return runsToStart.isEmpty() && runsInProgress.isEmpty();
		}
	}

	public void runEnded(SimRun run) {
		synchronized (this) {
			recordRun(run);
			runsInProgress.remove(run);
			runsCompleted.add(run);
		}
		runmanager.runEnded(run);
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

	public ArrayList<SimRun> getErrorRuns() {
		synchronized (this) {
			ArrayList<SimRun> ret = new ArrayList<>();
			for (SimRun run : runsCompleted) {
				if (run.isError()) {
					ret.add(run);
				}
			}
			return ret;
		}
	}

}
