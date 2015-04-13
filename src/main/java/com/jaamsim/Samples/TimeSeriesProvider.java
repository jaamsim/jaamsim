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
package com.jaamsim.Samples;


public interface TimeSeriesProvider extends SampleProvider {
	public double getNextTimeAfter(double simTime);
	public double getValueForTicks(long ticks);
	public long getNextChangeAfterTicks(long ticks);
	public long getMaxTicksValue();

	/**
	 * Returns the simulation time in ticks corresponding to the specified
	 * value that it interpolated from the time series entries.
	 * <p>
	 * The time series values must increase monotonically.
	 * @param val - specified value.
	 * @return interpolated simulation time in clock ticks.
	 */
	public long getInterpolatedTicksForValue(double val);

	/**
	 * Returns the value corresponding to the specified simulation
	 * time in simulation clock ticks that it interpolated from the
	 * time series entries.
	 * @param ticks - simulation time in clock ticks.
	 * @return interpolated value.
	 */
	public double getInterpolatedCumulativeValueForTicks(long ticks);
}
