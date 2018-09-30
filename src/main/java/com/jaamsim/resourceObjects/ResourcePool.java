/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018 JaamSim Software Inc.
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
package com.jaamsim.resourceObjects;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Entity;

public class ResourcePool extends AbstractResourceProvider {

	private final ArrayList<Seizable> seizableList;

	public ResourcePool() {
		seizableList = new ArrayList<>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		seizableList.clear();
		for (Entity ent : Entity.getClonesOfIterator(Entity.class, Seizable.class)) {
			Seizable unit = (Seizable) ent;
			if (unit.getResourcePool() != this)
				continue;
			seizableList.add(unit);
		}
	}

	@Override
	public boolean canSeize(int n, DisplayEntity ent) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void seize(int n, DisplayEntity ent) {
		// TODO Auto-generated method stub
	}

	@Override
	public void release(int n, DisplayEntity ent) {
		// TODO Auto-generated method stub
	}

}
