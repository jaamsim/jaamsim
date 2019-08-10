/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019 JaamSim Software Inc.
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

import java.util.Iterator;
import java.util.NoSuchElementException;


public abstract class EntityIterator<T extends Entity> implements Iterable<T>, Iterator<T> {
	// A "struct" to mimic a pointer-to-pointer like relationship
	public static class ListData {
		public Entity firstEnt;
		public Entity lastEnt;
		public int numLiveEnts;
	}

	//private final ArrayList<? extends Entity> allInstances;
	private final ListData listData;
	private boolean firstRead = true;
	protected final Class<T> entClass;
	private Entity curEnt;

	public EntityIterator(JaamSimModel simModel, Class<T> aClass) {
		listData = simModel.getListData();
		entClass = aClass;
	}

	abstract boolean matches(Class<?> entklass);

	private Entity peekNext() {

		if (curEnt == null && !firstRead)
			return null;

		Entity nextEnt = firstRead ? listData.firstEnt : curEnt.nextEnt;
		Entity prevEnt = curEnt; // prevEnt will be null at the beginning of the list

		// Advance until the end of the list or the next live entity that matches the
		// entity class we are looking for
		while(true) {
			if (nextEnt == null) {
				return null;
			}
			// Return a valid match
			if (!nextEnt.testFlag(Entity.FLAG_DEAD) && matches(nextEnt.getClass())) {
				return nextEnt;
			}
			if (nextEnt.testFlag(Entity.FLAG_DEAD)) {
				// Fix-up the linked list to skip over dead entities
				if (prevEnt == null) { // This is the beginning of the list
					listData.firstEnt = nextEnt.nextEnt;
				} else {
					prevEnt.nextEnt = nextEnt.nextEnt;
				}
			}

			// Advance
			prevEnt = nextEnt;
			nextEnt = nextEnt.nextEnt;
		}
	}

	@Override
	public boolean hasNext() {
		return peekNext() != null;
	}

	// Note, this warning is suppressed because the cast is effectively checked by match()
	@SuppressWarnings("unchecked")
	@Override
	public T next() {
		Entity nextEnt = peekNext();
		if (nextEnt == null) {
			throw new NoSuchElementException();
		}

		curEnt = nextEnt;
		firstRead = false;
		return (T)curEnt;
	}

	@Override
	public void remove() {
		 throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<T> iterator(){
		return this;
	}
}
