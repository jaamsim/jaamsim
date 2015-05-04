/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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
import com.jaamsim.input.TimeSeriesInput;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

/**
 * Non-Stationary Exponential Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 5th Edition", page 479.
 */
public class NonStatExponentialDist extends Distribution {

	@Keyword(description = "A time series containing the expected cumulative number of arrivals as a function of time.",
			example = "NonStatExponentialDist1 ExpectedArrivals { TimeSeries1 }")
	private final TimeSeriesInput expectedArrivals;

	private final MRG1999a rng = new MRG1999a();

	{
		minValueInput.setDefaultValue(0.0);

		unitType.setHidden(true);
		unitType.setDefaultValue(TimeUnit.class);
		this.setUnitType(TimeUnit.class);

		expectedArrivals = new TimeSeriesInput("ExpectedArrivals", "Key Inputs", null);
		expectedArrivals.setUnitType(DimensionlessUnit.class);
		this.addInput(expectedArrivals);
	}

	public NonStatExponentialDist() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected double getNextSample() {

		long ticksNow = getSimTicks();
		double valueNow = expectedArrivals.getValue().getInterpolatedCumulativeValueForTicks(ticksNow);
		double valueNext = valueNow - Math.log(rng.nextUniform());
		long ticksNext = expectedArrivals.getValue().getInterpolatedTicksForValue(valueNext);

		if (ticksNext == Long.MAX_VALUE)
			return Double.POSITIVE_INFINITY;

		if (ticksNext < ticksNow)
			error("Negative time advance");

		return FrameBox.ticksToSeconds(ticksNext - ticksNow);
	}

	@Override
	protected double getMeanValue() {
		double arrivals = expectedArrivals.getValue().getMaxValue();
		double dt = FrameBox.ticksToSeconds( expectedArrivals.getValue().getMaxTicksValue() );
		return dt/arrivals;
	}

	@Override
	protected double getStandardDeviation() {
		return 0.0d;
	}

}
