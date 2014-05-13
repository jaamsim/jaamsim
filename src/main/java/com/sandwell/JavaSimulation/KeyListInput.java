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
package com.sandwell.JavaSimulation;

import java.util.ArrayList;
import java.util.HashMap;

import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;

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
	public void parse(StringVector input)
	throws InputErrorException {
		ArrayList<StringVector> split = InputAgent.splitStringVectorByBraces(input);
		for (StringVector each : split)
			this.innerParse(each);
	}

	private void innerParse(StringVector input) {
		ArrayList<K1> list;
		try {
			// Determine the key(s)
			list = Input.parseEntityList(input.subString(0, 0), keyClass, true);
		}
		catch (InputErrorException e) {
			// A key was not provided.  Set the default value
			ArrayList<V> defValue = Input.parseEntityList( input.subString(0,input.size()-1), valClass, true );
			this.setDefaultValue( defValue );
			return;
		}

		// The input is of the form: <Key> <value1 value2 value3...>
		// Determine the value
		ArrayList<V> val = Input.parseEntityList( input.subString(1,input.size()-1), valClass, true );

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
