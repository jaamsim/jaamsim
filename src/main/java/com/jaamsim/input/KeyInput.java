/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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
import java.util.HashMap;
import java.util.List;

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpression;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Class KeyInput for storing objects of class V (e.g. Double or DoubleVector), with an optional key of class K1
 */
public class KeyInput<K1 extends Entity, V> extends Input<V> {
	private Class<? extends Unit> unitType = DimensionlessUnit.class; // for when V is a SampleProvider
	private Entity thisEnt;

	protected double minValue = Double.NEGATIVE_INFINITY;
	protected double maxValue = Double.POSITIVE_INFINITY;
	private Class<K1> keyClass;
	private Class<V> valClass;
	private HashMap<K1,V> hashMap;
	private int minCount = 0;
	private int maxCount = Integer.MAX_VALUE;
	private V noKeyValue; // the value when there is no key

	public KeyInput(Class<K1> kClass, Class<V> vClass, String keyword, String cat, V def) {
		super(keyword, cat, def);
		keyClass = kClass;
		valClass = vClass;
		hashMap = new HashMap<>();
		noKeyValue = def;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void copyFrom(Input<?> in) {
		super.copyFrom(in);
		KeyInput<K1, V> inp = (KeyInput<K1, V>) in;
		hashMap = inp.hashMap;
		noKeyValue = inp.noKeyValue;
	}

	public void setUnitType(Class<? extends Unit> u) {
		unitType = u;
	}

	public void setEntity(Entity ent) {
		thisEnt = ent;
	}

	static <T> T parse(List<String> data, Entity thisEnt, Class<T> aClass, double minValue, double maxValue, int minCount, int maxCount, Class<? extends Unit> unitType) {

		if( aClass == Double.class ) {
			DoubleVector tmp = Input.parseDoubles(data, minValue, maxValue, unitType);
			Input.assertCount(tmp, 1);
			return aClass.cast( tmp.get(0));
		}

		if( aClass == DoubleVector.class ) {
			DoubleVector tmp = Input.parseDoubles(data, minValue, maxValue, unitType);
			Input.assertCountRange(tmp, minCount, maxCount);
			return aClass.cast( tmp );
		}

		if( Entity.class.isAssignableFrom(aClass) ) {
			Class<? extends Entity> temp = aClass.asSubclass(Entity.class);
			Input.assertCount(data, 1, 1);
			return aClass.cast( Input.parseEntity(data.get(0), temp) );
		}

		if( aClass == Boolean.class ) {
			Input.assertCount(data, 1);
			Boolean value = Boolean.valueOf(Input.parseBoolean(data.get(0)));
			return aClass.cast(value);
		}

		if( aClass == Integer.class ) {
			Input.assertCount(data, 1);
			Integer value = Input.parseInteger(data.get( 0 ), (int)minValue, (int)maxValue);
			return aClass.cast(value);
		}

		if( aClass == SampleProvider.class ) {

			// Try to parse as a constant value
			try {
				DoubleVector tmp = Input.parseDoubles(data, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, unitType);
				Input.assertCount(tmp, 1);
				return aClass.cast( new SampleConstant(unitType, tmp.get(0)) );
			}
			catch (InputErrorException e) {}

			// Try parsing a SampleProvider
			try {
				Input.assertCount(data, 1);
				Entity ent = Input.parseEntity(data.get(0), Entity.class);
				SampleProvider s = Input.castImplements(ent, SampleProvider.class);
				if( s.getUnitType() != UserSpecifiedUnit.class )
					Input.assertUnitsMatch(unitType, s.getUnitType());
				return aClass.cast(s);
			}
			catch (InputErrorException e) {}

			// Try parsing an expression
			try {
				String expString = data.get(0);
				return aClass.cast( new SampleExpression(expString, thisEnt, unitType) );
			}
			catch (ExpError e) {
				throw new InputErrorException(e.toString());
			}
		}

		if( aClass == IntegerVector.class ) {
			IntegerVector value = Input.parseIntegerVector(data, (int)minValue, (int)maxValue);
			if (value.size() < minCount || value.size() > maxCount)
				throw new InputErrorException(INP_ERR_RANGECOUNT, minCount, maxCount, data);
			return aClass.cast(value);
		}

		// TODO - parse other classes
		throw new InputErrorException("%s is not supported for parsing yet", aClass);
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		for (KeywordIndex each : kw.getSubArgs())
			this.innerParse(each);
	}

	private void innerParse(KeywordIndex kw) {
		ArrayList<String> input = new ArrayList<>(kw.numArgs());
		for (int i = 0; i < kw.numArgs(); i++)
			input.add(kw.getArg(i));

		// If an entity key is not provided, set the "no key" value
		Entity ent = Input.tryParseEntity( input.get( 0 ), Entity.class );
		if( ent == null || input.size() == 1 ) {
			try {
				noKeyValue = KeyInput.parse( input, thisEnt, valClass, minValue,maxValue, minCount, maxCount, unitType );
				return;
			}
			catch (InputErrorException e) {
				Input.parseEntity(input.get(0), Entity.class);
			}
		}

		// The input is of the form: <Key> <Value>
		// Determine the key(s)
		ArrayList<K1> list = Input.parseEntityList(input.subList(0, 1), keyClass, true);

		// Determine the value
		V val = KeyInput.parse( input.subList(1,input.size()), thisEnt, valClass, minValue, maxValue, minCount, maxCount, unitType );

		// Set the value for the given keys
		for( int i = 0; i < list.size(); i++ ) {
			hashMap.put( list.get(i), val );
		}
	}

	@Override
	public void setTokens(KeywordIndex kw) {
		isDef = false;

		String[] args = kw.getArgArray();

		// If there are previous inputs and existing or new keys
		if (args.length > 0) {
			if (valueTokens != null && hashMap.keySet().size() > 0) {
				this.appendTokens(args);
				return;
			}
		}

		valueTokens = args;
	}

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	@Override
	public V getValue() {
		return null;
	}

	public V getValueFor( K1 k1 ) {
		V val = hashMap.get( k1 );
		if( val == null ) {
			return noKeyValue;
		}
		else {
			return val;
		}
	}

	public int size() {
		return hashMap.size();
	}

	public void setValidCount(int count) {
		this.setValidCountRange(count, count);
	}

	public void setValidCountRange(int min, int max) {
		minCount = min;
		maxCount = max;
	}

	@Override
	public String getDefaultString() {
		return getDefaultStringForKeyInputs(unitType);
	}

	public ArrayList<V> getAllValues() {

		ArrayList<V> values = new ArrayList<>();

		for( V each : hashMap.values() ) {
			values.add(each);
		}
		values.add(noKeyValue);
		return values;
	}

	public ArrayList<K1> getAllKeys() {

		ArrayList<K1> keys = new ArrayList<>();

		for( K1 each : hashMap.keySet() ) {
			keys.add(each);
		}

		return keys;
	}

	@Override
	public void reset() {
		super.reset();
		hashMap.clear();
		noKeyValue = this.getDefaultValue();
	}

	public V getNoKeyValue() {
		return noKeyValue;
	}
}
