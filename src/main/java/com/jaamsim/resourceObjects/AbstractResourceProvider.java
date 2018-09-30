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
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Keyword;

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

		// Prepare a list of the objects that seize this resource
		userList.clear();
		for (Entity ent : Entity.getClonesOfIterator(Entity.class, ResourceUser.class)) {
			ResourceUser ru = (ResourceUser) ent;
			if (ru.requiresResource(this))
				userList.add(ru);
		}
	}

	@Override
	public boolean isStrictOrder() {
		return strictOrder.getValue();
	}

	@Override
	public ArrayList<ResourceUser> getUserList() {
		return userList;
	}

}
