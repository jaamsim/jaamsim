/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2021 JaamSim Software Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.jaamsim.Graphics.DisplayEntity;

/**
 * Stores entities in order of priority and insertion sequence. Entities are grouped by type into
 * subclasses that maintained separately for increased efficiency.
 * @author Harry King
 *
 */
public class EntStorage {

	private final MappedTreeSet<String, StorageEntry> entrySet;
	private String typeWithMaxCount;  // entity type with the largest number of entities
	private int countForMaxType;     // largest number of entities for a given entity type

	public EntStorage() {
		entrySet = new MappedTreeSet<>();
	}

	public void clear() {
		entrySet.clear();
		typeWithMaxCount = null;
		countForMaxType = -1;
	}

	public static class StorageEntry implements Comparable<StorageEntry> {

		final DisplayEntity entity;
		final String type;
		final int priority;
		final long seqNum;
		final double timeAdded;

		public StorageEntry(DisplayEntity ent, String tp, int pri, long n, double t) {
			entity = ent;
			type = tp;
			priority = pri;
			seqNum = n;
			timeAdded = t;
		}

		@Override
		public int compareTo(StorageEntry entry) {
			int ret = Integer.compare(priority, entry.priority);
			if (ret != 0)
				return ret;

			return Long.compare(seqNum, entry.seqNum);
		}

		@Override
		public String toString() {
			return String.format("(%s, %s, %s, %s)",
					entity, type, priority, seqNum);
		}
	}

	/**
	 * Adds the specified entry to the storage if not present.
	 * @param entry - entry to be added to this storage.
	 * @return true if this storage did not already contain the specified entry.
	 */
	public boolean add(StorageEntry entry) {

		// Add the entity to the storage
		String type = entry.type;
		boolean bool = entrySet.add(type, entry);
		if (!bool)
			return false;

		// Does the entry have a entity type value?
		if (type == null || typeWithMaxCount == null)
			return true;

		// Update the maximum count
		if (type.equals(typeWithMaxCount)) {
			countForMaxType++;
		}
		else {
			int n = entrySet.size(type);
			if (n > countForMaxType) {
				typeWithMaxCount = type;
				countForMaxType = n;
			}
		}
		return true;
	}

	/**
	 * Removes the specified entry from this storage if present.
	 * @param entry to be removed from this storage.
	 * @return true if this storage contained the specified entry.
	 */
	public boolean remove(StorageEntry entry) {

		// Remove the entity from the storage
		String type = entry.type;
		boolean found = entrySet.remove(type, entry);
		if (!found)
			return false;

		// Does the entry have a entity type value?
		if (type == null || typeWithMaxCount == null)
			return true;

		// Update the maximum count
		if (type.equals(typeWithMaxCount)) {
			typeWithMaxCount = null;
			countForMaxType = -1;
		}
		return true;
	}

	/**
	 * Returns the number of entities in the storage.
	 * @return number of entities in storage.
	 */
	public int size() {
		return size(null);
	}

	/**
	 * Returns the number of entities in storage with a specified entity type.
	 * If the specified type is null, then every entity is counted.
	 * @param type - specified entity type.
	 * @return number of entities of the specified type.
	 */
	public int size(String type) {
		if (type == null)
			return entrySet.size();
		return entrySet.size(type);
	}

	/**
	 * Returns whether the storage is empty.
	 * @return true if the storage is empty
	 */
	public boolean isEmpty() {
		return isEmpty(null);
	}

	/**
	 * Returns whether the storage is empty for the specified entity type.
	 * @param type - specified entity type.
	 * @return true if the storage is empty
	 */
	public boolean isEmpty(String type) {
		if (type == null)
			return entrySet.isEmpty();
		return entrySet.isEmpty(type);
	}

	/**
	 * Returns the first StorageEntry in the storage.
	 * @return first entity in the storage.
	 */
	public StorageEntry first() {
		return first(null);
	}

