/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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
package com.jaamsim.input;

import java.util.ArrayList;
import java.util.HashMap;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.Group;
/**
 * Class OneOrTwoKeyInput for storing objects of class V (e.g. Double or DoubleVector),
 * with one mandatory key of class K1 and one optional key of class K2
 */
public class OneOrTwoKeyListInput<K1 extends Entity, K2 extends Entity, V extends Entity> extends Input<ArrayList<V>> {

	private Class<K1> key1Class;
	private Class<K2> key2Class;
	private Class<V> valClass;
	private HashMap<K1,HashMap<K2,ArrayList<V>>> hashMap;
	private ArrayList<V> noKeyValue; // the value when there is no key

	public OneOrTwoKeyListInput(Class<K1> k1Class, Class<K2> k2Class, Class<V> vClass, String keyword, String cat, ArrayList<V> def) {
		super(keyword, cat, def);
		key1Class = k1Class;
		key2Class = k2Class;
		valClass = vClass;
		hashMap = new HashMap<>();
		noKeyValue = def;
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
			noKeyValue = Input.parseEntityList( input, valClass, true );
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
		ArrayList<V> val = Input.parseEntityList( input.subList(numKeys,input.size()), valClass, true );

		// Set the value for the given keys
		for( int i = 0; i < list.size(); i++ ) {
			HashMap<K2,ArrayList<V>> h1 = hashMap.get( list.get( i ) );
			if( h1 == null ) {
				h1 = new HashMap<>();
				hashMap.put( list.get( i ), h1 );
			}
			for( int j = 0; j < list2.size(); j++ ) {
				h1.put( list2.get(j), val );
			}
		}
	}

	public int size() {
		return hashMap.size();
	}

	@Override
	public ArrayList<V> getValue() {
		return null;
	}

	public ArrayList<V> getValueFor( K1 k1, K2 k2 ) {
		HashMap<K2,ArrayList<V>> h1 = hashMap.get( k1 );

		// Is k1 not in the table?
		if( h1 == null ) {
			return noKeyValue;
		}
		else {
			ArrayList<V> val = h1.get( k2 );

			// Is k2 not in the table?
			if( val == null ) {

				// Is null not in the table?
				ArrayList<V> val2 = h1.get( null );
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

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return NO_VALUE;

		if (defValue.size() == 0)
			return NO_VALUE;

		StringBuilder tmp = new StringBuilder(defValue.get(0).getName());
		for (int i = 1; i < defValue.size(); i ++) {
			tmp.append(SEPARATOR);
			tmp.append(defValue.get(i).getName());
		}

		return tmp.toString();
	}

	@Override
	public void reset() {
		super.reset();
		hashMap.clear();
		noKeyValue = this.getDefaultValue();
	}
}
