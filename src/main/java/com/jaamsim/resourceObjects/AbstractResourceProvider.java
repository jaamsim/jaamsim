/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2023 JaamSim Software Inc.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Statistics.TimeBasedFrequency;
import com.jaamsim.Statistics.TimeBasedStatistics;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

public abstract class AbstractResourceProvider extends DisplayEntity implements ResourceProvider {

	@Keyword(description = "If TRUE, the next entity to seize the resource will be chosen "
	                     + "strictly on the basis of priority and waiting time. If this entity "
	                     + "is unable to seize the resource because of other restrictions such as "
	                     + "an OperatingThreshold input or the unavailability of other resources "
	                     + "it needs to seize at the same time, then other entities with lower "
	                     + "priority or shorter waiting time will NOT be allowed to seize the "
	                     + "resource. If FALSE, the entities will be tested in the same order of "
	                     + "priority and waiting time, but the first entity that is able to seize "
	                     + "the resource will be allowed to do so.",
	         exampleList = {"TRUE"})
	private final BooleanProvInput strictOrder;

	private ArrayList<ResourceUser> userList;  // objects that can use this provider's units

	//	Statistics
	private int unitsSeized;    // number of units that have been seized
	private int unitsReleased;  // number of units that have been released
	private final TimeBasedStatistics stats;
	private final TimeBasedFrequency freq;

	public final static String ERR_CAPACITY = "Insufficient resource units: available=%s, req'd=%s";

	{
		strictOrder = new BooleanProvInput("StrictOrder", KEY_INPUTS, false);
		this.addInput(strictOrder);
	}

	public AbstractResourceProvider() {
		userList = new ArrayList<>();
		stats = new TimeBasedStatistics();
		freq = new TimeBasedFrequency(0, 10);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		userList = getUserList(this);

		unitsSeized = 0;
		unitsReleased = 0;
		stats.clear();
		stats.addValue(0.0d, 0);
		freq.clear();
		freq.addValue(0.0d,  0);
	}

	@Override
	public boolean isStrictOrder() {
		return strictOrder.getNextBoolean(this, getSimTime());
	}

	@Override
	public ArrayList<ResourceUser> getUserList() {
		return userList;
	}

	@Override
	public void seize(int n, DisplayEntity ent) {
		if (isTraceFlag()) trace(1, "seize(%s, %s)", n, ent);
		unitsSeized += n;
	}

	@Override
	public void release(int n, DisplayEntity ent) {
		if (isTraceFlag()) trace(1, "release(%s, %s)", n, ent);
		unitsReleased += n;
	}

	public void collectStatistics(double simTime, int unitsInUse) {
		stats.addValue(simTime, unitsInUse);
		freq.addValue(simTime, unitsInUse);
	}

	@Override
	public void clearStatistics() {
		super.clearStatistics();
		double simTime = this.getSimTime();
		unitsSeized = 0;
		unitsReleased = 0;
		stats.clear();
		stats.addValue(simTime, getUnitsInUse());
		freq.clear();
		freq.addValue(simTime, getUnitsInUse());
	}

	/**
	 * Returns a list of the ResourceUsers (such as Seize) that want to seize the specified
	 * ResourceProvider (such as ResourcePool).
	 * @param pool - specified ResourceProvider
	 * @return list of ResourceUsers that want to seize this ResourceProvider
	 */
	public static ArrayList<ResourceUser> getUserList(ResourceProvider pool) {
		ArrayList<ResourceUser> ret = new ArrayList<>();
		JaamSimModel simModel = ((Entity) pool).getJaamSimModel();
		for (Entity ent : simModel.getClonesOfIterator(Entity.class, ResourceUser.class)) {
			ResourceUser ru = (ResourceUser) ent;
			if (ru.requiresResource(pool))
				ret.add(ru);
		}
		return ret;
	}

	public static void notifyResourceUsers(ResourceProvider prov) {
		notifyResourceUsers(new ArrayList<>(Arrays.asList(prov)));
	}

