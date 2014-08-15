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

import com.sandwell.JavaSimulation.Entity;

/**
 * Class KeyListInput for storing a list of entities of class V, with an optional key of class K1
 */
public class KeyListInput<K1 extends Entity, V extends Entity> extends Input<ArrayList<V>> {

	private Class<K1> keyClass;
	private Class<V> valClass;
	private HashMap<K1,ArrayList<V>> hashMap;

	public KeyListInput(Class<K1> kClass, Class<V> vClass, String keyword, String cat, ArrayList<V> def) {
		super(keyword, cat, def);
		keyClass = kClass;
		valClass = vClass;
		hashMap = new HashMap<K1,ArrayList<V>>();
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		for (KeywordIndex each : kw.getSubArgs())
			this.innerParse(each);
	}

	private void innerParse(KeywordIndex kw) {
		ArrayList<String> input = new ArrayList<String>(kw.numArgs());
		for (int i = 0; i < kw.numArgs(); i++)
			input.add(kw.getArg(i));

		ArrayList<K1> list;
		try {
			// Determine the key(s)
			list = Input.parseEntityList(input.subList(0, 1), keyClass, true);
		}
		catch (InputErrorException e) {
			// A key was not provided.  Set the default value
			ArrayList<V> defValue = Input.parseEntityList( input, valClass, true );
			this.setDefaultValue( defValue );
			return;
		}

		// The input is of the form: <Key> <value1 value2 value3...>
		// Determine the value
		ArrayList<V> val = Input.parseEntityList( input.subList(1,input.size()), valClass, true );

		// Set the value for the given keys
		for( int i = 0; i < list.size(); i++ ) {
			hashMap.put( list.get(i), val );
		}
	}

	@Override
	public ArrayList<V> getValue() {
		return null;
	}

	public ArrayList<V> getValueFor( K1 k1 ) {
		ArrayList<V> val = hashMap.get( k1 );
		if( val == null ) {
			return this.getDefaultValue();
		}
		else {
			return val;
		}
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return NO_VALUE;

		if (defValue.size() == 0)
			return NO_VALUE;

		StringBuilder tmp = new StringBuilder(defValue.get(0).getInputName());
		for (int i = 1; i < defValue.size(); i++) {
			tmp.append(SEPARATOR);
			tmp.append(defValue.get(i).getInputName());
		}
		return tmp.toString();
	}
}
