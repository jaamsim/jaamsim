/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2005-2011 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;
import java.util.Random;

import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;

/**
 * Probability distribution class.
 */
public class ProbabilityDistribution extends Entity
implements SampleProvider {
	private static final ArrayList<ProbabilityDistribution> allInstances;


	@Keyword(description = "The probability of a non-zero value.",
	         example = "ProbDist1 NonZeroProb { 0.2 }")
	protected DoubleInput nonZeroProb;

	@Keyword(description = "A list of probabilities whose size and ordering must be the same as the list of outcomes in ValueList.",
	         example = "ProbDist1 ProbList { 0.1 0.5 0.3 0.1 }")
	protected DoubleVector probList; // list of probabilities

	@Keyword(description = "A list of numerical outcomes corresponding to the probabilities specified in ValueProb.  " +
			        "Either this and ValueList OR CumValueList and CumValueProb must be specified.",
	         example = "ProbDist1 ValueList { 0.1 0.5 0.3 0.1 }")
	protected DoubleVector valueList; // list of bin values associated with probabilities

	protected Vector objectList;  // list of objects associated with probabilities
	protected IntegerVector counts; // list of number of occurrences for each value

	@Keyword(description = "A list of cumulative probabilities that must start with 0 and end with 1, " +
			        "corresponding to the list of outcomes in CumValueList.",
	         example = "ProbDist1 CumProbList { 0.0 0.1 0.3 0.5 0.8 1.0 }")
	protected DoubleVector cumProbList; // list of cumulative probabilities

	@Keyword(description = "A list of numerical outcomes corresponding to the cumulative probabilities specified in CumValueProb.  " +
	        "Either this and CumValueList OR ValueList and ValueProb must be specified.",
     example = "ProbDist1 CumValueList { 0.1 0.5 0.3 0.1 }")
	protected DoubleVector cumValueList; // list of values associated with cumulative probabilities
	protected Vector cumObjectList; // list of objects associated with cumulative probabilities

	protected Random randomGenerator; // random generator for picking values

	@Keyword(description = "An integer greater than or equal to 0 specifying the seed for the random number generator." +
			        "  If empty or zero, then the values are chosen deterministically and Interpolate must be set to FALSE.",
     example = "ProbDist1 RandomSeed { 0 }")
	protected int randomSeed; // random seed, default is 0 for selecting values deterministically

	@Keyword(description = "A Boolean value.  If TRUE, values sampled from this ProbabilityDistribution are interpolated from ValueList " +
			        "to create a continuous distribution. ",
	         example = "ProbDist1 Interpolate { FALSE }")
	protected boolean interpolate; // if true, then the return value is interpolated from the bin values, default is false
	protected boolean initialized;

	@Keyword(description = "A multiplicative factor applied to the values returned by the ProbabilityDistribution object.",
	         example = "ProbDist1 ValueFactor { 1.0 }")
	protected double valueFactor; // factor applied to the values returned by the distribution

	protected double expectedValue;

	static {
		allInstances = new ArrayList<ProbabilityDistribution>();
	}

	{
		addEditableKeyword( "CumValueList", "", "",      false, "Key Inputs" );
		addEditableKeyword( "CumProbList",  "", "",      false, "Key Inputs" );
		addEditableKeyword( "Interpolate",  "", "FALSE", false, "Key Inputs" );
		addEditableKeyword( "RandomSeed",   "", "0",     false, "Key Inputs" );
		addEditableKeyword( "ValueList",    "", "",      false, "Key Inputs" );
		addEditableKeyword( "ProbList",     "", "",      false, "Key Inputs" );
		addEditableKeyword( "ValueFactor",  "", "1.0",   false, "Key Inputs" );

		nonZeroProb = new DoubleInput("NonZeroProb", "Key Inputs", 1.0d);
		nonZeroProb.setValidRange(0.0d, 1.0d);
		this.addInput(nonZeroProb, true);
	}

	public ProbabilityDistribution() {
		allInstances.add(this);
		probList = new DoubleVector( 1, 1 );
		valueList = new DoubleVector( 1, 1 );
		objectList = new Vector( 1, 1 );
		counts = new IntegerVector( 1, 1 );

		cumProbList = new DoubleVector( 1, 1 );
		cumValueList = new DoubleVector( 1, 1 );
		cumObjectList = new Vector( 1, 1 );

		randomGenerator = new Random( 2345 );
		randomSeed = 0;
		interpolate = false;
		initialized = false;

		expectedValue = 0.0;
		valueFactor = 1.0;
	}

	// initialize a new Probability distribution which always returns a single double
	public ProbabilityDistribution( double aDouble ) {
		this();

		probList.add ( 1.0 );
		valueList.add( aDouble );

	}

	public static ArrayList<? extends ProbabilityDistribution> getAll() {
		return allInstances;
	}

	@Override
	public void kill() {
		super.kill();
		allInstances.remove(this);
	}

	// ******************************************************************************************
	// INPUT
	// ******************************************************************************************

	/**
	 * Interpret the input data in the given buffer of strings corresponding to the given keyword.
	 */
	@Override
	public void readData_ForKeyword(StringVector data, String keyword)
	throws InputErrorException {

		if( "ProbList".equalsIgnoreCase( keyword ) ) {
			DoubleVector temp = Input.parseDoubleVector(data, 0.0d, 1.0d);
			Input.assertSumTolerance(temp, 1.0d, 0.001d);
			probList = temp;
			return;
		}
		if( "CumProbList".equalsIgnoreCase( keyword ) ) {
			Input.assertCountRange(data, 2, Integer.MAX_VALUE);
			DoubleVector temp = Input.parseDoubleVector(data, 0.0d, 1.0d);

			for( int i = 0; i < temp.size() - 1; i++ ) {
				if (temp.get(i) > temp.get(i + 1))
					throw new InputErrorException("Expecting values in non-decreasing order");
			}
			if (temp.get(0) != 0.0)
				throw new InputErrorException("Expecting first value equal to 0.0");

			if (temp.lastElement() != 1.0)
				throw new InputErrorException("Expecting last value equal to 1.0");

			cumProbList = temp;
			return;
		}

		if( "ValueList".equalsIgnoreCase( keyword ) ) {

			// Check for a list of numbers
			if (Tester.isDouble(data.get(0))) {
				valueList = Tester.parseDoubleVector( data );
			}
			// Check for a list of objects
			else {
				ArrayList<Entity> temp = Input.parseEntityList(data, Entity.class, false);
				objectList.addAll(temp);
			}
			return;
		}

		if( "CumValueList".equalsIgnoreCase( keyword ) ) {
			// Check for a list of numbers
			if (Tester.isDouble(data.get(0))) {
				cumValueList = Tester.parseDoubleVector( data );
			}
			// Check for a list of objects
			else {
				ArrayList<Entity> temp = Input.parseEntityList(data, Entity.class, false);
				cumObjectList.addAll(temp);
			}
			return;
		}
		if( "RandomSeed".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 1);
			randomSeed = Input.parseInteger(data.get(0), 0, Integer.MAX_VALUE);
			return;
		}
		if( "Interpolate".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 1);
			interpolate = Input.parseBoolean(data.get(0));
			return;
		}
		if( "ValueFactor".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 1);
			valueFactor = Input.parseDouble(data.get(0), 1e-15d, Double.POSITIVE_INFINITY);
			return;
		}

		super.readData_ForKeyword( data, keyword );
	}

	// ******************************************************************************************
	// INITIALIZATION METHODS
	// ******************************************************************************************

	/**
	 * Reset the random generator (required for model re-starts)
	 */
	public void reset() {
		randomGenerator = new Random( 2345 );

		if( objectList.size() == 0 ) {
			// Initialize the random generator
			if( randomSeed > 0 ) {
				randomGenerator.setSeed( randomSeed );
			}

			if( randomSeed == 0 ) {
				if( interpolate ) {
					throw new InputErrorException( "Interpolation requires a random seed" );
				}
			}
		}
	}

	/**
	 * Initialize the probability distribution.
	 */
	@Override
	public void earlyInit() {

		// If already initialized, just reset the counts
		if( initialized ) {
			if( objectList.size() == 0 ) {
				counts.fillWithEntriesOf( valueList.size(), 0 );
			}
			else {
				counts.fillWithEntriesOf( objectList.size(), 0 );
			}
			return;
		}

		if( probList.size() == 0 && cumProbList.size() == 0 ) {
			throw new InputErrorException( getName() + " must specify probability or cumulative probability distribution" );
		}

		// If the probabilities and values were not specified, then create them from the cumulative probabilities and values
		if( probList.size() == 0 ) {

			// If objects are used, do nothing
			if( objectList.size() == 0 ) {

				// Check cumulative probabilities and values have the same size
				if( cumProbList.size() != cumValueList.size() ) {
					throw new InputErrorException( "Number of cumulative probabilities ("+cumProbList.size()+") must equal number of cumulative values ("+cumValueList.size()+")" );
				}
				for( int i = 0; i < cumProbList.size() - 1; i++ ) {
					probList.add((cumProbList.get( i + 1 ) - cumProbList.get( i )));
					valueList.add((( cumValueList.get( i + 1 ) + cumValueList.get( i ) ) / 2.0));
				}
			}
			else {
				if( cumObjectList.size() != cumProbList.size() ) {
					throw new InputErrorException( "Number of cumulative probabilities ("+cumProbList.size()+") must equal number of objects ("+cumObjectList.size()+")" );
				}
			}
		}

		// If the cumulative probabilities and values were not specified, then create them from the probabilities and values
		if( cumProbList.size() == 0 ) {

			// If objects are used, do nothing
			if( cumObjectList.size() == 0 ) {

				if( interpolate ) {
					throw new InputErrorException( "Must specify cumulative probabilities for interpolation" );
				}
				// Check cumulative probabilities and values have the same size
				if( objectList.size() == 0 ) {
					if( probList.size() != valueList.size() ) {
						throw new InputErrorException( "Number of probabilities must equal number of values" );
					}
				}
				else {
					if( objectList.size() != probList.size() ) {
						throw new InputErrorException( "Number of probabilities ("+probList.size()+") must equal number of objects ("+objectList.size()+")" );
					}
				}
				/*cumProbList.addElement( 0.0 );
				double cumProb = 0.0;
				for( int i = 0; i < probList.size(); i++ ) {
					cumProb += probList.get( i );
					cumProbList.addElement( cumProb );
				}*/
			}
		}

		if( objectList.size() == 0 ) {

			// Adjust for the probability of a value being zero, if required
			if (nonZeroProb.getValue() < 1.0d) {
				if( probList.size() > 0 ) {
					probList.add(0, (1.0 - nonZeroProb.getValue()));
					for( int i = 1; i < probList.size(); i++ ) {
						probList.set(i, (probList.get( i ) * nonZeroProb.getValue()));
					}
				}
				valueList.add(0, 0.0);
			}

			counts.fillWithEntriesOf( valueList.size(), 0 );

			// Calculate the expected value
			expectedValue = 0.0;
			for( int i = 0; i < probList.size(); i++ ) {
				expectedValue += probList.get( i ) * valueList.get( i );
			}
		}
		else {
			counts.fillWithEntriesOf( objectList.size(), 0 );
		}
		initialized = true;
	}

	public DoubleVector getProbList() {
		return probList;
	}

	public void setProbList( DoubleVector v ) {
		probList = v;
	}

	public DoubleVector getCumProbList() {
		return cumProbList;
	}

	public DoubleVector getValueList() {
		return valueList;
	}

	public Vector getObjectList() {
		return objectList;
	}

	public void setValueList( DoubleVector v ) {
		valueList = v;
	}

	public void setObjectList( Vector v ) {
		objectList = v;
	}

	public void setRandomSeed( int s ) {
		randomSeed = s;
		randomGenerator.setSeed( randomSeed );
	}

	// ******************************************************************************************
	// WORKING METHODS
	// ******************************************************************************************

	/**
	 * Return the expected value for the probability distribution
	 */
	public double getExpectedValue() {
		return expectedValue * valueFactor;
	}

	/**
	 * Return the minimum value for the probability distribution
	 */
	public double getMinimumValue() {
		if( interpolate ) {
			return cumValueList.getMin() * valueFactor;
		}
		else {
			return valueList.getMin() * valueFactor;
		}
	}

	/**
	 * Return the maximum value for the probability distribution
	 */
	public double getMaximumValue() {
		if( interpolate ) {
			return cumValueList.getMax() * valueFactor;
		}
		else {
			return valueList.getMax() * valueFactor;
		}
	}

	/**
	 * Return the next value for the probability distribution
	 */
	public double nextValue() {

		// If the random seed is 0, then select the value deterministically without interpolation
		if( randomSeed == 0 ) {
			int index = counts.selectIndexDeterministicallyUsingProbs( probList );
			return valueList.get( index - 1 ) * valueFactor;
		}
		// Otherwise, select the value using the random number generator
		else {
			// Is there interpolation?
			if( interpolate ) {
				return this.selectValueFrom_UsingCumProbs( cumValueList, cumProbList ) * valueFactor;
			}
			else {
				return this.selectValueFrom_UsingProbs( valueList, probList ) * valueFactor;
			}
		}
	}

	/**
	 * Return the next object for the probability distribution
	 */
	public Object nextObject() {

		// If the random seed is 0, then select the value deterministically without interpolation
		if( randomSeed == 0 ) {
			int index = counts.selectIndexDeterministicallyUsingProbs( probList );
			return objectList.get( index - 1 );
		}
		// Otherwise, select the value using the random number generator
		else {
			return this.selectObjectFrom_UsingProbs( objectList, probList );
		}
	}

	/**
	 * Use the random number generator to select a bin value from the given probability distribution.
	 */
	public double selectValueFrom_UsingProbs( DoubleVector binValues, DoubleVector probs ) {

		// Select a random number that is uniformly distributed between 0 and 1.
		double rand = randomGenerator.nextDouble();

		// Should the value be zero?
		if (rand < (1.0d - nonZeroProb.getValue())) {
			return 0.0;
		}
		else {
			// Adjust the random number so that it is again between 0 and 1
			rand = (rand - (1.0d - nonZeroProb.getValue())) / nonZeroProb.getValue();
		}

		// Compare the random number to the cummulative probabilities calculated from the distribution.
		double cumProb = 0.0;
		for( int i = 0; i < probs.size(); i++ ) {
			cumProb += probs.get( i );

			if( cumProb >= rand ) {
				return binValues.get( i );
			}
		}

		throw new ErrorException( "Probability distribution must sum to 1.0" );
	}

	/**
	 * Use the random number generator to select a bin value from the given probability distribution.
	 */
	public Object selectObjectFrom_UsingProbs( Vector binValues, DoubleVector probs ) {

		// Select a random number that is uniformly distributed between 0 and 1.
		double rand = randomGenerator.nextDouble();

		// Compare the random number to the cummulative probabilities calculated from the distribution.
		double cumProb = 0.0;
		for( int i = 0; i < probs.size(); i++ ) {
			cumProb += probs.get( i );

			if( cumProb >= rand ) {
				return binValues.get( i );
			}
		}

		throw new ErrorException( "Probability distribution must sum to 1.0" );
	}

	/**
	 * Use the random generator to select a value from the given cumulative probabilites.
	 * The value is selected by linearly interpolating between the given bin values.
	 */
	public double selectValueFrom_UsingCumProbs( DoubleVector binValues, DoubleVector cumProbs ) {

		// Check that the number of bins is equal to the number of probabilities
		if( binValues.size() != cumProbs.size() ) {
			throw new ErrorException( "Number of probabilities ("+binValues.size()+") does not match the number of bins ("+cumProbs.size()+")" );
		}

		//  Check that the final cumulative probability is 1
		if( Math.abs( (cumProbs.lastElement() - 1.0) ) > 0.001 ) {
			throw new ErrorException( "Final cumulative probability is not 1.000" );
		}

		//  Select a random number from 0 to 1.
		double rand = randomGenerator.nextDouble();

		// Should the value be zero?
		if (rand < (1.0d - nonZeroProb.getValue())) {
			return 0.0;
		}
		else {
			// Adjust the random number so that it is again between 0 and 1
			rand = (rand - (1.0d - nonZeroProb.getValue())) / nonZeroProb.getValue();
		}

		//  Determine the first bin whose cumulative probability is greater than or equal to the random number
		int bin = -1;
		int index = 0;
		while( bin == -1 ) {
			if( cumProbs.get( index ) >= rand ) {
				bin = index;
			}
			else {
				index += 1;
			}
		}

		//  If the cumulative probability is equal to the random number, then return the value of the bin (no interpolation)
		if( cumProbs.get( bin ) == rand ) {
			return binValues.get( bin );
		}

		//  Interpolate.  Assume the first bin contains values from 0 to the bin value
		if( bin == 0 ) {
			return ( rand / cumProbs.get( bin ) ) * binValues.get( bin );
		}
		else {
			double lower = binValues.get( bin - 1 );
			double upper = binValues.get( bin );
			double frac = ( rand - cumProbs.get( bin - 1 ) ) / ( cumProbs.get( bin ) - cumProbs.get( bin - 1 ) );

			//  Return the interpolated value
			return lower + (frac * (upper - lower));
		}
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		// Required because ProbabilityDistribution implements SampleProvider
		// Assumes all existing ProbabilityDistributions were for time
		return TimeUnit.class;
	}

	@Override
	public double getNextSample(double simTime) {
		// Assumes all existing ProbabilityDistributions were for time in hours
		// Return time in seconds
		return nextValue() * 3600.0 ;
	}
}