	/**
	 * Starts resource users on their next entities.
	 */
	public static void notifyResourceUsers(ArrayList<ResourceProvider> resList) {

		// Prepare a sorted list of the resource users that have a waiting entity
		ArrayList<ResourceUser> list = new ArrayList<>();
		for (ResourceProvider res : resList) {
			for (ResourceUser ru : res.getUserList()) {
				if (!list.contains(ru) && ru.hasWaitingEntity()) {
					list.add(ru);
				}
			}
		}
		Collections.sort(list, userCompare);

		// Attempt to start the resource users in order of priority and wait time
		while (true) {

			// Find the first resource user that can seize its resources
			ResourceUser selection = null;
			for (ResourceUser ru : list) {
				if (ru.isReadyToStart()) {
					selection = ru;
					break;
				}

				// In strict-order mode, only the highest priority/longest wait time entity is
				// eligible to seize its resources
				if (ru.hasStrictResource())
					return;
			}

			// If none of the resource users can seize its resources, then we are done
			if (selection == null)
				return;

			// Seize the resources
			selection.startNextEntity();

			// Remove any resource users than have no waiting entities and then re-sort
			Iterator<ResourceUser> itr = list.iterator();
			while (itr.hasNext()) {
				ResourceUser ru = itr.next();
				if (!ru.hasWaitingEntity()) {
					itr.remove();
				}
			}
			Collections.sort(list, userCompare);
		}
	}

	/**
	 * Sorts the users of the Resource by their priority and waiting time
	 */
	private static class UserCompare implements Comparator<ResourceUser> {
		@Override
		public int compare(ResourceUser ru1, ResourceUser ru2) {

			// Chose the object with the highest priority entity
			// (lowest numerical value, i.e. 1 is higher priority than 2)
			int ret = Integer.compare(ru1.getPriority(), ru2.getPriority());

			// If the priorities are the same, choose the one with the longest waiting time
			if (ret == 0) {
				return Double.compare(ru2.getWaitTime(), ru1.getWaitTime());
			}
			return ret;
		}
	}
	private static UserCompare userCompare = new UserCompare();

	@Output(name = "UserList",
	 description = "The objects that can seize units from this resource.",
	    sequence = 1)
	public ArrayList<ResourceUser> getUserList(double simTime) {
		return userList;
	}

	@Output(name = "Capacity",
	 description = "The total number of resource units that can be used.",
	    unitType = DimensionlessUnit.class,
	    sequence = 2)
	public int getPresentCapacity(double simTime) {
		return getCapacity(simTime);
	}

	@Output(name = "UnitsInUse",
	 description = "The present number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	    sequence = 3)
	public int getUnitsInUse(double simTime) {
		return getUnitsInUse();
	}

	@Output(name = "AvailableUnits",
	 description = "The number of resource units that are not in use.",
	    unitType = DimensionlessUnit.class,
	    sequence = 4)
	public int getAvailableUnits(double simTime) {
		return getCapacity(simTime) - getUnitsInUse();
	}

	@Output(name = "UnitsSeized",
	 description = "The total number of resource units that have been seized.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 5)
	public int getUnitsSeized(double simTime) {
		return unitsSeized;
	}

	@Output(name = "UnitsReleased",
	 description = "The total number of resource units that have been released.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 6)
	public int getUnitsReleased(double simTime) {
		return unitsReleased;
	}

	@Output(name = "UnitsInUseAverage",
	 description = "The average number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 7)
	public double getUnitsInUseAverage(double simTime) {
		return stats.getMean(simTime);
	}

	@Output(name = "UnitsInUseStandardDeviation",
	 description = "The standard deviation of the number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 8)
	public double getUnitsInUseStandardDeviation(double simTime) {
		return stats.getStandardDeviation(simTime);
	}

	@Output(name = "UnitsInUseMinimum",
	 description = "The minimum number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 9)
	public int getUnitsInUseMinimum(double simTime) {
		return (int) stats.getMin();
	}

	@Output(name = "UnitsInUseMaximum",
	 description = "The maximum number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 10)
	public int getUnitsInUseMaximum(double simTime) {
		int ret = (int) stats.getMax();
		// A unit that is seized and released immediately
		// does not count as a non-zero maximum in use
		if (ret == 1 && freq.getBinTime(simTime, 1) == 0.0d)
			return 0;
		return ret;
	}

	@Output(name = "UnitsInUseTimes",
	 description = "The total time that the number of resource units in use was 0, 1, 2, etc.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 11)
	public double[] getUnitsInUseDistribution(double simTime) {
		return freq.getBinTimes(simTime);
	}

}
