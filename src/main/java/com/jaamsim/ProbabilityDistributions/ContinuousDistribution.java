/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2022 JaamSim Software Inc.
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

import java.util.Arrays;

import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * ContinuousDistribution is a user-defined probability distribution that selects an output value based on an ordered list
 * of values and cumulative probabilities.  The inputs specify a continuous cumulative probability distribution by
 * linearly interpolating between the given values and cumulative probabilities.
 * @author Harry King
 *
 */
public class ContinuousDistribution extends Distribution {

	@Keyword(description = "The list of values for the user-defined cumulative probability distribution.",
	         exampleList = {"2.0  4.3  8.9"})
	private final ValueListInput valueListInput;

	@Keyword(description = "The list of cumulative probabilities corresponding to the values in the ValueList.  " +
			"The cumulative probabilities must be given in increasing order.  The first value must be exactly 0.0.  " +
			"The last value must be exactly 1.0.",
	         exampleList = {"0.0  0.6  1.0"})
	private final CumulativeProbInput cumulativeProbabilityListInput;

	private final MRG1999a rng = new MRG1999a();

	{
		valueListInput = new ValueListInput("ValueList", KEY_INPUTS, null);
		valueListInput.setUnitType(UserSpecifiedUnit.class);
		valueListInput.setRequired(true);
		valueListInput.setMonotonic( 1 );
		this.addInput( valueListInput);

		cumulativeProbabilityListInput = new CumulativeProbInput("CumulativeProbabilityList", KEY_INPUTS, null);
		cumulativeProbabilityListInput.setRequired(true);
		this.addInput(cumulativeProbabilityListInput);
	}

	public ContinuousDistribution() {}

	@Override
	public void validate() {
		super.validate();

		// The number of entries in the ValueList and CumulativeProbabilityList inputs must match
		if( cumulativeProbabilityListInput.getValue().size() != valueListInput.getValue().size() ) {
			throw new InputErrorException( "The number of entries for CumulativeProbabilityList and ValueList must be equal" );
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected void setUnitType(Class<? extends Unit> specified) {
		super.setUnitType(specified);
		valueListInput.setUnitType(specified);
	}

	@Override
	protected double getSample(double simTime) {
		double[] values = valueListInput.getValue().toArray();
		double[] cumProbs = cumulativeProbabilityListInput.getValue().toArray();
		return getSample(values, cumProbs, rng);
	}

	@Override
	protected double getMin(double simTime) {
		if (cumulativeProbabilityListInput.isDefault() || valueListInput.isDefault())
			return Double.NaN;
		return valueListInput.getValue().get(0);
	}

	@Override
	protected double getMax(double simTime) {
		if (cumulativeProbabilityListInput.isDefault() || valueListInput.isDefault())
			return Double.NaN;
		return valueListInput.getValue().lastElement();
	}

	@Override
	protected double getMean(double simTime) {
		if (cumulativeProbabilityListInput.isDefault() || valueListInput.isDefault())
			return Double.NaN;
		double[] values = valueListInput.getValue().toArray();
		double[] cumProbs = cumulativeProbabilityListInput.getValue().toArray();
		return getMean(values, cumProbs);
	}

	@Override
	protected double getStandardDev(double simTime) {
		if (cumulativeProbabilityListInput.isDefault() || valueListInput.isDefault())
			return Double.NaN;
		double[] values = valueListInput.getValue().toArray();
		double[] cumProbs = cumulativeProbabilityListInput.getValue().toArray();
		return getStandardDev(values, cumProbs);
	}

	public static double getSample(double[] values, double[] cumProbs, MRG1999a rng) {
		double rand = rng.nextUniform();
		int k = Arrays.binarySearch(cumProbs, rand);
		if (k > 0)
			return values[k];
		int i = -k - 1;  // index of first cumProb > rand
		if (i == values.length)
			return values[values.length - 1];
		if (i == 0)
			return values[0];
		double ret = values[i - 1] + (rand - cumProbs[i - 1])*(values[i] - values[i - 1])/(cumProbs[i] - cumProbs[i - 1]);
		return ret;
	}

	public static double getMean(double[] values, double[] cumProbs) {
		double sum = 0.0;
		for (int i = 1; i < cumProbs.length; i++) {
			sum += (cumProbs[i] - cumProbs[i - 1]) * (values[i] + values[i - 1]);
		}
		return 0.5 * sum;
	}

	public static double getStandardDev(double[] values, double[] cumProbs) {
		double sum = 0.0;
		for (int i = 1; i < cumProbs.length; i++) {
			double val = values[i];
			double lastVal = values[i - 1];
			sum += (cumProbs[i] - cumProbs[i - 1]) * (val*val + val*lastVal + lastVal*lastVal);
		}

		double mean = getMean(values, cumProbs);
		return  Math.sqrt( sum/3.0 - (mean * mean) );
	}

}
