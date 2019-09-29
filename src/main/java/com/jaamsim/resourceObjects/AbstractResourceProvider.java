/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2019 JaamSim Software Inc.
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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;

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
	private final BooleanInput strictOrder;

	private ArrayList<ResourceUser> userList;  // objects that can use this provider's units

	public final static String ERR_CAPACITY = "Insufficient resource units: available=%s, req'd=%s";

	{
		strictOrder = new BooleanInput("StrictOrder", KEY_INPUTS, false);
		this.addInput(strictOrder);
	}

	public AbstractResourceProvider() {
		userList = new ArrayList<>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		userList = getUserList(this);
	}

	@Override
	public boolean isStrictOrder() {
		return strictOrder.getValue();
	}

	@Override
	public ArrayList<ResourceUser> getUserList() {
		return userList;
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

			// If the selected object has no more entities, remove it from the list
			if (!selection.hasWaitingEntity()) {
				list.remove(selection);
			}
			// If it does have more entities, re-sort the list to account for the next entity
			else {
				Collections.sort(list, userCompare);
			}
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

}
