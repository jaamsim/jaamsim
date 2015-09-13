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
package com.jaamsim.StringProviders;

import com.jaamsim.Samples.SampleProvider;

public class StringProvSample implements StringProvider {
	private final SampleProvider samp;

	public StringProvSample(SampleProvider s) {
		samp = s;
	}

	@Override
	public String getNextString(double simTime, String fmt, double siFactor) {
		return String.format(fmt, samp.getNextSample(simTime)/siFactor);
	}

	@Override
	public String toString() {
		return samp.toString();
	}

}
