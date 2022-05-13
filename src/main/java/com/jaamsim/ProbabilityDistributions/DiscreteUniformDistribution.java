/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2022 JaamSim Software Inc.
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
package com.jaamsim.ProbabilityDistributions;

import com.jaamsim.rng.MRG1999a;

/**
 * Discrete Uniform Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 5th Edition", page 469.
 */
public class DiscreteUniformDistribution extends Distribution {

	private final MRG1999a rng = new MRG1999a();

	public DiscreteUniformDistribution() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected double getSample(double simTime) {
		int i = (int) minValueInput.getNextSample(simTime);
		int j = (int) maxValueInput.getNextSample(simTime);
		return getSample(i, j, rng);
	}

	@Override
	protected double getMean(double simTime) {
		int i = (int) minValueInput.getNextSample(simTime);
		int j = (int) maxValueInput.getNextSample(simTime);
		return getMean(i, j);
	}

	@Override
	protected double getStandardDev(double simTime) {
		int i = (int) minValueInput.getNextSample(simTime);
		int j = (int) maxValueInput.getNextSample(simTime);
		return getStandardDev(i, j);
	}

	public static int getSample(int i, double j, MRG1999a rng) {
		return (int) (i + rng.nextUniform() * (j - i + 1));
	}

	public static double getMean(int i, double j) {
		return 0.5d * (i + j);
	}

	public static double getStandardDev(int i, double j) {
		return Math.sqrt( (Math.pow(j - i + 1, 2) - 1) / 12.0d );
	}

}
