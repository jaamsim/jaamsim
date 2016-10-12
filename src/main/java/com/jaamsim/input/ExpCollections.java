package com.jaamsim.input;

import java.lang.reflect.Array;
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


	public static ExpResult getCollection(Object obj, Class<? extends Unit> ut) {
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

	private static class ListCollection implements ExpResult.Collection {

		private class Iter implements ExpResult.Iterator {

			private int next = 0;

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
			return new Iter();
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
	}

	private static class ArrayCollection implements ExpResult.Collection {

		private final Object array;
		private final Class<? extends Unit> unitType;

		public ArrayCollection(Object a, Class<? extends Unit> ut) {
			this.array = a;
			this.unitType = ut;
		}

		private class Iter implements ExpResult.Iterator {

			private int next = 0;

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
			return new Iter();
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
	}

	private static class DoubleVectorCollection implements ExpResult.Collection {

		private final DoubleVector vector;
		private final Class<? extends Unit> unitType;

		public DoubleVectorCollection(DoubleVector v, Class<? extends Unit> ut) {
			this.vector = v;
			this.unitType = ut;
		}

		private class Iter implements ExpResult.Iterator {

			private int next = 0;

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
			return new Iter();
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

	}

	private static class IntegerVectorCollection implements ExpResult.Collection {

		private final IntegerVector vector;
		private final Class<? extends Unit> unitType;

		public IntegerVectorCollection(IntegerVector v, Class<? extends Unit> ut) {
			this.vector = v;
			this.unitType = ut;
		}

		private class Iter implements ExpResult.Iterator {

			private int next = 0;

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
			return new Iter();
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

	}

	private static class MapCollection implements ExpResult.Collection {

		private final Map<?,?> map;
		private final Class<? extends Unit> unitType;

		public MapCollection(Map<?,?> m, Class<? extends Unit> ut) {
			this.map = m;
			this.unitType = ut;
		}

		private class Iter implements ExpResult.Iterator {

			java.util.Iterator<?> keySetIt = map.keySet().iterator();

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
			return new Iter();
		}

		@Override
		public ExpResult index(ExpResult index) throws ExpError {
			Object key;
			switch (index.type) {
			case ENTITY:
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

	}

}
