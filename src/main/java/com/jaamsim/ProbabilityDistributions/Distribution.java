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

import java.util.Random;

import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.IntegerInput;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation3D.DisplayEntity;

/**
 * ProbablityDistribution is the super-class for the various probability distributions implemented in JaamSim.
 * @author Harry King
 *
 */
public abstract class Distribution extends DisplayEntity
implements SampleProvider {
	@Keyword(description = "The unit type that the distribution returns values in.",
	         example = "ProbDist1 UnitType { DistanceUnit }")
	private final UnitTypeInput unitType;

	@Keyword(description = "Seed for the random number generator.  Must be an integer > 0.",
			 example = "ProbDist1 RandomSeed { 547 }")
	private final IntegerInput randomSeedInput;

	@Keyword(description = "Minimum value that can be returned.  Smaller values are rejected and resampled.",
	         example = "ProbDist1 MinValue { 0.0 }")
	private final ValueInput minValueInput;

	@Keyword(description = "Maximum value that can be returned.  Larger values are rejected and resampled.",
	         example = "ProbDist1 MaxValue { 200.0 }")
	private final ValueInput maxValueInput;

	protected final Random randomGenerator1; // first random generator for picking values
	private double presentSample; // last sample taken from the probability distribution (before ValueFactor is applied)

	private int sampleCount;
	private double sampleSum;
	private double sampleSquaredSum;
	private double sampleMin;
	private double sampleMax;

	{
		unitType = new UnitTypeInput("UnitType", "Key Inputs", UserSpecifiedUnit.class);
		this.addInput(unitType, true);

		randomSeedInput = new IntegerInput("RandomSeed", "Key Inputs", 1);
		randomSeedInput.setValidRange( 1, Integer.MAX_VALUE);
		this.addInput(randomSeedInput, true);

		minValueInput = new ValueInput("MinValue", "Key Inputs", Double.NEGATIVE_INFINITY);
		minValueInput.setUnitType(UserSpecifiedUnit.class);
		this.addInput(minValueInput, true);

		maxValueInput = new ValueInput("MaxValue", "Key Inputs", Double.POSITIVE_INFINITY);
		maxValueInput.setUnitType(UserSpecifiedUnit.class);
		this.addInput(maxValueInput, true);
	}

	public Distribution() {
		randomGenerator1 = new Random();
	}

	@Override
	public void validate() {
		super.validate();

		// The maximum value must be greater than the minimum value
		if( maxValueInput.getValue() <= minValueInput.getValue() ) {
			throw new InputErrorException( "The input for MaxValue must be greater than that for MinValue.");
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		// Set the seed for the first random generator
		randomGenerator1.setSeed( randomSeedInput.getValue() );

		// Initialise the sample statistics
		sampleCount = 0;
		sampleSum = 0.0;
		sampleSquaredSum = 0.0;
		sampleMin = Double.POSITIVE_INFINITY;
		sampleMax = Double.NEGATIVE_INFINITY;
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == unitType) {
			setUnitType(getUnitType());
			return;
		}
	}

	/**
	 * Select the next sample from the probability distribution.
	 */
	protected abstract double getNextSample();

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType.getUnitType();
	}

	protected void setUnitType(Class<? extends Unit> ut) {
		minValueInput.setUnitType(ut);
		maxValueInput.setUnitType(ut);
		this.getOutputHandle("CalculatedMean"             ).setUnitType(ut);
		this.getOutputHandle("CalculatedStandardDeviation").setUnitType(ut);
		this.getOutputHandle("SampleMean"                 ).setUnitType(ut);
		this.getOutputHandle("SampleStandardDeviation"    ).setUnitType(ut);
		this.getOutputHandle("SampleMin"                  ).setUnitType(ut);
		this.getOutputHandle("SampleMax"                  ).setUnitType(ut);
	}

	/**
	 * Returns the next sample from the probability distribution.
	 */
	@Override
	public final double getNextSample(double simTime) {
		// Loop until the select sample falls within the desired min and max values
		double nextSample;
		do {
			nextSample = this.getNextSample();
		}
		while (nextSample < this.minValueInput.getValue() ||
		       nextSample > this.maxValueInput.getValue());

		// Collect statistics on the sampled values
		presentSample = nextSample;
		sampleCount++;
		sampleSum += this.presentSample;
		sampleSquaredSum += this.presentSample * this.presentSample;
		sampleMin = Math.min(this.sampleMin, this.presentSample);
		sampleMax = Math.max(this.sampleMax, this.presentSample);
		return presentSample;
	}

	protected double getMinValue() {
		return minValueInput.getValue();
	}

	protected double getMaxValue() {
		return maxValueInput.getValue();
	}

	/**
	 * Returns the mean value for the distribution calculated from the inputs.  It is NOT the mean of the sampled values.
	 */
	protected abstract double getMeanValue();

	/**
	 * Returns the standard deviation for the distribution calculated from the inputs.  It is NOT the standard deviation of the sampled values.
	 */
	protected abstract double getStandardDeviation();

	@Output( name="CalculatedMean",
			 description="The mean of the probability distribution calculated directly from the inputs.  " +
					"It is NOT the mean of the sampled values.  The inputs for MinValue and MaxValue are ignored.",
			 unitType=UserSpecifiedUnit.class)
	public double getMeanValue( double simTime ) {
		return this.getMeanValue();
	}

	@Output( name="CalculatedStandardDeviation",
			 description="The standard deviation of the probability distribution calculated directly from the inputs.  " +
					"It is NOT the standard deviation of the sampled values.  The inputs for MinValue and MaxValue are ignored.",
			 unitType=UserSpecifiedUnit.class)
	public double getStandardDeviation( double simTime ) {
		return this.getStandardDeviation();
	}

	@Output( name="NumberOfSamples",
			 description="The number of times the probability distribution has been sampled.",
			 unitType=DimensionlessUnit.class)
	public int getNumberOfSamples( double simTime ) {
		return sampleCount;
	}

	@Output( name="SampleMean",
			 description="The mean of the values sampled from the probability distribution.",
			 unitType=UserSpecifiedUnit.class)
	public double getSampleMean( double simTime ) {
		return sampleSum / sampleCount;
	}

	@Output( name="SampleStandardDeviation",
			 description="The standard deviation of the values sampled from the probability distribution.",
			 unitType=UserSpecifiedUnit.class)
	public double getSampleStandardDeviation( double simTime ) {
		double sampleMean = sampleSum / sampleCount;
		return Math.sqrt( sampleSquaredSum/sampleCount - sampleMean*sampleMean );
	}

	@Output( name="SampleMin",
			 description="The minimum of the values sampled from the probability distribution.",
			 unitType=UserSpecifiedUnit.class)
	public double getSampleMin( double simTime ) {
		return sampleMin;
	}

	@Output( name="SampleMax",
			 description="The maximum of the values sampled from the probability distribution.",
			 unitType=UserSpecifiedUnit.class)
	public double getSampleMax( double simTime ) {
		return sampleMax;
	}
}

