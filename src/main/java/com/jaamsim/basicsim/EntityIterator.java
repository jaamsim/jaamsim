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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;


public abstract class EntityIterator<T extends Entity> implements Iterable<T>, Iterator<T> {
	private final ArrayList<? extends Entity> allInstances;
	protected final Class<T> entClass;
	private int curPos;
	private int nextPos;

	public EntityIterator(JaamSimModel simModel, Class<T> aClass) {
		allInstances = simModel.getEntities();
		entClass = aClass;
		curPos = -1;
		nextPos = -1;
	}

	abstract boolean matches(Class<?> entklass);

	private void updatePos() {
		if (nextPos >= allInstances.size())
			return;

		while (++nextPos < allInstances.size()) {
			// If we find a match, break out
			if (matches(allInstances.get(nextPos).getClass()))
				break;
		}
	}

	@Override
	public boolean hasNext() {
		if (curPos == nextPos)
			updatePos();

		if (nextPos < allInstances.size())
			return true;
		else
			return false;
	}

	@Override
	public T next() {
		if (curPos == nextPos)
			updatePos();

		if (nextPos < allInstances.size()) {
			curPos = nextPos;
			return entClass.cast(allInstances.get(curPos));
		}
		else {
			throw new NoSuchElementException();
		}
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
