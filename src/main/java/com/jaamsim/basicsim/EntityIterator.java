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
	private boolean needAdvance = true;
	protected final Class<T> entClass;
	private EntityListNode curNode;
	private EntityListNode endNode;

	public EntityIterator(JaamSimModel simModel, Class<T> aClass) {
		endNode = simModel.getEntityList();
		curNode = endNode;
		entClass = aClass;
	}

	abstract boolean matches(Class<?> entklass);

	// Advance the current pointer past any dead entities, or entities that do not match
	private void advance() {
		if (!needAdvance) {
			return;
		}
		curNode = curNode.next;
		needAdvance = false;

		while (true) {
			if (curNode == endNode) {
				return;
			}
			if (curNode == null) {
				// This is likely a race condition but unrecoverable
				// Terminate iteration
				return;
			}

			if (matches(curNode.entClass) && curNode.ent != null) {
				return;
			}
			curNode = curNode.next;
		}
	}

	@Override
	public boolean hasNext() {
		advance();
		return curNode != null && curNode != endNode;
	}

	// Note, this warning is suppressed because the cast is effectively checked by match()
	@SuppressWarnings("unchecked")
	@Override
	public T next() {
		advance();
		Entity nextEnt = curNode.ent;
		if (nextEnt == null || curNode == endNode) {
			throw new NoSuchElementException();
		}

		needAdvance = true;
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
