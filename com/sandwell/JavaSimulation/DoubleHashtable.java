/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2004-2011 Ausenco Engineering Canada Inc.
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

import java.util.Hashtable;
import java.util.Enumeration;
import static com.sandwell.JavaSimulation.Util.*;
import com.sandwell.JavaSimulation.ErrorException;

/**
 * This class implements a hashtable, which maps keys to values. Any non-null object can be
 * used as a key, but the value must be of type double. This class is intended to simply
 * storage of values that are of type double.
 */
public class DoubleHashtable {

	/**
	 * Stores mapping of keys to values. The keys are any non-null objects and the values
	 * are objects of type Double.
	 */
	private Hashtable<Object, Double> hashtable;

	/**
	 * Constructs a new, empty Hashtable with a default initial capacity (10) and load
	 * factor, which is 0.75.
	 */
	public DoubleHashtable() {
		this( 10, 0.75f );
	}

	/**
	 * Constructs a new, empty Hashtable with the specified initial capacity and default
	 * load factor, which is 0.75.
	 */
	public DoubleHashtable( int initialCapacity ) {
		this( initialCapacity, 0.75f );
	}

	/**
	 * Constructs a new, empty hashtable with the specified initial capacity and the
	 * specified load factor.
	 */
	public DoubleHashtable( int initialCapacity, float loadFactor ) {
		hashtable = new Hashtable<Object, Double>( initialCapacity, loadFactor );
	}

	/**
	 * Maps the specified key to the specified value in this hashtable. Neither the key
	 * nor the value can be null. If the key already exists in the hashtable, the value
	 * mapped to this key is replaced by the new value.
     * <p>
     * The value can be retrieved by calling the get method with a key that is equal to
     * the original key.
	 *
	 * @param key the hashtable key.
	 * @param value the value.
	 * @throws NullPointerException if the key or value is null.
	 */
	public void put( Object key, double value ) {
		hashtable.put( key, new Double( value ) );
	}

	/**
	 * Replaces the value that is mapped to the specified key with the new value.
	 *
	 * @param key the key whose associated value is being replaced.
	 * @param value the new value to be mapped to key.
	 */
	public void set( Object key, double value ) {
		hashtable.remove( key );
		hashtable.put( key, new Double( value ) );
	}

	/**
	 * Tests if the specified object is a key in this hashtable.
	 *
	 * @param key possible key.
	 * @return true if and only if the specified object is a key in this hashtable, as
	 * determined by the equals method; false otherwise.
	 * @throws NullPointerException if the key is null.
	 */
	public boolean containsKey( Object key ) {
		return hashtable.containsKey( key );
	}

	/**
	 * Returns the double value to which the specified key is mapped in this hashtable.
	 *
	 * @param key a key in the hashtable.
	 * @return the double value to which the key is mapped in this hashtable
	 * @throws NullPointerException if the key is null
	 * @throws ErrorException the hashtable does not contain the key.
	 */
	public double get( Object key ) {
		Double value = hashtable.get(key);
		if ( value == null ) {
			throw new ErrorException( "The hashtable does not contain the specified key." );
		}

		return value.doubleValue();
	}

	/**
     * Removes the key (and its corresponding value) from this hashtable. This method does
     * nothing if the key is not in the hashtable.
	 *
	 * @param key the key that needs to be removed.
	 * @return the removed value that was mapped to the specified key.
	 * @throws NullPointerException if the key is null.
	 * @throws ErrorException if the key is not in the hashtable.
	 */
	public double remove( Object key ) {
		Double value = hashtable.remove( key ); // Throws NullPointerException if
														 // key is null

		if ( value == null ) {
			throw new ErrorException( "The hashtable does not contain the specified key." );
		}

		return value.doubleValue();
	}

	/**
	 * Clears this hashtable so that it contains no keys.
	 */
	public void clear() {
		hashtable.clear();
	}

	/**
	 * Returns the number of keys in this hashtable.
	 *
	 * @return the number of keys in this hashtable.
	 */
	public int size() {
		return hashtable.size();
	}

	/**
	 * Returns an enumeration of the keys in this hashtable.
	 *
	 * @return an enumeration of the keys in this hashtable.
	 */
	public Enumeration<Object> keys() {
		return hashtable.keys();
	}

	/**
	 * Increments the value mapped to the key in this hashtable.
	 *
	 * @param key the key whose mapped value needs to be incremented.
	 * @return the previous value of the specified key in this hashtable.
	 * @throws NullPointerException if the key is null.
	 * @throws ErrorException if the hashtable does not contain the key.
	 */
	public double incrementValue( Object key, double incr ) {
		Double obj = hashtable.get( key ); // Throws NullPointerException if key is null.

		// Hashtable does not contain key
		if ( obj == null ) {
			throw new ErrorException( "Hashtable does not contain the specified key." );
		}

		double value = obj.doubleValue() + incr;

		return hashtable.put(key, new Double(value)).doubleValue();
	}

	/**
	 * Decrements the value mapped to the key in this hashtable.
	 *
	 * @param key the key whose mapped value needs to be decremented.
	 * @param decr the amount to be decremented
	 * @return the previous value of the specified key in this hashtable.
	 * @throws NullPointerException if the key is null.
	 * @throws ErrorException if the hashtable does not contain the key.
	 */
	public double decrementValue( Object key, double decr ) {
		Double obj = hashtable.get(key); // Throws NullPointerException if key is null.

		// Hashtable does not contain key
		if ( obj == null ) {
			throw new ErrorException( "Hashtable does not contain the specified key." );
		}

		double value = obj.doubleValue() - decr;

		return hashtable.put(key, new Double(value)).doubleValue();
	}

	/**
	 *
	 */
	public double getSumOfValues() {
		Object key;
		double sum = 0;

		for ( Enumeration<Object> e = hashtable.keys(); e.hasMoreElements(); ) {
			key = e.nextElement();
			sum += hashtable.get(key).doubleValue();
		}

		return sum;
	}

	/**
	 * Returns a string representation of this hashtable. The string representation
	 * returned has the following form:
	 * <pre>
	 *   ( key=value, key=value, ... )
	 * where
	 *   key = string representation of the key in the hashtable, and
	 *   value = the formatted double of the value mapped to this key
	 * </pre>
	 *
	 * @return the string representation of this hashtable.
	 */
	public String toString() {

		Object key;
		double value;
		StringBuilder out = new StringBuilder("(");

		Enumeration<Object> enumeration = hashtable.keys();

		if ( enumeration.hasMoreElements() ) {
			key   = enumeration.nextElement();
			value = hashtable.get(key).doubleValue();
			out.append(" ");
			out.append(key.toString());
			out.append("=");
			out.append(formatNumber(value));
		}

		while ( enumeration.hasMoreElements() ) {
			key   = enumeration.nextElement();
			value = hashtable.get(key).doubleValue();
			out.append(", ");
			out.append(key.toString());
			out.append("=");
			out.append(formatNumber(value));
		}

		out.append(" )");

		return out.toString();
	}
}
