/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
 * Uniform Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 448.
 */
public class UniformDistribution extends Distribution {
	private final MRG1999a rng = new MRG1999a();

	{
		minValueInput.setDefaultValue(0.0d);
		maxValueInput.setDefaultValue(1.0d);
	}

	public UniformDistribution() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected double getNextSample() {

		// Select the sample from a uniform distribution between the min and max values
		double min = this.getMinValue();
		double max = this.getMaxValue();
		return min + rng.nextUniform() * (max - min);
	}

	@Override
	protected double getMeanValue() {
		return ( 0.5 * ( this.getMinValue() + this.getMaxValue() ) );
	}

	@Override
	protected double getStandardDeviation() {
		return (  0.5 * ( this.getMaxValue() - this.getMinValue() ) / Math.sqrt(3.0) );
	}
}