	/**
	 * Returns the first StorageEntry in the storage with a specified entity type.
	 * If the specified type is null, the first StorageEntry is returned.
	 * @param type - specified entity type.
	 * @return first StorageEntry of the specified type.
	 */
	public StorageEntry first(String type) {
		if (type == null)
			return entrySet.first();
		return entrySet.first(type);
	}

	public Iterator<StorageEntry> iterator() {
		return iterator(null);
	}

	public Iterator<StorageEntry> iterator(String type) {
		if (type == null)
			return entrySet.iterator();
		return entrySet.iterator(type);
	}

	/**
	 * Returns the StorageEntries in storage.
	 * @return StorageEntries in storage.
	 */
	public Collection<StorageEntry> getEntries() {
		return getEntries(null);
	}

	/**
	 * Returns the StorageEntries in storage for the specified entity type.
	 * @param type - specified entity type.
	 * @return StorageEntries in storage.
	 */
	public Collection<StorageEntry> getEntries(String type) {
		if (type == null)
			return entrySet.values();
		return entrySet.values(type);
	}

	/**
	 * Returns the entity types that are present in the storage.
	 * @return set of entity types.
	 */
	public Set<String> getTypes() {
		return entrySet.keySet();
	}

	/**
	 * Returns the entity type that has the largest number of entities in the storage.
	 * @return entity type with the most entities.
	 */
	public String getTypeWithMaxCount() {
		if (typeWithMaxCount == null) {
			typeWithMaxCount = entrySet.maxKey();
			countForMaxType = entrySet.size(typeWithMaxCount);
		}
		return typeWithMaxCount;
	}

	/**
	 * Returns the number of entities for the most numerous entity type in storage.
	 * @return number of entities for the most numerous entity type.
	 */
	public int getCountForMaxType() {
		if (typeWithMaxCount == null) {
			typeWithMaxCount = entrySet.maxKey();
			countForMaxType = entrySet.size(typeWithMaxCount);
		}
		return countForMaxType;
	}

	/**
	 * Returns the entities in the storage.
	 * @return entities in storage
	 */
	public ArrayList<DisplayEntity> getEntityList() {
		ArrayList<DisplayEntity> ret = new ArrayList<>(entrySet.size());
		Iterator<StorageEntry> itr = entrySet.iterator();
		while (itr.hasNext()) {
			ret.add(itr.next().entity);
		}
		return ret;
	}

	/**
	 * Returns the entities in storage for the specified entity type.
	 * @param type - specified entity type
	 * @return entities in storage for the specified type
	 */
	public ArrayList<DisplayEntity> getEntityList(String type) {
		Collection<StorageEntry> entries = entrySet.values(type);
		ArrayList<DisplayEntity> ret = new ArrayList<>(entries.size());
		for (StorageEntry entry : entries) {
			ret.add(entry.entity);
		}
		return ret;
	}

	/**
	 * Returns the priority for each entity in the storage.
	 * @return priority for each entity
	 */
	public ArrayList<Integer> getPriorityList() {
		ArrayList<Integer> ret = new ArrayList<>(entrySet.size());
		Iterator<StorageEntry> itr = entrySet.iterator();
		while (itr.hasNext()) {
			ret.add(itr.next().priority);
		}
		return ret;
	}

	/**
	 * Returns the type for each entity in the storage.
	 * @return type for each entity
	 */
	public ArrayList<String> getTypeList() {
		ArrayList<String> ret = new ArrayList<>(entrySet.size());
		Iterator<StorageEntry> itr = entrySet.iterator();
		while (itr.hasNext()) {
			String type = itr.next().type;
			if (type != null) {
				ret.add(type);
			}
		}
		return ret;
	}

	/**
	 * Returns the time each entity has spent in the storage.
	 * @param simTime - present time
	 * @return time in storage for each entity
	 */
	public ArrayList<Double> getStorageTimeList(double simTime) {
		ArrayList<Double> ret = new ArrayList<>(entrySet.size());
		Iterator<StorageEntry> itr = entrySet.iterator();
		while (itr.hasNext()) {
			ret.add(simTime - itr.next().timeAdded);
		}
		return ret;
	}

	@Override
	public String toString() {
		return entrySet.toString();
	}

}
