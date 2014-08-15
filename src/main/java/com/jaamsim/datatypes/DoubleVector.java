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
package com.jaamsim.datatypes;

import java.util.Arrays;

import com.jaamsim.basicsim.ErrorException;
import com.sandwell.JavaSimulation.Tester;

/**
 * This class stores double values in an array.
 */
public class DoubleVector {
	private int numElements;
	private int capIncrement;
	private double[] storage;

	/**
	 * Construct an empty vector, size 10, size increment 5.
	 */
	public DoubleVector() {
		this(10, 5);
	}

	/**
	 * Construct an empty vector with the given initial capacity and with its
	 * capacity increment equal to one.
	 */
	public DoubleVector(int initialCapacity) {
		this(initialCapacity, 1);
	}

	public DoubleVector(DoubleVector original) {
		numElements = original.numElements;
		capIncrement = 1;
		storage = new double[numElements];
		System.arraycopy(original.storage, 0, storage, 0, numElements);
	}

	/**
	 * Construct an empty vector with the given initial capacity and capacity
	 * increment.
	 */
	public DoubleVector(int initialCapacity, int capacityIncrement) {
		storage = new double[initialCapacity];
		capIncrement = capacityIncrement;
		numElements = 0;
	}

	private void ensureCapacity(int newCapacity) {
		if (storage.length >= newCapacity)
			return;

		if (storage.length + capIncrement >= newCapacity) {
			newCapacity = storage.length + capIncrement;
		}

		double[] copy = new double[newCapacity];
		System.arraycopy(storage, 0, copy, 0, storage.length);
		storage = copy;
	}

	/**
	 * Remove all of the doubles from this vector.
	 */
	public void clear() {
		numElements = 0;
	}

	/**
	 * Return the double at the given position in this vector.
	 */
	public double get(int index) {
		if (index < 0 || index >= numElements)
			throw new ArrayIndexOutOfBoundsException("Invalid index:" + index);
		else
			return storage[index];
	}

	/**
	 * Return the last double in this vector.
	 */
	public double lastElement() {
		return get(size() - 1);
	}

	/**
	 * Append the specified double to the end of the vector. This method is identical to
	 * the addElement() method.
	 */
	public void add( double value ) {
		add(numElements, value);
	}

	public void add(int index, double value) {
		if (index < 0 || index > numElements) {
			throw new ArrayIndexOutOfBoundsException("Invalid index:" + index);
		}
		ensureCapacity(numElements + 1);
		// If not a simple append, move later entries one further to the right
		if (index != numElements) {
			System.arraycopy(storage, index, storage, index + 1, numElements - index);
		}
		storage[index] = value;
		numElements++;
	}

	/**
	 * Replaces the element at the specified position in this DoubleVector with the
	 * specified element. Returns the element that was replaced in the DoubleVector.
	 *
	 * @param index index of element to replace
	 * @param value element to be stored at the specified position
	 * @return the element previously at the specified position
	 * @exception ArrayIndexOutOfBoundsException index out of range (index < 0 || index >= size())
	 */
	public double set( int index, double value ) {
		if (index < 0 || index >= numElements) {
			throw new ArrayIndexOutOfBoundsException("Invalid index:" + index);
		}

		double old = storage[index];
		storage[index] = value;
		return old;
	}

	/**
	 * Removes the element at the specified position in this DoubleVector. Shifts any
	 * subsequent elements to the left (subtracts one from their indices). Returns the
	 * element that was removed from the DoubleVector.
	 *
	 * @param index the index of the element to removed
	 * @return element that was removed
	 */
	public double remove( int index ) {
		if (index < 0 || index >= numElements) {
			throw new ArrayIndexOutOfBoundsException("Invalid index:" + index);
		}

		double old = storage[index];
		// If not removing last element, shift later elements one to the left
		if (index != numElements - 1) {
			System.arraycopy(storage, index + 1, storage, index, numElements - index - 1);
		}
		numElements--;
		return old;
	}

