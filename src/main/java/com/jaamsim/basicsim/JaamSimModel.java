/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016-2019 JaamSim Software Inc.
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
package com.jaamsim.basicsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.jaamsim.events.EventManager;

public class JaamSimModel {

	private final EventManager eventManager;
	private final AtomicLong entityCount = new AtomicLong(0);
	private final ArrayList<Entity> allInstances = new ArrayList<>(100);
	private final HashMap<String, Entity> namedEntities = new HashMap<>(100);

	public JaamSimModel() {
		Entity.setJaamSimModel(this);
		eventManager = new EventManager("DefaultEventManager");
	}

	public EventManager getEventManager() {
		return eventManager;
	}

	final long getNextEntityID() {
		return entityCount.incrementAndGet();
	}

	public final Entity getNamedEntity(String name) {
		synchronized (allInstances) {
			return namedEntities.get(name);
		}
	}

	public final long getEntitySequence() {
		long seq = (long)allInstances.size() << 32;
		seq += entityCount.get();
		return seq;
	}

	private final int idToIndex(long id) {
		int lowIdx = 0;
		int highIdx = allInstances.size() - 1;

		while (lowIdx <= highIdx) {
			int testIdx = (lowIdx + highIdx) >>> 1; // Avoid sign extension
			long testNum = allInstances.get(testIdx).getEntityNumber();

			if (testNum < id) {
				lowIdx = testIdx + 1;
				continue;
			}

			if (testNum > id) {
				highIdx = testIdx - 1;
				continue;
			}

			return testIdx;
		}

		// Entity number not found
		return -(lowIdx + 1);
	}

	public final Entity idToEntity(long id) {
		synchronized (allInstances) {
			int idx = this.idToIndex(id);
			if (idx < 0)
				return null;

			return allInstances.get(idx);
		}
	}

	public final ArrayList<? extends Entity> getEntities() {
		synchronized(allInstances) {
			return allInstances;
		}
	}

	final void renameEntity(Entity e, String newName) {
		synchronized (allInstances) {
			// Unregistered entities do not appear in the named entity hashmap, no consistency checks needed
			if (!e.testFlag(Entity.FLAG_REGISTERED)) {
				e.entityName = newName;
				return;
			}

			if (namedEntities.get(newName) != null)
				throw new ErrorException("Entity name: %s is already in use.", newName);

			String oldName = e.entityName;
			if (oldName != null && namedEntities.remove(oldName) != e)
				throw new ErrorException("Named Entities Internal Consistency error");

			e.entityName = newName;
			namedEntities.put(newName, e);
		}
	}

	final void addInstance(Entity e) {
		synchronized(allInstances) {
			allInstances.add(e);
		}
	}

	final void restoreInstance(Entity e) {
		synchronized(allInstances) {
			int index = idToIndex(e.getEntityNumber());
			if (index >= 0) {
				throw new ErrorException("Entity already included in allInstances: %s", e);
			}
			allInstances.add(-index - 1, e);
		}
	}

	final void removeInstance(Entity e) {
		synchronized (allInstances) {
			int index = idToIndex(e.getEntityNumber());
			if (index < 0)
				return;

			if (e != allInstances.remove(index))
				throw new ErrorException("Internal Consistency Error - Entity List");

			if (e.testFlag(Entity.FLAG_REGISTERED)) {
				if (e != namedEntities.remove(e.entityName))
					throw new ErrorException("Named Entities Internal Consistency error: %s", e);
			}

			e.entityName = null;
			e.setFlag(Entity.FLAG_DEAD);
		}
	}
}
