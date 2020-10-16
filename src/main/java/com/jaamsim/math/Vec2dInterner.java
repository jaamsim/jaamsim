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
package com.jaamsim.math;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Vec2dInterner is a container type used to 'intern' Vec2d instances hopefully saving space on repeating entries
 * @author matt.chudleigh
 *
 */
public class Vec2dInterner {

	private int nextIndex = 0;
	private ArrayList<Vec2d> orderedValues = new ArrayList<>();

	private static class VecWrapper {
		public Vec2d val;
		public int index;
		public VecWrapper(Vec2d v) {
			val = v;
		}

		@Override
		public boolean equals(Object o) {
			VecWrapper vw = (VecWrapper)o;
			return val.equals2(vw.val);
		}

		@Override
		public int hashCode() {
			int hash = 0;
			hash ^= Double.valueOf(val.x).hashCode();
			hash ^= Double.valueOf(val.y).hashCode() * 3;
			return hash;
		}
	}

	private HashMap<VecWrapper, VecWrapper> map = new HashMap<>();

	/**
	 * intern will return a pointer to a Vec2d (which may differ from input 'v') that is mathematically equal but
	 * may be a shared object. Any value returned by intern should be defensively copied before being modified
	 */
	public Vec2d intern(Vec2d v) {
		VecWrapper wrapped = new VecWrapper(v);
		VecWrapper interned = map.get(wrapped);
		if (interned != null) {
			return interned.val;
		}

		// This wrapped value will be stored
		wrapped.index = nextIndex++;
		orderedValues.add(v);
		map.put(wrapped, wrapped);
		return v;
	}

	public Vec2d getValueForIndex(int i) {
		return orderedValues.get(i);
	}

	public int getIndexForValue(Vec2d v) {
		VecWrapper wrapped = new VecWrapper(v);
		return map.get(wrapped).index;
	}

	public int getMaxIndex() {
		return orderedValues.size();
	}

}
