/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.basicsim;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.sandwell.JavaSimulation.Entity;

public class ClonesOfIterableInterface<T extends Entity> implements Iterable<T>, Iterator<T> {
	private final ArrayList<? extends Entity> allInstances = Entity.getAll();
	private final Class<T> entClass;
	private final Class<?> ifaceClass;
	private int curPos;
	private int nextPos;

	public ClonesOfIterableInterface(Class<T> aClass, Class<?> iface) {
		entClass = aClass;
		ifaceClass = iface;
		curPos = -1;
		nextPos = -1;
	}

	private void updatePos() {
		if (nextPos >= allInstances.size())
			return;

		while (++nextPos < allInstances.size()) {
			// If we find a match, break out
			Class<?> klass = allInstances.get(nextPos).getClass();
			if (entClass.isAssignableFrom(klass) &&
			    ifaceClass.isAssignableFrom(klass))
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
