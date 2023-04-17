/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2023 JaamSim Software Inc.
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
package com.jaamsim.Graphics;

import com.jaamsim.basicsim.Entity;

public class AbstractDirectedEntity<T extends Entity> {

	public final T entity;
	public final boolean direction;

	public static final String REVERSE = "(R)";

	public AbstractDirectedEntity(T ent) {
		this(ent, true);
	}

	public AbstractDirectedEntity(T ent, boolean dir) {
		entity = ent;
		direction = dir;
	}

	public T getEntity() {
		return entity;
	}

	public boolean getDirection() {
		return direction;
	}

	@Override
	public String toString() {
		String ret = entity.getName();
		if (!direction)
			ret = ret + REVERSE;
		return ret;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj == this) return true;
		if (!(obj instanceof AbstractDirectedEntity)) return false;
		AbstractDirectedEntity<?> de = (AbstractDirectedEntity<?>) obj;
		return de.entity == entity && de.direction == direction;
	}

}
