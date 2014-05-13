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
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

/**
 * Class KeyInput for storing objects of class V (e.g. Double or DoubleVector), with an optional key of class K1
 */
public class KeyInput<K1 extends Entity, V> extends Input<V> {
	private Class<? extends Unit> unitType = DimensionlessUnit.class; // for when V is a SampleProvider

	protected double minValue = Double.NEGATIVE_INFINITY;
	protected double maxValue = Double.POSITIVE_INFINITY;
	private Class<K1> keyClass;
	private Class<V> valClass;
	private HashMap<K1,V> hashMap;
	private int minCount = 0;
	private int maxCount = Integer.MAX_VALUE;

	public KeyInput(Class<K1> kClass, Class<V> vClass, String keyword, String cat, V def) {
		super(keyword, cat, def);
		keyClass = kClass;
		valClass = vClass;
		hashMap = new HashMap<K1,V>();
	}

	public void setUnitType(Class<? extends Unit> u) {
		unitType = u;
		unitString = null;
	}

	private String unitString = "";
	public void setUnits(String units) {
		unitString = units;
	}

	@Override
	public void parse(StringVector input)
	throws InputErrorException {
		ArrayList<StringVector> split = InputAgent.splitStringVectorByBraces(input);
		for (StringVector each : split)
			this.innerParse(each);
	}

	private void innerParse(StringVector input) {
		// If an entity key is not provided, set the default value
		Entity ent = Input.tryParseEntity( input.get( 0 ), Entity.class );
		if( ent == null || input.size() == 1 ) {
			V defValue = Input.parse( input.subString(0,input.size()-1), valClass, unitString, minValue, maxValue, minCount, maxCount, unitType );
			this.setDefaultValue( defValue );
			return;
		}

		// The input is of the form: <Key> <Value>
		// Determine the key(s)
		ArrayList<K1> list = Input.parseEntityList(input.subString(0, 0), keyClass, true);

		// Determine the value
		V val = Input.parse( input.subString(1,input.size()-1), valClass, unitString, minValue, maxValue, minCount, maxCount, unitType );

		// Set the value for the given keys
		for( int i = 0; i < list.size(); i++ ) {
			hashMap.put( list.get(i), val );
		}
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
			return this.getDefaultValue();
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
		return getDefaultStringForKeyInputs(unitString);
	}

	public ArrayList<V> getAllValues() {

		ArrayList<V> values = new ArrayList<V>();

		for( V each : hashMap.values() ) {
			values.add(each);
		}
		values.add(this.getDefaultValue());
		return values;
	}
}
