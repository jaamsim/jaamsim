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
import java.util.Collections;
import java.util.HashMap;

import com.jaamsim.basicsim.Entity;

/**
 * Class TwoKeyListInput for storing a list of entities of class V, with optional keys of class K1 and K2
 */
public class TwoKeyListInput<K1 extends Entity, K2 extends Entity, V extends Entity> extends ListInput<ArrayList<V>> {

	private Class<K1> key1Class;
	private Class<K2> key2Class;
	private Class<V> valClass;
	private HashMap<K1,HashMap<K2,ArrayList<V>>> hashMap;
	private ArrayList<V> noKeyValue;

	public TwoKeyListInput(Class<K1> k1Class, Class<K2> k2Class, Class<V> vClass, String keyword, String cat, ArrayList<V> def) {
		super(keyword, cat, def);
		key1Class = k1Class;
		key2Class = k2Class;
		valClass = vClass;
		hashMap = new HashMap<>();
		noKeyValue = def;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void copyFrom(Input<?> in) {
		super.copyFrom(in);
		TwoKeyListInput<K1, K2, V> inp = (TwoKeyListInput<K1, K2, V>) in;
		hashMap = inp.hashMap;
		noKeyValue = inp.noKeyValue;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		ArrayList<String> input = new ArrayList<>(kw.numArgs());
		for (int i = 0; i < kw.numArgs(); i++)
			input.add(kw.getArg(i));
		// If two entity keys are not provided, set the "no key" value
		Entity ent1 = Input.tryParseEntity( input.get( 0 ), Entity.class );
		Entity ent2 = null;
		if( input.size() > 1 ) {
			ent2 = Input.tryParseEntity( input.get( 1 ), Entity.class );
		}
		if( ent1 == null || ent2 == null ) {
			noKeyValue = Input.parseEntityList( input, valClass, true );
			return;
		}

		// The input is of the form: <Key1> <Key2> <Value>
		// Determine the key(s)
		ArrayList<K1> list = Input.parseEntityList(input.subList(0, 1), key1Class, true);
		ArrayList<K2> list2 = Input.parseEntityList(input.subList(1, 2), key2Class, true);

		// Determine the value
		ArrayList<V> val = Input.parseEntityList( input.subList(2,input.size()), valClass, true );

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

	@Override
	public ArrayList<V> getValue() {
		return null;
	}

	public ArrayList<V> getValueFor( K1 k1, K2 k2 ) {
		HashMap<K2,ArrayList<V>> h1 = hashMap.get( k1 );
		if( h1 == null ) {
			return noKeyValue;
		}
		else {
			ArrayList<V> val = h1.get( k2 );
			if( val == null ) {
				return noKeyValue;
			}
			else {
				return val;
			}
		}
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return "";

		if (defValue.size() == 0)
			return "";

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

	@Override
	public int getListSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<>();
		for(V each: Entity.getClonesOfIterator(valClass) ) {
			if(each.testFlag(Entity.FLAG_GENERATED))
				continue;

			list.add(each.getName());
		}
		Collections.sort(list);
		return list;
	}
}
