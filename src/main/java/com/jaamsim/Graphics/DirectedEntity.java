/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2020 JaamSim Software Inc.
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

import java.util.ArrayList;

public class DirectedEntity {

	public final DisplayEntity entity;
	public final boolean direction;

	public static final String REVERSE = "(R)";

	public DirectedEntity(DisplayEntity ent) {
		this(ent, true);
	}

	public DirectedEntity(DisplayEntity ent, boolean dir) {
		entity = ent;
		direction = dir;
	}

	public static ArrayList<DirectedEntity> getList(ArrayList<DisplayEntity> list, boolean dir) {
		ArrayList<DirectedEntity> ret = new ArrayList<>(list.size());
		for (DisplayEntity ent : list) {
			ret.add(new DirectedEntity(ent, dir));
		}
		return ret;
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
		if (!(obj instanceof DirectedEntity)) return false;
		DirectedEntity de = (DirectedEntity) obj;
		return de.entity == entity && de.direction == direction;
	}

}
