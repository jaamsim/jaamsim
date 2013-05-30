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
	@Keyword(description = "The unittype that the distribution returns values in.",
	         example = "ProbDist1 UnitType { DistanceUnit }")
	private final UnitTypeInput unitType;

	@Keyword(description = "Seed for the random number generator.  Must be an integer > 0.",
			 example = "ProbDist1 RandomSeed { 547 }")
	private final IntegerInput randomSeedInput;

	@Keyword(description = "Minimum value that can be returned (before ValueFactor is applied). " +
					"Smaller values are rejected and resampled.",
	         example = "ProbDist1 MinValue { 0.0 }")
	private final ValueInput minValueInput;

	@Keyword(description = "Maximum value that can be returned (before ValueFactor is applied). " +
					"Larger values are rejected and resampled.",
	         example = "ProbDist1 MaxValue { 200.0 }")
	private final ValueInput maxValueInput;

	protected final Random randomGenerator1; // first random generator for picking values
	protected final Random randomGenerator2; // second random generator for picking values
	private double presentSample; // last sample taken from the probability distribution (before ValueFactor is applied)

	private int sampleCount;
	private double sampleSum;
	private double sampleSquaredSum;
	private double sampleMin;
	private double sampleMax;

	{
		unitType = new UnitTypeInput("UnitType", "Key Inputs");
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
		randomGenerator2 = new Random();
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

		// Set the seeds for the two random generators
		randomGenerator1.setSeed( randomSeedInput.getValue() );
		int seed = (int) ( randomGenerator1.nextDouble() * 10000.0 );
		randomGenerator2.setSeed( seed );

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
	private void setNextSample() {
		// Loop until the select sample falls within the desired min and max values
		do {
			presentSample = this.getNextNonZeroSample();
		}
		while (presentSample < minValueInput.getValue() ||
		       presentSample > maxValueInput.getValue());

		// Collect statistics on the sampled values
		sampleCount++;
		sampleSum += presentSample;
		sampleSquaredSum += presentSample*presentSample;
		sampleMin = Math.min( sampleMin, presentSample);
		sampleMax = Math.max( sampleMax, presentSample);
	}

	/**
	 * Select the next sample from the probability distribution.
	 */
	protected abstract double getNextNonZeroSample();

	/**
	 * Return the present sample from probability distribution.
	 */
	public double getValue(double simTime) {
		return presentSample;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType.getUnitType();
	}

	protected void setUnitType(Class<? extends Unit> specified) {
		minValueInput.setUnitType(specified);
		maxValueInput.setUnitType(specified);
	}

	/**
	 * Returns the next sample from the probability distribution.
	 */
	@Override
	public double getNextSample(double simTime) {
		this.setNextSample();
		return (this.getValue(simTime));
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
			 		"It is NOT the mean of the sampled values.  The inputs for MinValue and MaxValue are ignored.")
	public double getMeanValue( double simTime ) {
		return this.getMeanValue();
	}

	@Output( name="CalculatedStandardDeviation",
			 description="The standard deviation of the probability distribution calculated directly from the inputs.  " +
			 		"It is NOT the standard deviation of the sampled values.  The inputs for MinValue and MaxValue are ignored.")
	public double getStandardDeviation( double simTime ) {
		return this.getStandardDeviation();
	}

	@Output( name="NumberOfSamples",
			 description="The number of times the probability distribution has been sampled.")
	public int getNumberOfSamples( double simTime ) {
		return sampleCount;
	}

	@Output( name="SampleMean",
			 description="The mean of the values sampled from the probability distribution.")
	public double getSampleMean( double simTime ) {
		return sampleSum / sampleCount;
	}

	@Output( name="SampleStandardDeviation",
			 description="The standard deviation of the values sampled from the probability distribution.")
	public double getSampleStandardDeviation( double simTime ) {
		double sampleMean = sampleSum / sampleCount;
		return Math.sqrt( sampleSquaredSum/sampleCount - sampleMean*sampleMean );
	}

	@Output( name="SampleMin",
			 description="The minimum of the values sampled from the probability distribution.")
	public double getSampleMin( double simTime ) {
		return sampleMin;
	}

	@Output( name="SampleMax",
			 description="The maximum of the values sampled from the probability distribution.")
	public double getSampleMax( double simTime ) {
		return sampleMax;
	}
}

