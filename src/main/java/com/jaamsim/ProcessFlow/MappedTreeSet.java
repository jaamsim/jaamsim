/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018 JaamSim Software Inc.
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
package com.jaamsim.ProcessFlow;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * Stores a set of unique objects in an order determined by the object's comparator.
 * The objects are grouped into subsets by the value of a key, which is normally a
 * property of the object.
 * @author Harry King
 *
 * @param <K> - key
 * @param <V> - object
 */
public class MappedTreeSet<K,V> {

	private final TreeSet<V> objSet;  // contains all the objects
	private final HashMap<K, TreeSet<V>> subsetMap;  // maps a key to sub-sets of the objects

	public MappedTreeSet() {
		objSet = new TreeSet<>();
		subsetMap = new HashMap<>();
	}

	public void clear() {
		objSet.clear();
		subsetMap.clear();
	}

	/**
	 * Adds the specified element to the set if it is not already present.
	 * If the key is not null, the element is added to both the set of all elements and to
	 * the subset for that key.
	 * @param key - property used to group the stored elements into subsets.
	 * @param e - element to be added to this set.
	 * @return true if this set did not already contain the specified element.
	 */
	public boolean add(K key, V e) {

		// Add the object to the complete set
		boolean ret = objSet.add(e);
		if (!ret)
			return false;

		// If there is a key for this object, add it to its subset
		if (key == null)
			return true;

		// If this is the first object for its key, create a new subset
		TreeSet<V> subSet = subsetMap.get(key);
		if (subSet == null) {
			subSet = new TreeSet<>();
			subsetMap.put(key, subSet);
		}
		ret = subSet.add(e);
		return ret;
	}

	/**
	 * Removes the specified element from this set if it is present.
	 * If the key is not null, the element is removed from both the set of all elements and from
	 * the subset for that key.
	 * @param key - property used to group the stored elements into subsets.
	 * @param o - object to be removed from this set if present.
	 * @return true if this set contained the specified element.
	 */
	public boolean remove(K key, Object o) {

		// Remove the object from the complete set
		boolean found = objSet.remove(o);
		if (!found)
			return false;

		// If there a key for this object, remove it from its subset
		if (key == null)
			return true;

		TreeSet<V> subSet = subsetMap.get(key);
		if (subSet == null)
			return false;

		found = subSet.remove(o);
		if (!found)
			return false;

		// Delete the subset if it is now empty
		if (subSet.isEmpty()) {
			subsetMap.remove(key);
		}
		return true;
	}

	public int size() {
		return objSet.size();
	}

	public int size(K key) {
		TreeSet<V> subSet = subsetMap.get(key);
		if (subSet == null)
			return 0;
		return subSet.size();
	}

	public boolean isEmpty() {
		return objSet.isEmpty();
	}

	public boolean isEmpty(K key) {
		TreeSet<V> subSet = subsetMap.get(key);
		return subSet == null || subSet.isEmpty();
	}

	public boolean contains(Object o) {
		return objSet.contains(o);
	}

	public Iterator<V> iterator() {
		return objSet.iterator();
	}

	public Iterator<V> iterator(K key) {
		return subsetMap.get(key).iterator();
	}

	public Object[] toArray() {
		return objSet.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return objSet.toArray(a);
	}

	public boolean containsKey(Object key) {
		return subsetMap.containsKey(key);
	}

	public V first() {
		return objSet.first();
	}

	public V first(Object key) {
		TreeSet<V> subSet = subsetMap.get(key);
		if (subSet == null)
			return null;
		return subSet.first();
	}

	public V last() {
		return objSet.last();
	}

	public V last(Object key) {
		TreeSet<V> subSet = subsetMap.get(key);
		if (subSet == null)
			return null;
		return subSet.last();
	}

	public Set<K> keySet() {
		return subsetMap.keySet();
	}

	public Collection<V> values() {
		return objSet;
	}

	public Collection<V> values(Object key) {
		return subsetMap.get(key);
	}

	/**
	 * Returns the key for the subset that has the greatest number of elements.
	 * @return key with the most elements.
	 */
	public K maxKey() {
		K ret = null;
		int n = 0;
		for (Entry<K, TreeSet<V>> each : subsetMap.entrySet()) {
			if (ret == null || each.getValue().size() > n) {
				ret = each.getKey();
				n = each.getValue().size();
			}
		}
		return ret;
	}

	@Override
	public String toString() {
		return objSet.toString();
	}

}
