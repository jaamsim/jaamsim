/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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
			exampleList = {"TimeSeries1"})
	private final TimeSeriesInput expectedArrivals;

	private final MRG1999a rng = new MRG1999a();

	{
		minValueInput.setDefaultValue(0.0);

		unitType.setHidden(true);
		unitType.setDefaultValue(TimeUnit.class);
		this.setUnitType(TimeUnit.class);

		expectedArrivals = new TimeSeriesInput("ExpectedArrivals", "Key Inputs", null);
		expectedArrivals.setUnitType(DimensionlessUnit.class);
		expectedArrivals.setRequired(true);
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
