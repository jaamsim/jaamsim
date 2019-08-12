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
		public int killsSincePurge = 0;
	}

	private final ListData listData;
	private boolean firstRead = true;
	protected final Class<T> entClass;
	private Entity curEnt;

	public EntityIterator(JaamSimModel simModel, Class<T> aClass) {
		listData = simModel.getListData();
		curEnt = listData.firstEnt;
		entClass = aClass;
	}

	abstract boolean matches(Class<?> entklass);

	// Advance the current pointer past any dead entities, or entities that do not match
	// Also update the list to bypass dead entities
	private Entity validateNext(Entity nextEnt, Entity prevEnt) {
		while(true) {
			if (nextEnt == null) {
				listData.killsSincePurge = 0;
				return null;
			}
			// Return a valid match
			if (!nextEnt.testFlag(Entity.FLAG_DEAD) && matches(nextEnt.getClass())) {
				return nextEnt;
			}
			boolean skipPrevAdvance = false;
			if (nextEnt.testFlag(Entity.FLAG_DEAD) && nextEnt != listData.lastEnt) {
				// Fix-up the linked list to skip over dead entities

				// It is important to not skip the last element, because different iterators can
				// race to update the lastEnt pointer

				if (prevEnt == null) { // This is the beginning of the list
					synchronized(listData) {
						listData.firstEnt = nextEnt.nextEnt;
					}
				} else {
					// Safeguard, this can only happen due to races, but it has happened
					if (nextEnt.nextEnt != null) {
						prevEnt.nextEnt = nextEnt.nextEnt;
						skipPrevAdvance = true;
					}
				}
			}

			// Advance
			if (!skipPrevAdvance) {
				prevEnt = nextEnt;
			}
			nextEnt = nextEnt.nextEnt;
		}
	}

	@Override
	public boolean hasNext() {
		if (firstRead) {
			curEnt = validateNext(curEnt, null);
			firstRead = false;
		}
		return curEnt != null;
	}

	// Note, this warning is suppressed because the cast is effectively checked by match()
	@SuppressWarnings("unchecked")
	@Override
	public T next() {
		if (firstRead) {
			curEnt = validateNext(curEnt, null);
			firstRead = false;
		}
		Entity nextEnt = curEnt;
		if (nextEnt == null) {
			throw new NoSuchElementException();
		}

		curEnt =  validateNext(curEnt.nextEnt, curEnt);
		return (T)nextEnt;
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
