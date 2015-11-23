/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2005-2011 Ausenco Engineering Canada Inc.
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
 * This class stores boolean values in an array.
 */
public class BooleanVector {
	private int numElements;
	private int capIncrement;
	private boolean[] storage;

	/**
	 * Construct an empty vector, size 10, size increment 5.
	 */
	public BooleanVector() {
		this(10, 5);
	}

	/**
	 * Construct an empty vector with the given initial capacity and with its
	 * capacity increment equal to one.
	 */
	public BooleanVector(int initialCapacity) {
		this(initialCapacity, 1);
	}

	/**
	 * Construct an empty vector with the given initial capacity and capacity
	 * increment.
	 */
	public BooleanVector(int initialCapacity, int capacityIncrement) {
		storage = new boolean[initialCapacity];
		capIncrement = capacityIncrement;
		numElements = 0;
	}

	private void ensureCapacity(int newCapacity) {
		if (storage.length >= newCapacity)
			return;

		if (storage.length + capIncrement >= newCapacity) {
			newCapacity = storage.length + capIncrement;
		}

		boolean[] copy = new boolean[newCapacity];
		System.arraycopy(storage, 0, copy, 0, storage.length);
		storage = copy;
	}

	/**
	 * Remove all of the booleans from this vector.
	 */
	public void clear() {
		numElements = 0;
	}

	/**
	 * Return the boolean at the given position in this vector.
	 */
	public boolean get(int index) {
		if (index < 0 || index >= numElements)
			throw new ArrayIndexOutOfBoundsException("Invalid index:" + index);
		else
			return storage[index];
	}

	/**
	 * Append the specified boolean to the end of the vector. This method is identical to
	 * the add() method.
	 */
	public void add(boolean value) {
		add(numElements, value);
	}

	public void add(int index, boolean value) {
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
	 * Replaces the element at the specified position in this BooleanVector with the
	 * specified element. Returns the element that was replaced in the BooleanVector.
	 *
	 * @param index index of element to replace
	 * @param value element to be stored at the specified position
	 * @return the element previously at the specified position
	 * @exception ArrayIndexOutOfBoundsException index out of range (index < 0 || index >= size())
	 */
	public boolean set(int index, boolean value) {
		if (index < 0 || index >= numElements) {
			throw new ArrayIndexOutOfBoundsException("Invalid index:" + index);
		}

		boolean old = storage[index];
		storage[index] = value;
		return old;
	}

	/**
	 * Removes the element at the specified position in this BooleanVector. Shifts any
	 * subsequent elements to the left (subtracts one from their indices). Returns the
	 * element that was removed from the BooleanVector.
	 *
	 * @param index the index of the element to removed
	 * @return element that was removed
	 */
	public boolean remove(int index) {
		if (index < 0 || index >= numElements) {
			throw new ArrayIndexOutOfBoundsException("Invalid index:" + index);
		}

		boolean old = storage[index];
		// If not removing last element, shift later elements one to the left
		if (index != numElements - 1) {
			System.arraycopy(storage, index + 1, storage, index, numElements - index - 1);
		}
		numElements--;
		return old;
	}

	/**
	 * Fill the vector with the given number of entries of the given value.
	 */
	public void fillWithEntriesOf(int entries, boolean value) {
		ensureCapacity(entries);
		Arrays.fill(storage, value);
		numElements = entries;
	}

	/**
	 * Return the number of booleans in this vector.
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
	 * Return a string containing the contents of the BooleanVector.
	 */
	@Override
	public String toString() {
		StringBuilder out = new StringBuilder("{");

		for (int i = 0; i < size(); i++) {
			if (i == 0)
				out.append(" ");
			else
				out.append(", ");
			out.append(storage[i]);
		}
		out.append(" }");
		return out.toString();
	}
}
