/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation;

import java.util.Random;

/**
 * Random generator class.
 */
public class RandomGenerator {

	Random random;

	public RandomGenerator() {
		random = new Random( 2345 );
	}

	public int selectIndexUsingProbs( DoubleVector probs ) {
		//Use the random number generator to select an index from the given probability distribution.
		double rand = 0.0;
		double cumProb = 0.0;

		// Select a random number that is uniformly distributed between 0 and 1.
		rand = random.nextDouble();

		// Compare the random number to the cummulative probabilities calculated from the distribution.
		for( int i = 0; i < probs.size(); i++ ) {
			cumProb += probs.get( i );

			if( cumProb >= rand ) {
				return i;
			}
		}

		throw new ErrorException( "Probability distribution must sum to 1.0" );
	}

	/**
	 * Selects a continuous value in the range of the given bin values based on the given cumulative probabilites.
	 * The value is selected by chosing a bin and then interpolating.
	 * Check that the number of bins is equal to the number of probabilities
	 */
	public double selectValueFrom_UsingCumProbs( DoubleVector binValues, DoubleVector cumProbs ) {

		double rand;
		int index;
		int bin;
		double lower;
		double upper;
		double frac;

		if( !(binValues.size() == cumProbs.size()) ) {
			throw new ErrorException( "Number of probabilities does not match the number of bins" );
		}

		//  Check that the final cumulative probability is 1
		if( Math.abs( (cumProbs.lastElement() - 1.0) ) > 0.001 ) {
			throw new ErrorException( "Final cumulative probability is not 1.000" );
		}

		//  Select a random number from 0 to 1.
		rand = random.nextDouble();

		//  Determine the first bin whose cumulative probability is greater than or equal to the random number
		bin = 0;
		index = 1;
		while( !((bin) != 0) ) {
			if( cumProbs.get( index - 1 ) >= rand ) {
				bin = index;
			}
			else {
				index = (index + 1);
			}
		}

		//  If the cumulative probability is equal to the random number, then return the value of the bin (no interpolation)
		if( cumProbs.get( bin - 1 ) == rand ) {
			return binValues.get( bin - 1 );
		}

		//  Interpolate.  Assume the first bin contains values from 0 to the bin value
		if( bin == 1 ) {
			return ((rand / cumProbs.get( bin - 1 )) * binValues.get( bin - 1 ));
		}
		else {
			lower = binValues.get( (bin - 1) - 1 );
			upper = binValues.get( bin - 1 );
			frac = ((rand - cumProbs.get( (bin - 1) - 1 )) / (cumProbs.get( bin - 1 ) - cumProbs.get( (bin - 1) - 1 )));

			//  Return the interpolated value
			return (lower + (frac * (upper - lower)));
		}
	}

	public void initialiseWith( int seed ) {
		random.setSeed( seed );
	}

	public double getUniform() {
		return random.nextDouble();
	}

	public double getUniformFrom_To( double lower, double upper ) {
		return ((upper - lower) * random.nextDouble()) + lower;
	}
}
