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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.Statistics.SampleStatistics;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * ProbablityDistribution is the super-class for the various probability distributions implemented in JaamSim.
 * @author Harry King
 *
 */
public abstract class Distribution extends DisplayEntity
implements SampleProvider, RandomStreamUser {

	@Keyword(description = "The unit type for the values returned by the distribution. "
	                     + "MUST be entered before most other inputs.",
	         exampleList = {"DistanceUnit"})
	protected final UnitTypeInput unitType;

	@Keyword(description = "Seed for the random number generator. "
	                     + "Must be an integer >= 0. \n\n"
	                     + "The 'RandomSeed' keyword works together with the "
	                     + "'GlobalSubstreamSeed' keyword for Simulation to determine the random "
	                     + "sequence. "
	                     + "The 'GlobalSubsteamSeed' keyword allows the user to change all the "
	                     + "random sequences in a model with a single input.\n\n"
	                     + "When an object with this input is copied and pasted, the RandomSeed "
	                     + "input is reset to an unused value for each copy that is pasted.",
			 exampleList = {"547"})
	private final IntegerInput randomSeedInput;

	@Keyword(description = "Minimum value that can be returned. "
	                     + "Smaller values are rejected and resampled.",
	         exampleList = {"0.0", "InputValue1", "'2 * [InputValue1].Value'"})
	protected final SampleInput minValueInput;

	@Keyword(description = "Maximum value that can be returned. "
	                     + "Larger values are rejected and resampled.",
	         exampleList = {"200.0", "InputValue1", "'2 * [InputValue1].Value'"})
	protected final SampleInput maxValueInput;

	private final SampleStatistics stats = new SampleStatistics();
	private double lastSample = 0;

	private static int MAX_ATTEMPTS = 1000;

	{
		unitType = new UnitTypeInput("UnitType", KEY_INPUTS, UserSpecifiedUnit.class);
		unitType.setRequired(true);
		unitType.setCallback(inputCallback);
		this.addInput(unitType);

		randomSeedInput = new IntegerInput("RandomSeed", KEY_INPUTS, -1);
		randomSeedInput.setValidRange(0, Integer.MAX_VALUE);
		randomSeedInput.setRequired(true);
		randomSeedInput.setDefaultText("None");
		this.addInput(randomSeedInput);

		minValueInput = new SampleInput("MinValue", KEY_INPUTS, Double.NEGATIVE_INFINITY);
		minValueInput.setUnitType(UserSpecifiedUnit.class);
		this.addInput(minValueInput);

		maxValueInput = new SampleInput("MaxValue", KEY_INPUTS, Double.POSITIVE_INFINITY);
		maxValueInput.setUnitType(UserSpecifiedUnit.class);
		this.addInput(maxValueInput);
	}

	public Distribution() {}

	@Override
	public void validate() {
		super.validate();

		// The maximum value must be greater than or equal to the minimum value
		if( this.getMaxValue() < this.getMinValue() ) {
			throw new InputErrorException( "The input for MaxValue must be greater than or equal to the input for MinValue.");
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		stats.clear();
		lastSample = getInitValue();
	}

	static final InputCallback inputCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((Distribution)ent).updateInputValue();
		}
	};

	void updateInputValue() {
		setUnitType(getUnitType());
	}

	public double getInitValue() {
		return getMeanValue(0);
	}

	@Override
	public void setInputsForDragAndDrop() {
		super.setInputsForDragAndDrop();

		// Find the largest seed used so far
		int seed = getSimulation().getLargestStreamNumber();

		// Set the random number seed next unused value
		InputAgent.applyIntegers(this, randomSeedInput.getKeyword(), seed + 1);
	}

	@Override
	public Class<? extends Unit> getUserUnitType() {
		return unitType.getUnitType();
	}

	/**
	 * Select the next sample from the probability distribution.
	 */
	protected abstract double getSample(double simTime);

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType.getUnitType();
	}

	protected void setUnitType(Class<? extends Unit> ut) {
		minValueInput.setUnitType(ut);
		maxValueInput.setUnitType(ut);
	}

	@Override
	public int getStreamNumber() {
		return randomSeedInput.getValue();
	}

	@Override
	public String getStreamNumberKeyword() {
		return randomSeedInput.getKeyword();
	}

	public int getSubstreamNumber() {
		return getSimulation().getSubstreamNumber();
	}

	/**
	 * Returns the next sample from the probability distribution.
	 */
	@Output(name = "Value",
	 description = "The last value sampled from the distribution. When used in an "
	             + "expression, this output returns a new sample every time the expression "
	             + "is evaluated.",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 0)
	@Override
	public final double getNextSample(double simTime) {
		// If we are not in a model context, do not perturb the distribution by sampling,
		// instead simply return the last sampled value
		if (!EventManager.hasCurrent()) {
			if (simTime == 0.0d)
				return getInitValue();
			return lastSample;
		}

		// Loop until the select sample falls within the desired min and max values
		double nextSample;
		double minVal = this.minValueInput.getNextSample(simTime);
		double maxVal = this.maxValueInput.getNextSample(simTime);
		int n = 0;
		do {
			if (n > MAX_ATTEMPTS) {
				this.error("Could not find a sample value that was within the range specified by "
						+ "the MinValue and MaxValue inputs.%n"
						+ "Number of samples tested = %s", MAX_ATTEMPTS);
			}
			nextSample = this.getSample(simTime);
			n++;
		}
		while (nextSample < minVal ||
		       nextSample > maxVal);

		lastSample = nextSample;
		stats.addValue(nextSample);
		return nextSample;
	}

	@Override
	public double getMinValue() {
		return minValueInput.getValue().getMinValue();
	}

	@Override
	public double getMaxValue() {
		return maxValueInput.getValue().getMaxValue();
	}

	/**
	 * Returns the mean value for the distribution calculated from the inputs.  It is NOT the mean of the sampled values.
	 */
	protected abstract double getMean(double simTime);

	/**
	 * Returns the standard deviation for the distribution calculated from the inputs.  It is NOT the standard deviation of the sampled values.
	 */
	protected abstract double getStandardDev(double simTime);

	@Output(name = "CalculatedMean",
	 description = "The mean of the probability distribution calculated directly from the inputs. "
	             + "It is NOT the mean of the sampled values. "
	             + "The inputs for MinValue and MaxValue are ignored.",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 1)
	@Override
	public double getMeanValue(double simTime) {
		return this.getMean(simTime);
	}

	@Output(name = "CalculatedStandardDeviation",
	 description = "The standard deviation of the probability distribution calculated directly "
	             + "from the inputs. It is NOT the standard deviation of the sampled values. "
	             + "The inputs for MinValue and MaxValue are ignored.",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 2)
	public double getStandardDeviation(double simTime) {
		return this.getStandardDev(simTime);
	}

	@Output(name = "NumberOfSamples",
	 description = "The number of times the probability distribution has been sampled.",
	    unitType = DimensionlessUnit.class,
	    sequence = 3)
	public long getNumberOfSamples(double simTime) {
		return stats.getCount();
	}

	@Output(name = "SampleMean",
	 description = "The mean of the values sampled from the probability distribution.",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 4)
	public double getSampleMean(double simTime) {
		return stats.getMean();
	}

	@Output(name = "SampleStandardDeviation",
	 description = "The standard deviation of the values sampled from the probability "
	             + "distribution.",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 5)
	public double getSampleStandardDeviation(double simTime) {
		return stats.getStandardDeviation();
	}

	@Output(name = "SampleMin",
	 description = "The minimum of the values sampled from the probability distribution.",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 6)
	public double getSampleMin(double simTime) {
		return stats.getMin();
	}

	@Output(name = "SampleMax",
	 description = "The maximum of the values sampled from the probability distribution.",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 7)
	public double getSampleMax(double simTime) {
		return stats.getMax();
	}
}
