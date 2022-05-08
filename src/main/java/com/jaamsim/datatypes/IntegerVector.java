/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2022 JaamSim Software Inc.
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
package com.jaamsim.datatypes;

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

	public int[] toArray() {
		return storage;
	}

}