	/**
	 * Add the specified value to the value at the specified index.
	 */
	public void addAt( double value, int index ) {
		set(index, (get( index ) + value));
	}

	/**
	 * Subtract the specified value from the value at the specified index.
	 */
	public void subAt( double value, int index ) {
		set(index, (get( index ) - value));
	}

	/**
	 * Add the specified vector to this vector.
	 */
	public void add( DoubleVector vec ) {
		if( this.size() != vec.size() ) {
			throw new ErrorException( "Both vectors should have the same size" );
		}
		for( int i = 0; i < this.size(); i++ ) {
			addAt( vec.get( i ), i );
		}
	}

	/**
	 * Copy the specified vector to this vector.
	 */
	public void set(DoubleVector original) {
		clear();

		for (int i=0; i<original.size(); i++) {
			add(original.get(i));
		}
	}

	/**
	 * Return the sum of the doubles in this vector.
	 */
	public double sum() {
		// Create a temporary double
		double total = 0.0;

		// Add the double values to the total
		for( int i = 0; i < size(); i++ ) {
			total += get( i );
		}

		return total;
	}

	/**
	 * Fill the vector with the given number of entries of the given value.
	 */
	public void fillWithEntriesOf( int entries, double value ) {
		ensureCapacity(entries);
		Arrays.fill(storage, value);
		numElements = entries;
	}

	/**
	 * Return the number of doubles in this vector.
	 */
	public int size() {
		return numElements;
	}

	/**
	 * Return the current capacity of this vector.
	 */
	public int capacity() {
		return storage.length;
	}

	/**
	 * Return a string representation of this vector, containing the String
	 * representation of each element.
	 */
	/*
	 * public String toString() { return vector.toString(); }
	 */

	/**
	 * Return a string containing the contents of the DoubleVector.
	 */
	public String toString( String str ) {

		java.text.DecimalFormat formatter = new java.text.DecimalFormat( str );

		double x;
		StringBuilder out = new StringBuilder();

		if( size() > 0 ) {
			x = this.get( 0 );
			out.append("{ ");
			out.append(formatter.format(x));

			for( int i = 1; i < size(); i++ ) {
				x = this.get( i );
				out.append(", ");
				out.append(formatter.format(x));
			}
			out.append(" }");
		}
		else {
			out.append("{ }");
		}
		return out.toString();
	}

	@Override
	public String toString() {
		return ( this.toString( "") );
	}

	public double getMin() {
		if( size() < 1 ) {
			return 0.0;
		}
		else {
			double testValue = get( 0 );
			for( int i = 1; i < size(); i++ ) {
				testValue = Math.min( testValue, get( i ) );
			}
			return testValue;
		}
	}

	public double getMax() {
		if( size() < 1 ) {
			return 0.0;
		}
		else {
			double testValue = get( 0 );
			for( int i = 1; i < size(); i++ ) {
				testValue = Math.max( testValue, get( i ) );
			}
			return testValue;
		}
	}

