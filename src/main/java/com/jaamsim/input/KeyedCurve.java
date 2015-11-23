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
package com.jaamsim.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public abstract class KeyedCurve<T> {

	private class Key {
		public double time;
		public T val;
	}
	private ArrayList<Key> keys;
	private boolean isSorted;

	public KeyedCurve() {
		keys = new ArrayList<>();
		isSorted = true;
	}

	public void addKey(double time, T val) {
		Key k = new Key();
		k.time = time;
		k.val = val;
		keys.add(k);
		isSorted = false;
	}

	public T getValAtTime(double time) {
		if (!isSorted) {
			sortKeys();
		}

		// Treat NaN as negative infinity, as it will at least return a valid number
		if (Double.isNaN(time)) {
			time = Double.NEGATIVE_INFINITY;
		}
		if (keys.size() == 0) {
			 return null;
		}
		if (keys.size() == 1) {
			return keys.get(0).val;
		}
		// Are we ahead of the beginning?
		if (time <= keys.get(0).time) {
			return keys.get(0).val;
		}
		// Are we past the end?
		if (time >= keys.get(keys.size()-1).time) {
			return keys.get(keys.size()-1).val;
		}

		// Use a binary search to find the segment we want
		int start = 0;
		int end = keys.size() - 1;
		while (end - start > 1) {
			int pivot = (end + start)/2;
			double pivotTime = keys.get(pivot).time;
			if (pivotTime == time) {
				return keys.get(pivot).val;
			}
			if (pivotTime > time) {
				end = pivot;
			} else {
				start = pivot;
			}
		}

		double startTime = keys.get(start).time;
		double endTime = keys.get(end).time;
		double ratio = (time - startTime) / (endTime - startTime);

		return interpVal(keys.get(start).val, keys.get(end).val, ratio);
	}

	public boolean hasKeys() {
		return keys.size() != 0;
	}

	protected abstract T interpVal(T val0, T val1, double ratio);

	private void sortKeys() {
		Collections.sort(keys, sorter);
		isSorted = true;
	}

	private class KeySorter implements Comparator<Key>{

		@Override
		public int compare(Key arg0, Key arg1) {
			return Double.compare(arg0.time, arg1.time);
		}
	}
	private final KeySorter sorter = new KeySorter();
}
