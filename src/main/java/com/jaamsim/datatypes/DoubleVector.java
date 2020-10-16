/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
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

import com.jaamsim.basicsim.ErrorException;

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
	 * Reverse the elements position n to 0 and 0 to n
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
