/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016 JaamSim Software Inc.
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.ExpResult.Iterator;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class ExpCollections {

	public static boolean isCollectionClass(Class<?> klass) {
		if (Map.class.isAssignableFrom(klass)) {
			return true;
		}
		if (List.class.isAssignableFrom(klass)) {
			return true;
		}
		if (DoubleVector.class.isAssignableFrom(klass)) {
			return true;
		}
		if (IntegerVector.class.isAssignableFrom(klass)) {
			return true;
		}
		if (klass.isArray()) {
			return true;
		}

		return false;
	}


	/**
	 * Wrap an existing Java collection in an expression collection
	 * @param obj - collection object to be wrapped
	 * @param ut - unit type
	 * @return - Read-only ExpResult representing the collection
	 */
	public static ExpResult wrapCollection(Object obj, Class<? extends Unit> ut) {
		if (obj instanceof Map) {
			MapCollection col = new MapCollection((Map<?,?>)obj, ut);
			return ExpResult.makeCollectionResult(col);
		}

		if (obj instanceof List) {
			ListCollection col = new ListCollection((List<?>)obj, ut);
			return ExpResult.makeCollectionResult(col);
		}

		if (obj.getClass().isArray()) {
			ArrayCollection col = new ArrayCollection(obj, ut);
			return ExpResult.makeCollectionResult(col);
		}

		if (obj instanceof DoubleVector) {
			DoubleVectorCollection col = new DoubleVectorCollection((DoubleVector)obj, ut);
			return ExpResult.makeCollectionResult(col);
		}

		if (obj instanceof IntegerVector) {
			IntegerVectorCollection col = new IntegerVectorCollection((IntegerVector)obj, ut);
			return ExpResult.makeCollectionResult(col);
		}

		assert false;
		return null;
	}

	/**
	 * Create an expression collection that may be assigned into (aka: written).
	 * This obeys a single level of copy-on-write semantics if the original object is marked as constant
	 * @param vals - The original values for the collection (may be an ArrayList or Map)
	 * @param constExp - Is the original a constant?
	 */
	public static ExpResult makeAssignableArrrayCollection(ArrayList<ExpResult> vals, boolean constExp) {
		return ExpResult.makeCollectionResult(new AssignableArrayCollection(vals, constExp));
	}

	public static ExpResult makeAssignableMapCollection(Map<String, ExpResult> vals, boolean constExp) {
		return ExpResult.makeCollectionResult(new AssignableMapCollection(vals, constExp));
	}

	public static ExpResult appendCollections(ExpResult.Collection c0, ExpResult.Collection c1) throws ExpError {
		ArrayList<ExpResult> res = new ArrayList<>();
		ExpResult.Iterator it = c0.getIter();
		while (it.hasNext()) {
			ExpResult val = c0.index(it.nextKey());
			res.add(val);
		}
		it = c1.getIter();
		while (it.hasNext()) {
			ExpResult val = c1.index(it.nextKey());
			res.add(val);
		}
		return ExpResult.makeCollectionResult(new AssignableArrayCollection(res, false));
	}

	public static ExpResult appendToCollection(ExpResult.Collection col, ExpResult val) throws ExpError {
		ArrayList<ExpResult> res = new ArrayList<>();
		ExpResult.Iterator it = col.getIter();
		while (it.hasNext()) {
			ExpResult v = col.index(it.nextKey());
			res.add(v);
		}
		res.add(val);

		return ExpResult.makeCollectionResult(new AssignableArrayCollection(res, false));
	}

	private static class ListCollection implements ExpResult.Collection {

		private static class Iter implements ExpResult.Iterator {

			private int next = 0;
			private final List<?> list;
			public Iter(List<?> l) {
				this.list = l;
			}

			@Override
			public boolean hasNext() {
				return next < list.size();
			}

			@Override
			public ExpResult nextKey() throws ExpError {
				ExpResult ret = ExpResult.makeNumResult(next + 1, DimensionlessUnit.class);
				next++;
				return ret;
			}
		}

		@Override
		public Iterator getIter() {
			return new Iter(list);
		}

		private final List<?> list;
		private final Class<? extends Unit> unitType;

		public ListCollection(List<?> l, Class<? extends Unit> ut) {
			this.list = l;
			this.unitType = ut;
		}

		@Override
		public ExpResult index(ExpResult index) throws ExpError {
			if (index.type != ExpResType.NUMBER) {
				throw new ExpError(null, 0, "ArrayList  is not being indexed by a number");
			}

			int indexVal = (int)index.value - 1; // Expressions use 1-base arrays

			if (indexVal >= list.size()  || indexVal < 0) {
				return ExpResult.makeNumResult(0, unitType); // TODO: Is this how we want to handle this case?
			}
			Object val = list.get(indexVal);

			return ExpEvaluator.getResultFromObject(val, unitType);
		}

		@Override
		public int getSize() {
			return list.size();
		}

		@Override
		public ExpResult.Collection assign(ExpResult key, ExpResult value) throws ExpError {
			throw new ExpError(null, 0, "Can not assign to built in collection");
		}

		@Override
		public String getOutputString() {
			try {
				StringBuilder sb = new StringBuilder();
				sb.append("{");
				for (int i = 0; i < list.size(); ++i) {
					ExpResult val = index(ExpResult.makeNumResult(i+1, DimensionlessUnit.class));
					sb.append(val.getOutputString());
					if (i < list.size() -1) {
						sb.append(", ");
					}
				}
				sb.append("}");
				return sb.toString();

			} catch (ExpError err) {
				return String.format("An error occurred: %s", err.getMessage());
			}
		}
		@Override
		public ExpResult.Collection getCopy() {
			return this;
		}
	}

	private static class ArrayCollection implements ExpResult.Collection {

		private final Object array;
		private final Class<? extends Unit> unitType;

		public ArrayCollection(Object a, Class<? extends Unit> ut) {
			this.array = a;
			this.unitType = ut;
		}

		private static class Iter implements ExpResult.Iterator {

			private int next = 0;
			private final Object array;

			public Iter(Object a) {
				array = a;
			}

			@Override
			public boolean hasNext() {
				return next < Array.getLength(array);
			}

			@Override
			public ExpResult nextKey() throws ExpError {
				ExpResult ret = ExpResult.makeNumResult(next + 1, DimensionlessUnit.class);
				next++;
				return ret;
			}
		}

		@Override
		public Iterator getIter() {
			return new Iter(array);
		}

		@Override
		public ExpResult index(ExpResult index) throws ExpError {
			if (index.type != ExpResType.NUMBER) {
				throw new ExpError(null, 0, "ArrayList  is not being indexed by a number");
			}

			int indexVal = (int)index.value - 1;

			int length = Array.getLength(array);

			if (indexVal >= length  || indexVal < 0) {
				return ExpResult.makeNumResult(0, unitType); // TODO: Is this how we want to handle this case?
			}

			Class<?> componentClass = array.getClass().getComponentType();

			if (!componentClass.isPrimitive()) {
				// This is an object type so we can use the rest of the reflection system as usual
				Object val = Array.get(array, indexVal);
				return ExpEvaluator.getResultFromObject(val, unitType);
			}

			if (    componentClass == Double.TYPE ||
			        componentClass == Float.TYPE ||
			        componentClass == Long.TYPE ||
			        componentClass == Integer.TYPE ||
			        componentClass == Short.TYPE ||
			        componentClass == Byte.TYPE ||
			        componentClass == Character.TYPE) {
				// This is a numeric type and should be convertible to double
				return ExpResult.makeNumResult(Array.getDouble(array, indexVal), unitType);
			}
			if (componentClass == Boolean.TYPE) {
				// Convert boolean to 1 or 0
				double val = (Array.getBoolean(array, indexVal)) ? 1.0 : 0.0;
				return ExpResult.makeNumResult(val, unitType);
			}
			throw new ExpError(null, 0, "Unknown type in array");
		}
		@Override
		public int getSize() {
			return Array.getLength(array);
		}
		@Override
		public ExpResult.Collection assign(ExpResult key, ExpResult value) throws ExpError {
			throw new ExpError(null, 0, "Can not assign to built in collection");
		}

		@Override
		public String getOutputString() {
			try {
				StringBuilder sb = new StringBuilder();
				sb.append("{");
				for (int i = 0; i < Array.getLength(array); ++i) {
					ExpResult val = index(ExpResult.makeNumResult(i+1, DimensionlessUnit.class));
					sb.append(val.getOutputString());
					if (i < Array.getLength(array) -1) {
						sb.append(", ");
					}
				}
				sb.append("}");
				return sb.toString();

			} catch (ExpError err) {
				return String.format("An error occurred: %s", err.getMessage());
			}
		}
		@Override
		public ExpResult.Collection getCopy() {
			return this;
		}

	}

	private static class DoubleVectorCollection implements ExpResult.Collection {

		private final DoubleVector vector;
		private final Class<? extends Unit> unitType;

		public DoubleVectorCollection(DoubleVector v, Class<? extends Unit> ut) {
			this.vector = v;
			this.unitType = ut;
		}

		private static class Iter implements ExpResult.Iterator {

			private int next = 0;
			private final DoubleVector vector;

			public Iter(DoubleVector v) {
				this.vector = v;
			}
			@Override
			public boolean hasNext() {
				return next < vector.size();
			}

			@Override
			public ExpResult nextKey() throws ExpError {
				ExpResult ret = ExpResult.makeNumResult(next + 1, DimensionlessUnit.class);
				next++;
				return ret;
			}
		}
		@Override
		public Iterator getIter() {
			return new Iter(vector);
		}

		@Override
		public ExpResult index(ExpResult index) throws ExpError {

			if (index.type != ExpResType.NUMBER) {
				throw new ExpError(null, 0, "DoubleVector is not being indexed by a number");
			}

			int indexVal = (int)index.value - 1; // Expressions use 1-base arrays

			if (indexVal >= vector.size() || indexVal < 0) {
				return ExpResult.makeNumResult(0, unitType); // TODO: Is this how we want to handle this case?
			}

			Double value = vector.get(indexVal);
			return ExpResult.makeNumResult(value, unitType);
		}
		@Override
		public int getSize() {
			return vector.size();
		}
		@Override
		public ExpResult.Collection assign(ExpResult key, ExpResult value) throws ExpError {
			throw new ExpError(null, 0, "Can not assign to built in collection");
		}

		@Override
		public String getOutputString() {
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			for (int i = 0; i < vector.size(); ++i) {
				sb.append(vector.get(i+1)*Unit.getDisplayedUnitFactor(unitType));
				sb.append(" ");
				sb.append(Unit.getDisplayedUnit(unitType));
				if (i < vector.size()) {
					sb.append(", ");
				}
			}
			sb.append("}");
			return sb.toString();
		}

		@Override
		public ExpResult.Collection getCopy() {
			return this;
		}
	}

	private static class IntegerVectorCollection implements ExpResult.Collection {

		private final IntegerVector vector;
		private final Class<? extends Unit> unitType;

		public IntegerVectorCollection(IntegerVector v, Class<? extends Unit> ut) {
			this.vector = v;
			this.unitType = ut;
		}

		private static class Iter implements ExpResult.Iterator {
			private int next = 0;
			private final IntegerVector vector;

			public Iter(IntegerVector v) {
				this.vector = v;
			}

			@Override
			public boolean hasNext() {
				return next < vector.size();
			}

			@Override
			public ExpResult nextKey() throws ExpError {
				ExpResult ret = ExpResult.makeNumResult(next + 1, DimensionlessUnit.class);
				next++;
				return ret;
			}
		}
		@Override
		public Iterator getIter() {
			return new Iter(vector);
		}

		@Override
		public ExpResult index(ExpResult index) throws ExpError {

			if (index.type != ExpResType.NUMBER) {
				throw new ExpError(null, 0, "IntegerVector is not being indexed by a number");
			}

			int indexVal = (int)index.value - 1; // Expressions use 1-base arrays

			if (indexVal >= vector.size() || indexVal < 0) {
				return ExpResult.makeNumResult(0, unitType); // TODO: Is this how we want to handle this case?
			}

			Integer value = vector.get(indexVal);
			return ExpResult.makeNumResult(value, unitType);
		}
		@Override
		public int getSize() {
			return vector.size();
		}
		@Override
		public ExpResult.Collection assign(ExpResult key, ExpResult value) throws ExpError {
			throw new ExpError(null, 0, "Can not assign to built in collection");
		}

		@Override
		public String getOutputString() {
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			for (int i = 0; i < vector.size(); ++i) {
				sb.append(vector.get(i+1)*Unit.getDisplayedUnitFactor(unitType));
				sb.append(" ");
				sb.append(Unit.getDisplayedUnit(unitType));
				if (i < vector.size()) {
					sb.append(", ");
				}
			}
			sb.append("}");
			return sb.toString();
		}

		@Override
		public ExpResult.Collection getCopy() {
			return this;
		}
	}

	private static class MapCollection implements ExpResult.Collection {

		private final Map<?,?> map;
		private final Class<? extends Unit> unitType;

		public MapCollection(Map<?,?> m, Class<? extends Unit> ut) {
			this.map = m;
			this.unitType = ut;
		}

		private static class Iter implements ExpResult.Iterator {

			java.util.Iterator<?> keySetIt;
			public Iter(Map<?,?> map) {
				keySetIt = map.keySet().iterator();
			}

			@Override
			public boolean hasNext() {
				return keySetIt.hasNext();
			}

			@Override
			public ExpResult nextKey() throws ExpError {
				Object mapKey = keySetIt.next();

				return ExpEvaluator.getResultFromObject(mapKey, DimensionlessUnit.class);
			}
		}
		@Override
		public Iterator getIter() {
			return new Iter(map);
		}

		@Override
		public ExpResult index(ExpResult index) throws ExpError {
			Object key;
			switch (index.type) {
			case ENTITY:
				if (index.entVal == null) {
					throw new ExpError(null, 0, "Trying use a null entity as a key");
				}

				key = index.entVal;
				break;
			case NUMBER:
				key = Double.valueOf(index.value);
				break;
			case STRING:
				key = index.stringVal;
				break;
			case COLLECTION:
				throw new ExpError(null, 0, "Can not index with a collection");
			default:
				assert(false);
				key = null;
				break;
			}
			Object val = map.get(key);
			if (val == null) {
				return ExpResult.makeNumResult(0, unitType); // TODO: Is this how we want to handle this case?
			}
			return ExpEvaluator.getResultFromObject(val, unitType);
		}
		@Override
		public int getSize() {
			return map.size();
		}
		@Override
		public ExpResult.Collection assign(ExpResult key, ExpResult value) throws ExpError {
			throw new ExpError(null, 0, "Can not assign to built in collection");
		}
		@Override
		public String getOutputString() {
			try {
				StringBuilder sb = new StringBuilder();
				sb.append("{");
				Iterator it = getIter();
				while(it.hasNext()) {
					ExpResult index = it.nextKey();
					sb.append(index.getOutputString());
					sb.append(" = ");
					sb.append(index(index).getOutputString());
					if (it.hasNext()) {
						sb.append(", ");
					}
				}
				sb.append("}");
				return sb.toString();

			} catch (ExpError err) {
				return String.format("An error occurred: %s", err.getMessage());
			}
		}

		@Override
		public ExpResult.Collection getCopy() {
			return this;
		}
	}
	private static class AssignableArrayCollection implements ExpResult.Collection {

		private final ArrayList<ExpResult> list;
		private final boolean isConstExp;

		public AssignableArrayCollection(ArrayList<ExpResult> vals, boolean constExp) {
			isConstExp = constExp;
			if (isConstExp) {
				list = vals;
			} else {
				list = new ArrayList<>(vals);
			}
		}

		@Override
		public ExpResult index(ExpResult index) throws ExpError {
			if (index.type != ExpResType.NUMBER) {
				throw new ExpError(null, 0, "ArrayList is not being indexed by a number");
			}

			int indexVal = (int)index.value - 1; // Expressions use 1-base arrays

			if (indexVal >= list.size()  || indexVal < 0) {
				return ExpResult.makeNumResult(0, DimensionlessUnit.class); // TODO: Is this how we want to handle this case?
			}
			return list.get(indexVal);
		}

		@Override
		public ExpResult.Collection assign(ExpResult index, ExpResult value) throws ExpError {

			if (isConstExp) {
				// This version is a constant, and therefore shareable. Create a new modifiable copy.
				ExpResult.Collection copy = getCopy();
				return copy.assign(index,  value);
			}

			if (index.type != ExpResType.NUMBER) {
				throw new ExpError(null, 0, "Assignment is not being indexed by a number");
			}
			int indexVal = (int)index.value - 1; // Expressions use 1-base arrays
			if (indexVal < 0) {
				throw new ExpError(null, 0, "Attempting to assign to a negative number: %d", indexVal);
			}
			if (indexVal >= list.size()) {
				// This is a dynamically expanding list, so fill in until we get to the index
				ExpResult filler = ExpResult.makeNumResult(0, DimensionlessUnit.class);
				list.ensureCapacity(indexVal+1);
				for (int i = list.size(); i <= indexVal; ++i) {
					list.add(filler);
				}
			}
			list.set(indexVal, value);
			return this;
		}

		private static class Iter implements ExpResult.Iterator {

			private int next = 0;
			private final List<?> list;
			public Iter(List<?> l) {
				this.list = l;
			}

			@Override
			public boolean hasNext() {
				return next < list.size();
			}

			@Override
			public ExpResult nextKey() throws ExpError {
				ExpResult ret = ExpResult.makeNumResult(next + 1, DimensionlessUnit.class);
				next++;
				return ret;
			}
		}

		@Override
		public Iterator getIter() {
			return new Iter(list);
		}

		@Override
		public int getSize() {
			return list.size();
		}
		@Override
		public String getOutputString() {
			try {
				StringBuilder sb = new StringBuilder();
				sb.append("{");
				for (int i = 0; i < list.size(); ++i) {
					ExpResult val = index(ExpResult.makeNumResult(i+1, DimensionlessUnit.class));
					sb.append(val.getOutputString());
					if (i < list.size() -1) {
						sb.append(", ");
					}
				}
				sb.append("}");
				return sb.toString();

			} catch (ExpError err) {
				return String.format("An error occurred: %s", err.getMessage());
			}
		}

		@Override
		public ExpResult.Collection getCopy() {
			return new AssignableArrayCollection(list, false);
		}

	}
	private static class AssignableMapCollection implements ExpResult.Collection {

		private final Map<String, ExpResult> map;
		private final boolean isConstExp;

		public AssignableMapCollection(Map<String, ExpResult> initMap, boolean constExp) {
			isConstExp = constExp;
			if (isConstExp) {
				map = initMap;
			} else {
				map = new HashMap<>(initMap);
			}
		}

		@Override
		public ExpResult index(ExpResult index) throws ExpError {
			if (index.type != ExpResType.STRING) {
				throw new ExpError(null, 0, "Map is not being indexed by a string");
			}

			String indexVal = index.stringVal;

			ExpResult res = map.get(indexVal);

			if (res == null) {
				return ExpResult.makeNumResult(0, DimensionlessUnit.class); // TODO: Is this how we want to handle this case?
			}
			return res;
		}

		@Override
		public ExpResult.Collection assign(ExpResult index, ExpResult value) throws ExpError {

			if (isConstExp) {
				// This version is a constant, and therefore shareable. Create a new modifiable copy.
				ExpResult.Collection copy = getCopy();
				return copy.assign(index,  value);
			}

			if (index.type != ExpResType.STRING) {
				throw new ExpError(null, 0, "Assignment is not being indexed by a string");
			}

			String indexVal = index.stringVal;

			map.put(indexVal, value);
			return this;
		}

		private static class Iter implements ExpResult.Iterator {

			java.util.Iterator<?> keySetIt;
			public Iter(Map<?,?> map) {
				keySetIt = map.keySet().iterator();
			}

			@Override
			public boolean hasNext() {
				return keySetIt.hasNext();
			}

			@Override
			public ExpResult nextKey() throws ExpError {
				Object mapKey = keySetIt.next();

				return ExpEvaluator.getResultFromObject(mapKey, DimensionlessUnit.class);
			}
		}

		@Override
		public Iterator getIter() {
			return new Iter(map);
		}

		@Override
		public int getSize() {
			return map.size();
		}
		@Override
		public String getOutputString() {
			try {
				StringBuilder sb = new StringBuilder();
				sb.append("{");
				Iterator it = getIter();
				while(it.hasNext()) {
					ExpResult index = it.nextKey();
					sb.append(index.getOutputString());
					sb.append(" = ");
					sb.append(index(index).getOutputString());
					if (it.hasNext()) {
						sb.append(", ");
					}
				}
				sb.append("}");
				return sb.toString();

			} catch (ExpError err) {
				return String.format("An error occurred: %s", err.getMessage());
			}
		}

		@Override
		public ExpResult.Collection getCopy() {
			return new AssignableMapCollection(map, false);
		}

	}

}
