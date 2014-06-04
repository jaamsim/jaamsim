/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.ProbabilityDistributions;

import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.InputErrorException;

/**
 * ContinuousDistribution is a user-defined probability distribution that selects an output value based on an ordered list
 * of values and cumulative probabilities.  The inputs specify a continuous cumulative probability distribution by
 * linearly interpolating between the given values and cumulative probabilities.
 * @author Harry King
 *
 */
public class ContinuousDistribution extends Distribution {

	@Keyword(description = "The list of values for the user-defined cumulative probability distribution.",
	         example = "ContinuousDist1 ValueList { 2.0  4.3  8.9 }")
	private final ValueListInput valueListInput;

	@Keyword(description = "The list of cumulative probabilities corresponding to the values in the ValueList.  " +
			"The cumulative probabilities must be given in increasing order.  The first value must be exactly 0.0.  " +
			"The last value must be exactly 1.0.",
	         example = "ContinuousDist1 CumulativeProbabilityList { 0.0  0.6  1.0 }")
	private final ValueListInput cumulativeProbabilityListInput;

	private final MRG1999a rng = new MRG1999a();

	{
		valueListInput = new ValueListInput("ValueList", "Key Inputs", null);
		valueListInput.setUnitType(UserSpecifiedUnit.class);
		this.addInput( valueListInput);

		cumulativeProbabilityListInput = new ValueListInput( "CumulativeProbabilityList", "Key Inputs", null);
		cumulativeProbabilityListInput.setUnitType(DimensionlessUnit.class);
		cumulativeProbabilityListInput.setValidRange(0.0d, 1.0d);
		this.addInput( cumulativeProbabilityListInput);
	}

	public ContinuousDistribution() {}

	@Override
	public void validate() {
		super.validate();

		// The number of entries in the ValueList and CumulativeProbabilityList inputs must match
		if( cumulativeProbabilityListInput.getValue().size() != valueListInput.getValue().size() ) {
			throw new InputErrorException( "The number of entries for CumulativeProbabilityList and ValueList must be equal" );
		}

		// The first entry in the CumulativeProbabilityList must be 0.0
		if( cumulativeProbabilityListInput.getValue().get(0) != 0.0 ) {
			throw new InputErrorException( "The first entry for CumulativeProbabilityList must be exactly 0.0" );
		}

		// The last entry in the CumulativeProbabilityList must be 1.0
		int n = cumulativeProbabilityListInput.getValue().size();
		if( cumulativeProbabilityListInput.getValue().get(n-1) != 1.0 ) {
			throw new InputErrorException( "The last entry for CumulativeProbabilityList must be exactly 1.0" );
		}

		// The entries in the CumulativeProbabilityList must be given in increasing order
		double last = -1.0;
		for( int i=0; i<n; i++) {
			if( cumulativeProbabilityListInput.getValue().get(i) <= last ) {
				throw new InputErrorException( "The entries for CumulativeProbabilityList must be given in increasing order" );
			}
			last = cumulativeProbabilityListInput.getValue().get(i);
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
	protected double getNextSample() {

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
		return Math.max( valueListInput.getValue().get(0), minValueInput.getValue());
	}

	@Override
	public double getMaxValue() {
		return Math.min( valueListInput.getValue().lastElement(), maxValueInput.getValue());
	}

	@Override
	protected double getMeanValue() {
		double sum = 0.0;
		DoubleVector cumList = cumulativeProbabilityListInput.getValue();
		DoubleVector valueList = valueListInput.getValue();
		for( int i=1; i<cumList.size(); i++) {
			sum += ( cumList.get(i) - cumList.get(i-1) ) * ( valueList.get(i) + valueList.get(i-1) );
		}
		return 0.5 * sum;
	}

	@Override
	protected double getStandardDeviation() {
		double sum = 0.0;
		DoubleVector cumList = cumulativeProbabilityListInput.getValue();
		DoubleVector valueList = valueListInput.getValue();
		for( int i=1; i<cumList.size(); i++) {
			double val = valueList.get(i);
			double lastVal = valueList.get(i-1);
			sum += ( cumList.get(i) - cumList.get(i-1) ) * ( val*val + val*lastVal + lastVal*lastVal );
		}

		double mean = getMeanValue();
		return  Math.sqrt( sum/3.0 - (mean * mean) );
	}
}
