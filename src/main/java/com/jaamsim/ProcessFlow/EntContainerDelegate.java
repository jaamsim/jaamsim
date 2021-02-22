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
import java.util.Iterator;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProcessFlow.EntStorage.StorageEntry;

public class EntContainerDelegate implements EntContainer {

	private EntStorage storage;
	private DisplayEntity lastEntity;
	private long initialNumberAdded;
	private long initialNumberRemoved;
	private long numberAdded;
	private long numberRemoved;

	public EntContainerDelegate() {
		storage = new EntStorage();
	}

	public void clear() {
		storage.clear();
		lastEntity = null;
		initialNumberAdded = 0L;
		initialNumberRemoved = 0L;
		numberAdded = 0L;
		numberRemoved = 0L;
	}

	@Override
	public void registerEntity(DisplayEntity ent) {
		lastEntity = ent;
		numberAdded++;
	}

	@Override
	public void addEntity(DisplayEntity ent) {}

	/**
	 * Adds the specified entity to the container.
	 * @param ent - entity to be added
	 * @param type - type of entity
	 * @param pri - priority for removal
	 * @param fifo - true if entity stored in FIFO order, false if LIFO order
	 * @param simTime - present simulation time
	 */
	public void addEntity(DisplayEntity ent, String type, int pri, boolean fifo, double simTime) {

		// Ensure that the entity has been registered
		if (ent != lastEntity)
			ent.error("An entity must be registered by the container before it can be added.");

		// Build the entry for the entity
		long n = this.getTotalNumberAdded();
		if (!fifo) {
			n *= -1;
		}

		StorageEntry entry = new StorageEntry(ent, type, pri, n, simTime);
		storage.add(entry);
	}

	@Override
	public DisplayEntity removeEntity(String type) {
		StorageEntry entry = storage.first(type);
		storage.remove(entry);
		DisplayEntity ent = entry.entity;
		numberRemoved++;
		return ent;
	}

	@Override
	public int getCount(String type) {
		return storage.size(type);
	}

	@Override
	public boolean isEmpty(String type) {
		return storage.isEmpty(type);
	}

	public void clearStatistics() {
		initialNumberAdded = numberAdded;
		initialNumberRemoved = numberRemoved;
		numberAdded = 0L;
		numberRemoved = 0L;
	}

	public Iterator<DisplayEntity> iterator() {
		Iterator<DisplayEntity> ret = new Iterator<DisplayEntity>() {

			private Iterator<StorageEntry> itr = storage.iterator();

			@Override
			public boolean hasNext() {
				return itr.hasNext();
			}

			@Override
			public DisplayEntity next() {
				return itr.next().entity;
			}

		};
		return ret;
	}

	public long getTotalNumberAdded() {
		return initialNumberAdded + numberAdded;
	}

	public long getTotalNumberProcessed() {
		return initialNumberRemoved + numberRemoved;
	}

	public DisplayEntity getLastEntity() {
		return lastEntity;
	}

	public ArrayList<DisplayEntity> getEntityList(String type) {
		return storage.getEntityList(type);
	}

	public ArrayList<Integer> getPriorityList() {
		return storage.getPriorityList();
	}

	public ArrayList<String> getTypeList() {
		return storage.getTypeList();
	}

	public ArrayList<Double> getStorageTimeList(double simTime) {
		return storage.getStorageTimeList(simTime);
	}

	@Override
	public String toString() {
		return storage.toString();
	}

	// Used stubs for the StateUser methods

	@Override
	public void setPresentState(String state) {}

	@Override
	public boolean isWorkingState() {
		return false;
	}

}
