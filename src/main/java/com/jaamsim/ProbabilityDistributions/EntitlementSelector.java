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

import com.jaamsim.datatypes.DoubleVector;

/**
 * EntitlementSelector selects the next entity to return based on the difference between the expected and actual numbers
 * of samples for each object.  The object with the biggest difference (expected - actual) is selected.
 * @author Harry King
 *
 */
public class EntitlementSelector extends ObjectSelector {

	@Override
	protected int getNextIndex() {
		DoubleVector probList = this.getProbabilityList();
		int index = 0;
		double maxDiff = Double.NEGATIVE_INFINITY;

		int nextTotal = this.getNumberOfSamples() + 1;
		for( int i=0; i<probList.size(); i++) {
			double diff = nextTotal * probList.get(i) - this.getSampleCount(i);
			if( diff > maxDiff ) {
				maxDiff = diff;
				index = i;
			}
		}
		return index;
	}
}
