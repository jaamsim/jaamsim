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
import java.util.Collections;

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
			if (unit.getResourcePool() != this || !((DisplayEntity)unit).isActive())
				continue;
			seizableList.add(unit);
		}
	}

	public ArrayList<Seizable> getEligibleList(DisplayEntity ent) {
		ArrayList<Seizable> ret = new ArrayList<>(seizableList.size());
		for (Seizable unit : seizableList) {
			if (!unit.canSeize(ent))
				continue;
			ret.add(unit);
		}
		return ret;
	}

	@Override
	public boolean canSeize(int n, DisplayEntity ent) {
		return n <= getEligibleList(ent).size();
	}

	@Override
	public void seize(int n, DisplayEntity ent) {
		if (isTraceFlag()) trace(1,"seize(%s, %s)", n, ent);

		// List the units that are eligible to be seized
		ArrayList<Seizable> eligibleList = getEligibleList(ent);
		if (n > eligibleList.size())
			error(ERR_CAPACITY, eligibleList.size(), n);

		// Sort the units by priority and release time
		ArrayList<SeizableUnit> list = new ArrayList<>(eligibleList.size());
		for (Seizable unit : eligibleList) {
			list.add(new SeizableUnit(unit, ent));
		}
		Collections.sort(list);

		// Seize the first n units
		for (int i = 0; i < n; i++) {
			list.get(i).unit.seize(ent);
		}
	}

	@Override
	public void release(int n, DisplayEntity ent) {
		if (isTraceFlag()) trace(1,"release(%s, %s)", n, ent);
		for (Seizable unit : seizableList) {
			if (unit.getAssignment() != ent)
				continue;
			unit.release();
		}
	}

	private static class SeizableUnit implements Comparable<SeizableUnit> {
		private final Seizable unit;
		private final int priority;
		private final long ticks;

		private SeizableUnit(Seizable u, DisplayEntity ent) {
			unit = u;
			priority = u.getPriority(ent);
			ticks = u.getLastReleaseTicks();
		}

		@Override
		public int compareTo(SeizableUnit su) {

			// Compare priorities
			int ret = Integer.compare(priority, su.priority);
			if (ret != 0)
				return ret;

			// Priorities are equal
			// Compare the release times
			return Long.compare(ticks, su.ticks);
		}

		@Override
		public String toString() {
			return unit.toString();
		}
	}

}
