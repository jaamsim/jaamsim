/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2024 JaamSim Software Inc.
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

public class ResourceUserDelegate {

	private final ArrayList<ResourceProvider> resourceList;

	public ResourceUserDelegate(ArrayList<ResourceProvider> resList) {
		resourceList = resList;
	}

	public ArrayList<ResourceProvider> getResourceList() {
		return resourceList;
	}

	public int getListSize() {
		return resourceList.size();
	}

	public boolean isEmpty() {
		return resourceList.isEmpty();
	}

	public boolean requiresResource(ResourceProvider res) {
		return resourceList.contains(res);
	}

	public boolean canSeizeResources(double simTime, int[] nums, DisplayEntity ent) {
		for (int i = 0; i < resourceList.size(); i++) {
			if (!resourceList.get(i).canSeize(simTime, nums[i], ent)) {
				return false;
			}
		}
		return true;
	}

	public void seizeResources(int[] nums, DisplayEntity ent) {
		for (int i = 0; i < resourceList.size(); i++) {
			if (nums[i] == 0)
				continue;
			resourceList.get(i).seize(nums[i], ent);
		}
	}

	public void releaseResources(int[] nums, DisplayEntity ent) {
		for (int i = 0; i < resourceList.size(); i++) {
			if (nums[i] == 0)
				continue;
			resourceList.get(i).release(nums[i], ent);
		}
	}

	public boolean hasStrictResource() {
		for (ResourceProvider res : resourceList) {
			if (res.isStrictOrder()) {
				return true;
			}
		}
		return false;
	}

}
