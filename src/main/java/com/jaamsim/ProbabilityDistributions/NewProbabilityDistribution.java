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

import com.jaamsim.input.Output;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.IntegerInput;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.InputErrorException;
import java.util.Random;

/**
 * ProbablityDistribution is the super-class for the various probability distributions implemented in JaamSim.
 * @author Harry King
 *
 */
public abstract class NewProbabilityDistribution extends DisplayEntity {

	@Keyword(desc = "Seed for the random number generator.  Must be an integer > 0.",
			 example = "ProbDist1 RandomSeed { 547 }")
	private final IntegerInput randomSeedInput;

	@Keyword(desc = "Probability of a non-zero value. The probability of a non-zero value is sampled first. " +
					"The probability distribution is sampled only if the first sample is positive. " +
					"For example, if the non-zero probability is 0.2, then the value of zero will be returned for 80% of the samples. " +
					"A non-zero value sampled from the probability distribution will be returned for 20% of the samples.",
	         example = "ProbDist1 NonZeroProb { 0.2 }")
	private final DoubleInput nonZeroProbInput;

	@Keyword(desc = "Multiplicative factor applied to the values returned by the ProbabilityDistribution object. " +
					"Used for unit conversion.",
	         example = "ProbDist1 ValueFactor { 1.0 }")
	private final DoubleInput valueFactorInput;

	@Keyword(desc = "Minimum value that can be returned (before ValueFactor is applied). " +
					"Smaller values are rejected and resampled.",
	         example = "ProbDist1 MinValue { 0.0 }")
	private final DoubleInput minValueInput;

	@Keyword(desc = "Maximum value that can be returned (before ValueFactor is applied). " +
					"Larger values are rejected and resampled.",
	         example = "ProbDist1 MaxValue { 200.0 }")
	private final DoubleInput maxValueInput;

	protected final Random randomGenerator1; // first random generator for picking values
	protected final Random randomGenerator2; // second random generator for picking values
	private double presentSample; // last sample taken from the probability distribution (before ValueFactor is applied)

	private int sampleCount;
	private double sampleSum;
	private double sampleSquaredSum;
	private double sampleMin;
	private double sampleMax;

	{
		randomSeedInput = new IntegerInput("RandomSeed", "Key Inputs", 1);
		randomSeedInput.setValidRange( 1, Integer.MAX_VALUE);
		this.addInput(randomSeedInput, true);

		nonZeroProbInput = new DoubleInput("NonZeroProb", "Key Inputs", 1.0d);
		nonZeroProbInput.setValidRange( 0.0d, 1.0d);
		this.addInput(nonZeroProbInput, true);

		valueFactorInput = new DoubleInput("ValueFactor", "Key Inputs", 1.0d);
		this.addInput(valueFactorInput, true);

		minValueInput = new DoubleInput("MinValue", "Key Inputs", Double.NEGATIVE_INFINITY);
		this.addInput(minValueInput, true);

		maxValueInput = new DoubleInput("MaxValue", "Key Inputs", Double.POSITIVE_INFINITY);
		this.addInput(maxValueInput, true);
	}

	public NewProbabilityDistribution() {
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

	/**
	 * Select the next sample from the probability distribution.
	 */
	private void setNextSample() {

		// If the NonZeroProb input is used, then test for a non-zero sample first
		if( nonZeroProbInput.getValue() < 1.0 &&
				( randomGenerator2.nextDouble() > nonZeroProbInput.getValue() ) ) {
			presentSample = 0.0;
		}

		// Sample a non-zero value from the distribution
		else {

			// Loop until the select sample falls within the desired min and max values
			do {
				presentSample = this.getNextNonZeroSample();
			} while( presentSample < minValueInput.getValue() || ( presentSample > maxValueInput.getValue() ) );
		}

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
	public double getValue() {
		return presentSample * valueFactorInput.getValue();
	}

	/**
	 * Returns the next sample from the probability distribution.
	 */
	public double nextValue() {
		this.setNextSample();
		return ( this.getValue() );
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

