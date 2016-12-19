/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016 JaamSim Software Inc.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

public class JaamSimModel {
	private final AtomicLong entityCount = new AtomicLong(0);
	private final ArrayList<Entity> allInstances = new ArrayList<>(100);
	final HashMap<String, Entity> namedEntities = new HashMap<>(100);

	public JaamSimModel() {
	}

	final long getNextEntityID() {
		return entityCount.incrementAndGet();
	}

	public final Entity getNamedEntity(String name) {
		synchronized (namedEntities) {
			return namedEntities.get(name);
		}
	}

	public final long getEntitySequence() {
		long seq = (long)allInstances.size() << 32;
		seq += entityCount.get();
		return seq;
	}

	public final Entity idToEntity(long id) {
		synchronized (allInstances) {
			for (Entity e : allInstances) {
				if (e.getEntityNumber() == id) {
					return e;
				}
			}
			return null;
		}
	}

	private static class EntityComparator implements Comparator<Entity> {
		@Override
        public int compare(Entity e1, Entity e2) {
			return Long.compare(e1.getEntityNumber(), e2.getEntityNumber());
        }
     }
	private static final EntityComparator entityComparator = new EntityComparator();

	public final ArrayList<? extends Entity> getEntities() {
		synchronized(allInstances) {
			return allInstances;
		}
	}

	void addInstance(Entity e) {
		synchronized(allInstances) {
			allInstances.add(e);
		}
	}

	void removeInstance(Entity e) {
		synchronized (allInstances) {
			int index = Collections.binarySearch(allInstances, e, entityComparator);
			if (index >= 0)
				allInstances.remove(index);
		}
	}
}
