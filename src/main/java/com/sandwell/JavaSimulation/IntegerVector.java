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

import java.util.Arrays;

/**
 * This class stores integer values in an array.
 */
public class IntegerVector {
	private int numElements;
	private int capIncrement;
	private int[] storage;

	/**
	 * Construct an empty vector, size 10, size increment 5.
	 */
	public IntegerVector() {
		this(10, 5);
	}

	/**
	 * Construct an empty vector with the given initial capacity and with its
	 * capacity increment equal to one.
	 */
	public IntegerVector(int initialCapacity) {
		this(initialCapacity, 1);
	}

	/**
	 * Construct a copy of the given Integer Vector
	 */
	public IntegerVector( IntegerVector original ) {
		numElements = original.numElements;
		capIncrement = 1;
		storage = new int[numElements];
		System.arraycopy(original.storage, 0, storage, 0, numElements);
	}

	/**
	 * Construct an empty vector with the given initial capacity and capacity
	 * increment.
	 */
	public IntegerVector(int initialCapacity, int capacityIncrement) {
		storage = new int[initialCapacity];
		capIncrement = capacityIncrement;
		numElements = 0;
	}

	private void ensureCapacity(int newCapacity) {
		if (storage.length >= newCapacity)
			return;

		if (storage.length + capIncrement >= newCapacity) {
			newCapacity = storage.length + capIncrement;
		}

		int[] copy = new int[newCapacity];
		System.arraycopy(storage, 0, copy, 0, storage.length);
		storage = copy;
	}

	/**
	 * Remove all of the integers from this vector.
	 */
	public void clear() {
		numElements = 0;
	}

	/**
	 * Return the integer at the given position in this vector.
	 */
	public int get(int index) {
		if (index < 0 || index >= numElements)
			throw new ArrayIndexOutOfBoundsException("Invalid index:" + index);
		else
			return storage[index];
	}

	/**
	 * Append the specified integer to the end of the vector. This method is
	 * identical to the addElement() method.
	 */
	public void add(int value) {
		add(numElements, value);
	}

	public void add(int index, int value) {
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
	 * Set the component at the given index of this vector to be the given
	 * integer.
	 */
	public int set(int index, int value) {
		if (index < 0 || index >= numElements) {
			throw new ArrayIndexOutOfBoundsException("Invalid index:" + index);
		}

		int old = storage[index];
		storage[index] = value;
		return old;
	}

	public int remove(int index) {
		if (index < 0 || index >= numElements) {
			throw new ArrayIndexOutOfBoundsException("Invalid index:" + index);
		}

		int old = storage[index];
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
	public void addAt( int value, int index ) {
		set( index, get( index ) + value );
	}

	/**
	 * Subtract the specified value from the value at the specified index.
	 */
	public void subAt( int value, int index ) {
		set( index, get( index ) - value );
	}

	/**
	 * Return the sum of the integers in this vector.
	 */
	public int sum() {
		// Create a temporary int
		int total = 0;

		// Add the int values to the total
		for( int i = 0; i < size(); i++ ) {
			total += get( i );
		}

		return total;
	}

	/**
	 * Fill the vector with the given number of entries of the given value.
	 */
	public void fillWithEntriesOf( int entries, int value ) {
		ensureCapacity(entries);
		Arrays.fill(storage, value);
		numElements = entries;
	}

	/**
	 * Return the number of ints in this vector.
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
	 * Tests if the specified integer is a component in this vector.
	 */
	public boolean contains( int value ) {
		if (indexOf(value) == -1)
			return false;
		else
			return true;
	}

	/**
	 * Return a string containing the contents of the IntegerVector.
	 */
	@Override
	public String toString() {
		StringBuilder out = new StringBuilder("{");

		for (int i = 0; i < size(); i++) {
			if (i == 0)
				out.append(" ");
			else
				out.append(", ");
			out.append(get(i));
		}
		out.append(" }");

		return out.toString();
	}

	/**
	 * Select an index deterministically from the given probability distribution.
	 * The integer buffer receiver of this method is the number of times that each
	 * index has been selected.
	 */
	public int selectIndexDeterministicallyUsingProbs( DoubleVector probs ) {

		int n;
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
		x = (n + 1);

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

		// Increment the count for this index
		n = get( selectedIndex );
		set( selectedIndex, n + 1 );
		return selectedIndex + 1;
	}

	public int getMin() {
		if( size() < 1 ) {
			return 0;
		}
		else {
			int testValue = get( 0 );
			for( int i = 1; i < size(); i++ ) {
				testValue = Math.min( testValue, get( i ) );
			}
			return testValue;
		}
	}

	public int getMax() {
		if( size() < 1 ) {
			return 0;
		}
		else {
			int testValue = get( 0 );
			for( int i = 1; i < size(); i++ ) {
				testValue = Math.max( testValue, get( i ) );
			}
			return testValue;
		}
	}

	/**
	 * Searches for the first occurence of the given argument, testing for equality using the equals method
	 */
	public int indexOf(int testValue) {
		for (int i = 0; i < numElements; i++) {
			if (storage[i] == testValue) {
				return i;
			}
		}
		return -1;
	}

    /**
     * Change to the next permutation in lexicographic order
     */
    public void nextPermutation() {

    	int i = size() - 1;
    	while( get( i - 1 ) >= get( i ) ) {
    		i--;
    	}

    	int j = size();
    	while( get( j - 1 ) <= get( i - 1 ) ) {
    		j--;
    	}

    	// Swap values at positions (i-1) and (j-1)
    	int temp = get( i - 1 );
    	set( i - 1, get( j - 1 ) );
    	set( j - 1, temp );

    	i++;
    	j = size();

    	while( i < j ) {
        	temp = get( i - 1 );
        	set( i - 1, get( j - 1 ) );
        	set( j - 1, temp );
		    i++;
		    j--;
    	}
	}
}