	public int indexOf( double testValue ) {
		for (int i = 0; i < numElements; i++) {
			if (storage[i] == testValue) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Return the indices of elements closest to value, assuming items are in ascending order
	 */
	public int[] getClosestIndicesForValue( double value ) {
		int[] indices = new int[2];

		if( size() > 0 ) {
			for( int i = 0; i < size(); i++ ) {
				if( value <= get( i ) ) {

					if( i == 0 ) {
						indices[0] = 0;
						indices[1] = 0;
						return indices;
					}
					else {
						if( Tester.equalCheckTolerance( value, get( i ) ) ) {
							indices[0] = i;
							indices[1] = i;
							return indices;
						}
						else {
							indices[0] = i - 1;
							indices[1] = i;
							return indices;
						}
					}
				}
			}
			indices[0] = size() - 1;
			indices[1] = size() - 1;
			return indices;
		}
		else {
			indices[0] = -1;
			indices[1] = -1;
			return indices;
		}
	}

	/**
	 * Return the indices of elements closest to value, assuming items are in descending value
	 */
	public int[] getClosestIndicesForDescendingValue( double value ) {
		int[] indices = new int[2];

		if( size() > 0 ) {
			for( int i = 0; i < size(); i++ ) {
				if( value >= get( i ) ) {

					if( i == 0 ) {
						indices[0] = 0;
						indices[1] = 0;
						return indices;
					}
					else {
						if( Tester.equalCheckTolerance( value, get( i ) ) ) {
							indices[0] = i;
							indices[1] = i;
							return indices;
						}
						else {
							indices[0] = i - 1;
							indices[1] = i;
							return indices;
						}
					}
				}
			}
			indices[0] = size() - 1;
			indices[1] = size() - 1;
			return indices;
		}
		else {
			indices[0] = -1;
			indices[1] = -1;
			return indices;
		}
	}

	/**
	 * Select an index deterministically from the given probability distribution.
	 * The double buffer receiver of this method is the content of each
	 * index has been selected.
	 */
	public int selectIndexDeterministicallyUsingProbs( DoubleVector probs ) {

		double n;
		double x;
		double maxDifference;
		double cumProb;
		double expectedNumber;
		double difference;
		int selectedIndex;

		// Check the number of indices is equal to number of probabilities
		if( size() != probs.size() ) {
			throw new ErrorException( "Number of probabilities ("+probs.size()+") does not match the receiver ("+size()+")" );
		}

		// Calculate the total number of samples for this selection
		// (One more than the sum of the counts so far)
		n = 0;
		cumProb = 0.0;

		for( int i = 0; i < size(); i++ ) {
			n += get( i );
			cumProb += probs.get( i );
		}
		x = n + 1;

		if( Math.abs( cumProb - 1.0 ) > 0.001 ) {
			throw new ErrorException( "Probabilities do not sum to 1.000" );
		}

		// Loop through indices
		maxDifference = -100000.0;
		selectedIndex = -1;

		for( int i = 0; i < probs.size(); i++ ) {
			// Calculate the expected number of events for this probability
			expectedNumber = x * probs.get( i );

			// Select the index with the largest difference between the current
			// number and the expected number of events
			difference = expectedNumber - get( i );

			if( !(maxDifference + 1.0E-10 >= difference) ) {
				maxDifference = difference;
				selectedIndex = i;
			}
		}

		if( selectedIndex < 0 ) {
			throw new ErrorException( "Error in Method" );
		}
		return selectedIndex;
	}

	/**
	 * Select an index deterministically from the given probability distribution.
	 * The double buffer receiver of this method is the content of each index has been selected.
	 * Ignore is a list of all indices that cannot be selected.
	 */
	public int selectIndexDeterministicallyUsingProbsIgnore( DoubleVector probs, IntegerVector ignore ) {

		double n;
		double x;
		double maxDifference;
		double cumProb;
		double expectedNumber;
		double difference;
		int selectedIndex;

		// Check the number of indices is equal to number of probabilities
		if( size() != probs.size() ) {
			throw new ErrorException( "Number of probabilities ("+probs.size()+") does not match the receiver ("+size()+")" );
		}

		// Calculate the total number of samples for this selection
		// (One more than the sum of the counts so far)
		n = 0;
		cumProb = 0.0;

		for( int i = 0; i < size(); i++ ) {
			n += get( i );
			cumProb += probs.get( i );
		}
		x = n + 1;

		if( Math.abs( cumProb - 1.0 ) > 0.001 ) {
			throw new ErrorException( "Probabilities do not sum to 1.000" );
		}

		// Loop through indices
		maxDifference = -100000.0;
		selectedIndex = -1;

		for( int i = 0; i < probs.size(); i++ ) {

			if( ignore.contains(i) || probs.get(i) == 0.0 )
				continue;

			// Calculate the expected number of events for this probability
			expectedNumber = x * probs.get( i );

			// Select the index with the largest difference between the current
			// number and the expected number of events
			difference = expectedNumber - get( i );

			if( !(maxDifference + 1.0E-10 >= difference) ) {
				maxDifference = difference;
				selectedIndex = i;
			}
		}

		if( selectedIndex < 0 ) {
			throw new ErrorException( "Error in Method" );
		}
		return selectedIndex;
	}

	/**
	 * Return a list of indices representing the next values that would be selected using the given probabilities
	 */
	public IntegerVector getNextIndicesDeterministicallyUsingProbs( DoubleVector probs ) {

		// Check the number of indices is equal to number of probabilities
		if( size() != probs.size() ) {
			throw new ErrorException( "Number of probabilities ("+probs.size()+") does not match the receiver ("+size()+")" );
		}

		// Calculate the total number of samples for this selection (One more than the sum of the counts so far)
		double n = 0;
		double cumProb = 0.0;

		for( int i = 0; i < size(); i++ ) {
			n += get( i );
			cumProb += probs.get( i );
		}
		double x = n + 1;

		if( Math.abs( cumProb - 1.0 ) > 0.001 ) {
			throw new ErrorException( "Probabilities do not sum to 1.000" );
		}

		// Loop through indices
		IntegerVector sortedIndices = new IntegerVector();
		DoubleVector sortedDiffs = new DoubleVector();

		for( int i = 0; i < probs.size(); i++ ) {
			// Calculate the expected number of events for this probability
			double expectedNumber = x * probs.get( i );

			// Select the index with the largest difference between the current
			// number and the expected number of events
			double difference = expectedNumber - get( i );
			int destIndex = 0;
			for( int j=0; j < sortedDiffs.size(); j++ ) {
				if( sortedDiffs.get( j ) < difference ) {
					break;
				}
				destIndex++;
			}
			sortedDiffs.add( destIndex, difference );
			sortedIndices.add( destIndex, i );
		}

		return sortedIndices;
	}

	/**
	 * Select indices deterministically from the given probability distribution.
	 * The float buffer receiver of this method is the number of times that each
	 * index has been fully selected. The given split determines how many
	 * distinct indices will be selected, and by how much they will be
	 * incremented (the total increments add up to one). Check that the number
	 * of indices is equal to the number of probabilities
	 */
	public IntegerVector selectIndicesDeterministicallyUsingProbs_Split( DoubleVector probs, DoubleVector split ) {

		double n;
		double maxDifference;
		double cumProb;
		double expectedNumber;
		double difference;
		int selectedIndex;
		IntegerVector selectedIndices;

		if( !(this.size() == probs.size()) ) {
			throw new ErrorException( "Number of probabilities does not match the number the receiver" );
		}

		//  Check that there is a valid split
		if( split.size() == 0 ) {
			throw new ErrorException( "No split given" );
		}
		/*else {
			if( Math.abs( (split.sum() - 1.0) ) > 0.001 ) {
				throw new ErrorException( "Split does not sum to 1.0" );
			}
		}*/
		selectedIndices = new IntegerVector( 1, 1 );

		// Calculate the total number of samples for this selection so far
		n = 0.0;
		cumProb = 0.0;
		for( int j = 1; j <= this.size(); j++ ) {
			n = (n + this.get( j - 1 ));
			cumProb = (cumProb + probs.get( j - 1 ));
		}
		if( Math.abs( (cumProb - 1.0) ) > 0.001 ) {
			throw new ErrorException( "Probabilities do not sum to 1.000" );
		}
		for( int i = 1; i <= split.size(); i++ ) {
			n = (n + split.get( i - 1 ));

			// Loop through the indices
			maxDifference = -100000.0;
			selectedIndex = 0;
			for( int j = 1; j <= probs.size(); j++ ) {
				if( !((selectedIndices.indexOf( j ) + 1) != 0) ) {
					if( probs.get( j - 1 ) > 0.0 ) {

						// Calculate the expected number of events for this
						// probability
						expectedNumber = (n * probs.get( j - 1 ));

						// Select the index with the largest difference between
						// the current number and the expected number of events
						difference = (expectedNumber - this.get( j - 1 ));
						if( !(maxDifference + 1.0E-10 >= difference) ) {
							maxDifference = difference;
							selectedIndex = j;
						}
					}
				}
			}
			if( selectedIndex == 0 ) {
				throw new ErrorException( "Error in method" );
			}

			// Increment the count for this index
			this.addAt( split.get( i - 1 ), selectedIndex - 1 );
			selectedIndices.add(selectedIndex);
		}
		return selectedIndices;
	}

	/**
	 * Select indices deterministically from the given probability distribution.
	 * The float buffer receiver of this method is the number of times that each
	 * index has been fully selected. The given split determines how many
	 * distinct indices will be selected, and by how much they will be
	 * incremented (the total increments add up to one).  The same index can be selected twice.
	 */
	public IntegerVector selectIndicesDeterministicallyUsingProbs_SplitAllowDuplicateIndex( DoubleVector probs, DoubleVector split ) {

		double n;
		double maxDifference;
		double cumProb;
		double expectedNumber;
		double difference;
		int selectedIndex;
		IntegerVector selectedIndices;

		// Check that the number of indices is equal to the number of probabilities
		if( !(this.size() == probs.size()) ) {
			throw new ErrorException( "Number of probabilities does not match the number the receiver" );
		}

		//  Check that there is a valid split
		if( split.size() == 0 ) {
			throw new ErrorException( "No split given" );
		}
		/*else {
			if( Math.abs( (split.sum() - 1.0) ) > 0.001 ) {
				throw new ErrorException( "Split does not sum to 1.0" );
			}
		}*/
		selectedIndices = new IntegerVector( 1, 1 );

		// Calculate the total number of samples for this selection so far
		n = 0.0;
		cumProb = 0.0;
		for( int j = 1; j <= this.size(); j++ ) {
			n = (n + this.get( j - 1 ));
			cumProb = (cumProb + probs.get( j - 1 ));
		}
		if( Math.abs( (cumProb - 1.0) ) > 0.001 ) {
			throw new ErrorException( "Probabilities do not sum to 1.000" );
		}
		for( int i = 1; i <= split.size(); i++ ) {
			n = (n + split.get( i - 1 ));

			// Loop through the indices
			maxDifference = -100000.0;
			selectedIndex = 0;
			for( int j = 1; j <= probs.size(); j++ ) {
				if( probs.get( j - 1 ) > 0.0 ) {

					// Calculate the expected number of events for this
					// probability
					expectedNumber = (n * probs.get( j - 1 ));

					// Select the index with the largest difference between
					// the current number and the expected number of events
					difference = (expectedNumber - this.get( j - 1 ));
					if( !(maxDifference + 1.0E-10 >= difference) ) {
						maxDifference = difference;
						selectedIndex = j;
					}
				}
			}
			if( selectedIndex == 0 ) {
				throw new ErrorException( "Error in method" );
			}

			// Increment the count for this index
			this.addAt( split.get( i - 1 ), selectedIndex - 1 );
			selectedIndices.add(selectedIndex);
		}
		return selectedIndices;
	}

	/**
	 * Reverse the elements position n to 0 and 0 to n
	 * @return
	 */
	public void reverse() {
		int numSwaps = this.size() / 2;
		int swapIndex = this.size() - 1;

		for (int i = 0; i < numSwaps; i++, swapIndex--) {
			double temp = this.get(swapIndex);
			this.set(swapIndex, this.get(i));
			this.set(i, temp);
		}
	}

}
