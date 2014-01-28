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

public class InstanceIterable<T extends Entity> implements Iterable<T>, Iterator<T> {

	private final ArrayList<? extends Entity> allInstances = Entity.getAll();
	private final Class<T> entClass;
	private int curPos;
	private int nextPos;

	public InstanceIterable(Class<T> aClass) {
		entClass = aClass;
		curPos = -1;
		nextPos = -1;
	}

	private boolean findNext(){
		if(nextPos+1 >= allInstances.size())
			return false;
		while(allInstances.get(++nextPos).getClass() != entClass){
			// if this is the last item on the list, exit
			if(nextPos+1 >= allInstances.size())
				return false;
		}
		return true;
	}

	@Override
	public boolean hasNext() {
		if(nextPos+1 >=  allInstances.size())
			return false;
		if(nextPos > curPos)
			return true;
		return findNext();
	}

	@Override
	public T next() {
		if(nextPos == curPos){
			if( !findNext() )
				throw new NoSuchElementException();
		}
		curPos = nextPos;
		return entClass.cast(allInstances.get(curPos));
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
