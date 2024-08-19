/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2024 JaamSim Software Inc.
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

import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Samples.TimeSeries;
import com.jaamsim.Samples.TimeSeriesProvider;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.TimeSeriesInput;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

/**
 * Non-Stationary Exponential Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 5th Edition", page 479.
 */
public class NonStatExponentialDist extends Distribution {

	@Keyword(description = "A time series containing the expected cumulative number of arrivals "
	                     + "as a function of time.",
	         exampleList = {"TimeSeries1"})
	private final TimeSeriesInput expectedArrivals;

	@Keyword(description = "An optional factor that multiplies the data from the "
	                     + "'ExpectedArrival' input. "
	                     + "For example, an input of 2 will double the number of expected "
	                     + "arrivals at any given time.",
	         exampleList = {"2.0"})
	private final SampleInput scaleFactor;

	private final MRG1999a rng = new MRG1999a();

	{
		minValueInput.setDefaultValue(0.0d);

		unitType.setHidden(true);
		unitType.setDefaultValue(TimeUnit.class);
		this.setUnitType(TimeUnit.class);

		expectedArrivals = new TimeSeriesInput("ExpectedArrivals", KEY_INPUTS, null);
		expectedArrivals.setUnitType(DimensionlessUnit.class);
		expectedArrivals.setRequired(true);
		this.addInput(expectedArrivals);

		scaleFactor = new SampleInput("ScaleFactor", KEY_INPUTS, 1.0d);
		scaleFactor.setUnitType(DimensionlessUnit.class);
		scaleFactor.setOutput(true);
		this.addInput(scaleFactor);
	}

	public NonStatExponentialDist() {}

	@Override
	public void validate() {
		super.validate();

		if (!(expectedArrivals.getValue() instanceof TimeSeries))
			throw new InputErrorException("The ExpectedArrivals input must be a TimeSeries, "
					+ "not a constant.");

		TimeSeries ts = (TimeSeries) (expectedArrivals.getValue());

		if (!ts.isMonotonic(1))
			throw new InputErrorException("The ExpectedArrivals input must be a TimeSeries "
					+ "that increases monotonically.");

		if (ts.getMinValue() != 0.0d)
			throw new InputErrorException("The ExpectedArrivals input must be a TimeSeries "
					+ "that starts with zero expected arrivals at time zero.");
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	private double getScaleFactor(double simTime) {
		return scaleFactor.getNextSample(this, simTime);
	}

	@Override
	protected double getSample(double simTime) {

		long ticksNow = getSimTicks();  // ignore the simTime passed as an argument
		TimeSeriesProvider ts = expectedArrivals.getValue();
		double factor = getScaleFactor(simTime);
		double valueNow = factor * ts.getInterpolatedCumulativeValueForTicks(ticksNow);
		double valueNext = valueNow - Math.log(rng.nextUniform());
		long ticksNext = ts.getInterpolatedTicksForValue(valueNext/factor);

		if (ticksNext == Long.MAX_VALUE)
			return Double.POSITIVE_INFINITY;

		if (ticksNext < ticksNow)
			error("Negative time advance");

		return getJaamSimModel().getEventManager().ticksToSeconds(ticksNext - ticksNow);
	}

	@Override
	protected double getMean(double simTime) {
		if (expectedArrivals.getValue() == null)
			return Double.NaN;
		double factor = getScaleFactor(simTime);
		double arrivals = factor * expectedArrivals.getValue().getMaxValue();
		double dt = getJaamSimModel().getEventManager().ticksToSeconds( expectedArrivals.getValue().getMaxTicksValue() );
		return dt/arrivals;
	}

	@Override
	protected double getStandardDev(double simTime) {
		return Double.NaN;
	}

	@Override
	protected double getMin(double simTime) {
		if (expectedArrivals.isDefault())
			return Double.NaN;
		return expectedArrivals.getValue().getMinValue();
	}

	@Override
	protected double getMax(double simTime) {
		if (expectedArrivals.isDefault())
			return Double.NaN;
		return expectedArrivals.getValue().getMaxValue();
	}

}
