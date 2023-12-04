/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2020 JaamSim Software Inc.
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
import com.jaamsim.ProcessFlow.StateUserEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;

public class ResourcePool extends AbstractResourceProvider {

	private final ArrayList<Seizable> seizableList;

	public ResourcePool() {
		seizableList = new ArrayList<>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		seizableList.clear();
		for (Entity ent : getJaamSimModel().getClonesOfIterator(Entity.class, Seizable.class)) {
			Seizable unit = (Seizable) ent;
			if (unit.getResourcePool() != this || !((DisplayEntity)unit).isActive())
				continue;
			seizableList.add(unit);
		}
	}

	@Override
	public int getCapacity(double simTime) {
		int ret = 0;
		for (Seizable unit : seizableList) {
			if (unit instanceof StateUserEntity && !((StateUserEntity) unit).isAvailable())
				continue;
			ret++;
		}
		return ret;
	}

	@Override
	public int getUnitsInUse() {
		int ret = 0;
		for (Seizable unit : seizableList) {
			if (unit.getAssignment() == null)
				continue;
			ret++;
		}
		return ret;
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
	public boolean canSeize(double simTime, int n, DisplayEntity ent) {
		return n <= getEligibleList(ent).size();
	}

	@Override
	public void seize(int n, DisplayEntity ent) {
		super.seize(n, ent);

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

		double simTime = EventManager.simSeconds();
		collectStatistics(simTime, getUnitsInUse());
	}

	@Override
	public void release(int n, DisplayEntity ent) {
		super.release(n, ent);
		for (Seizable unit : seizableList) {
			if (unit.getAssignment() != ent)
				continue;
			unit.release();
		}

		double simTime = EventManager.simSeconds();
		collectStatistics(simTime, getUnitsInUse());
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

	@Output(name = "UnitsList",
	 description = "The ResourceUnits that are members of this ResourcePool.",
	    sequence = 1)
	public ArrayList<Seizable> getUnitsList(double simTime) {
		return seizableList;
	}

	@Output(name = "UnitsInUseList",
	 description = "The present number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	    sequence = 2)
	public ArrayList<Seizable> getUnitsInUseList(double simTime) {
		ArrayList<Seizable> ret = new ArrayList<>(seizableList.size());
		for (Seizable unit : seizableList) {
			if (unit.getAssignment() == null)
				continue;
			ret.add(unit);
		}
		return ret;
	}

	@Output(name = "AvailableUnitsList",
	 description = "The number of resource units that are not in use.",
	    unitType = DimensionlessUnit.class,
	    sequence = 3)
	public ArrayList<Seizable> getAvailableUnitsList(double simTime) {
		ArrayList<Seizable> ret = new ArrayList<>(seizableList.size());
		for (Seizable unit : seizableList) {
			if (unit.getAssignment() != null)
				continue;
			if (unit instanceof StateUserEntity && !((StateUserEntity) unit).isAvailable())
				continue;
			ret.add(unit);
		}
		return ret;
	}

}
