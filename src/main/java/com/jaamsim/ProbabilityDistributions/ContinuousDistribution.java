/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
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

import com.jaamsim.datatypes.DoubleVector;
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

		double rand = rng.nextUniform();
		DoubleVector cumList = cumulativeProbabilityListInput.getValue();
		for( int i=1; i<cumList.size(); i++) {
			if( rand < cumList.get(i) ) {
				double cum = cumList.get(i);
				double lastCum = cumList.get(i-1);
				double val = valueListInput.getValue().get(i);
				double lastVal = valueListInput.getValue().get(i-1);
				return lastVal + (rand-lastCum)*(val-lastVal)/(cum-lastCum);
			}
		}
		return valueListInput.getValue().get( cumList.size()-1 );
	}

	@Override
	public double getMinValue() {
		return Math.max( valueListInput.getValue().get(0), super.getMinValue());
	}

	@Override
	public double getMaxValue() {
		return Math.min( valueListInput.getValue().lastElement(), super.getMaxValue());
	}

	@Override
	protected double getMean(double simTime) {
		double sum = 0.0;
		DoubleVector cumList = cumulativeProbabilityListInput.getValue();
		DoubleVector valueList = valueListInput.getValue();
		if (cumList == null || valueList == null)
			return Double.NaN;
		for( int i=1; i<cumList.size(); i++) {
			sum += ( cumList.get(i) - cumList.get(i-1) ) * ( valueList.get(i) + valueList.get(i-1) );
		}
		return 0.5 * sum;
	}

	@Override
	protected double getStandardDev(double simTime) {
		double sum = 0.0;
		DoubleVector cumList = cumulativeProbabilityListInput.getValue();
		DoubleVector valueList = valueListInput.getValue();
		if (cumList == null || valueList == null)
			return Double.NaN;
		for( int i=1; i<cumList.size(); i++) {
			double val = valueList.get(i);
			double lastVal = valueList.get(i-1);
			sum += ( cumList.get(i) - cumList.get(i-1) ) * ( val*val + val*lastVal + lastVal*lastVal );
		}

		double mean = getMean(simTime);
		return  Math.sqrt( sum/3.0 - (mean * mean) );
	}
}
