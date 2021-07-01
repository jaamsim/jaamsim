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
public class Scenario {

	private final ArrayList<SampleStatistics> runStatistics;

	public Scenario(int numOuts) {
		runStatistics = new ArrayList<>(numOuts);
		for (int i = 0; i < numOuts; i++) {
			runStatistics.add(new SampleStatistics());
		}
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

}
