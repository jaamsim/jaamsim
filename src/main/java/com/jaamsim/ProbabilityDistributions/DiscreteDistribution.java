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
 * DiscreteDistribution is a user-defined probability distribution that selects from a given list of specific values
 * based on a specified probability for each value.  No interpolation is performed between values.
 * @author Harry King
 *
 */
public class DiscreteDistribution extends Distribution {

	@Keyword(description = "The list of discrete values that can be returned by the distribution.  " +
			"The values can be any positive or negative and can be listed in any order.  " +
			"No interpolation is performed between these values.",
	         example = "DiscreteDist1 ValueList { 6.2 10.1 }")
	private final ValueListInput valueListInput;

	@Keyword(description = "The list of probabilities corresponding to the discrete values in the ValueList.  Must sum to 1.0.",
	         example = "DiscreteDist1 ProbabilityList { 0.3  0.7 }")
	private final ValueListInput probabilityListInput;

	private final MRG1999a rng = new MRG1999a();

	{
		valueListInput = new ValueListInput( "ValueList", "Key Inputs", null);
		valueListInput.setUnitType(UserSpecifiedUnit.class);
		this.addInput( valueListInput);

		probabilityListInput = new ValueListInput( "ProbabilityList", "Key Inputs", null);
		probabilityListInput.setUnitType(DimensionlessUnit.class);
		probabilityListInput.setValidSum(1.0d, 0.001d);
		this.addInput( probabilityListInput);
	}

	public DiscreteDistribution() {}

	@Override
	public void validate() {
		super.validate();

		// The number of entries in the ValueList and ProbabilityList inputs must match
		if( probabilityListInput.getValue().size() != valueListInput.getValue().size() ) {
			throw new InputErrorException( "The number of entries for ProbabilityList and ValueList must be equal" );
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
		double cumProb = 0.0;
		DoubleVector probList = probabilityListInput.getValue();
		for( int i=0; i<probList.size(); i++) {
			cumProb += probList.get(i);
			if( rand <= cumProb ) {
				return valueListInput.getValue().get(i);
			}
		}
		return valueListInput.getValue().get( probList.size()-1 );
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
		double ret = 0.0;
		for( int i=0; i<probabilityListInput.getValue().size(); i++) {
			ret += probabilityListInput.getValue().get(i) * valueListInput.getValue().get(i);
		}
		return ret;
	}

	@Override
	protected double getStandardDeviation() {
		double sum = 0.0;
		for( int i=0; i<probabilityListInput.getValue().size(); i++) {
			double val = valueListInput.getValue().get(i);
			sum += probabilityListInput.getValue().get(i) * val * val;
		}
		double mean = getMeanValue();
		return  Math.sqrt( sum - (mean * mean) );
	}
}
