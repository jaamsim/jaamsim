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

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.Group;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
/**
 * Class OneOrTwoKeyInput for storing objects of class V (e.g. Double or DoubleVector),
 * with one mandatory key of class K1 and one optional key of class K2
 */
public class OneOrTwoKeyInput<K1 extends Entity, K2 extends Entity, V> extends Input<V> {

	private Class<? extends Unit> unitType = DimensionlessUnit.class;
	protected double minValue = Double.NEGATIVE_INFINITY;
	protected double maxValue = Double.POSITIVE_INFINITY;
	private Class<K1> key1Class;
	private Class<K2> key2Class;
	private Class<V> valClass;
	private HashMap<K1,HashMap<K2,V>> hashMap;
	private int minCount = 0;
	private int maxCount = Integer.MAX_VALUE;
	private V noKeyValue; // the value when there is no key

	public OneOrTwoKeyInput(Class<K1> k1Class, Class<K2> k2Class, Class<V> vClass, String keyword, String cat, V def) {
		super(keyword, cat, def);
		key1Class = k1Class;
		key2Class = k2Class;
		valClass = vClass;
		hashMap = new HashMap<>();
		noKeyValue = def;
	}

	public void setUnitType(Class<? extends Unit> units) {
		unitType = units;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void copyFrom(Input<?> in) {
		super.copyFrom(in);
		OneOrTwoKeyInput<K1, K2, V> inp = (OneOrTwoKeyInput<K1, K2, V>) in;
		hashMap = inp.hashMap;
		noKeyValue = inp.noKeyValue;
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

		// If one entity key is not provided, set the default value
		Entity ent1 = Input.tryParseEntity( input.get( 0 ), key1Class );

		// could be a group
		if( ent1 == null ) {
			ent1 = Input.tryParseEntity( input.get( 0 ), Group.class );
		}

		if( ent1 == null ) {
			noKeyValue = Input.parse( input, valClass, null, minValue, maxValue, minCount, maxCount, unitType );
			return;
		}

		// Determine the keys
		ArrayList<K1> list = Input.parseEntityList(input.subList(0, 1), key1Class, true);
		ArrayList<K2> list2;

		// If ent2 is null, assume the line is of the form <Key1> <Value>
		// The second key was not given.  Use null as the second key.
		int numKeys;
		Entity ent2 = Input.tryParseEntity( input.get( 1 ), key2Class );

		// could be a group
		if( ent2 == null ) {
			ent2 = Input.tryParseEntity( input.get( 1 ), Group.class );
		}

		if( ent2 == null ) {
			numKeys = 1;
			list2 = new ArrayList<>();
			list2.add( null );
		}
		else {
			// Otherwise assume the line is of the form <Key1> <Key2> <Value>
			// The second key was given.  Store the second key.
			numKeys = 2;
			list2 = Input.parseEntityList(input.subList(1, 2), key2Class, true);
		}

		// Determine the value
		V val = Input.parse( input.subList(numKeys,input.size()), valClass, null, minValue, maxValue, minCount, maxCount, unitType );

		// Set the value for the given keys
		for( int i = 0; i < list.size(); i++ ) {
			HashMap<K2,V> h1 = hashMap.get( list.get( i ) );
			if( h1 == null ) {
				h1 = new HashMap<>();
				hashMap.put( list.get( i ), h1 );
			}
			for( int j = 0; j < list2.size(); j++ ) {
				h1.put( list2.get( j ), val );
			}
		}
	}

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	public int size() {
		return hashMap.size();
	}

	@Override
	public V getValue() {
		return null;
	}

	public V getValueFor( K1 k1, K2 k2 ) {
		HashMap<K2,V> h1 = hashMap.get( k1 );

		// Is k1 not in the table?
		if( h1 == null ) {
			return noKeyValue;
		}
		else {
			V val = h1.get( k2 );

			// Is k2 not in the table?
			if( val == null ) {

				// Is null not in the table?
				V val2 = h1.get( null );
				if( val2 == null ) {

					// Return the default value
					return noKeyValue;
				}
				else {

					// Return the value for (k1,null) i.e. when k1 is specified alone
					return val2;
				}
			}
			else {

				// Return the value for (k1,k2)
				return val;
			}
		}
	}

	/**
	 * Return a list of all stored values in the table
	 * @return
	 */
	public ArrayList<V> getAllValues() {

		ArrayList<V> values = new ArrayList<>();

		for( HashMap<K2,V> each : hashMap.values() ) {
			values.addAll( each.values() );
		}

		values.add( noKeyValue );

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
	public String getDefaultString() {
		return getDefaultStringForKeyInputs(unitType, null);
	}

	@Override
	public void reset() {
		super.reset();
		hashMap.clear();
		noKeyValue = this.getDefaultValue();
	}
}
