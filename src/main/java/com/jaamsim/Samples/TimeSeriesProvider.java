/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.Samples;


public interface TimeSeriesProvider extends SampleProvider {
	public double getNextTimeAfter(double simTime);
	public double getValueForTicks(long ticks);
	public long getNextChangeAfterTicks(long ticks);
	public long getLastChangeBeforeTicks(long ticks);
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
