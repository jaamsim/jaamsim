/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.ProcessFlow;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Keyword;

public class AddTo extends AbstractPack {

	@Keyword(description = "The queue in which the waiting containers will be placed.")
	private final EntityInput<Queue> containerQueue;

	{
		numberOfEntities.setValidRange(0, Double.POSITIVE_INFINITY);

		containerQueue = new EntityInput<>(Queue.class, "ContainerQueue", KEY_INPUTS, null);
		containerQueue.setRequired(true);
		this.addInput(containerQueue);
	}

	public AddTo() {}

	@Override
	public void addEntity(DisplayEntity ent) {
		double simTime = getSimTime();

		// Add an incoming container to its queue
		if (ent instanceof EntContainer)
			containerQueue.getValue().addEntity(ent);
		else
			getQueue(simTime).addEntity(ent);
	}

	@Override
	public ArrayList<Queue> getQueues() {
		ArrayList<Queue> ret = super.getQueues();
		if (containerQueue.getValue() != null)
			ret.add(containerQueue.getValue());
		return ret;
	}

	@Override
	protected boolean isContainerAvailable() {
		return !containerQueue.getValue().isEmpty();
	}

	@Override
	protected EntContainer getNextContainer() {
		return (EntContainer) containerQueue.getValue().removeFirst();
	}

	@Override
	protected boolean startProcessing(double simTime) {

		// Is there a container waiting to be filled?
		if (container == null && containerQueue.getValue().isEmpty()) {
			return false;
		}

		return super.startProcessing(simTime);
	}

	@Override
	public ArrayList<DisplayEntity> getSourceEntities() {
		ArrayList<DisplayEntity> ret = super.getSourceEntities();
		if (containerQueue.getValue() != null)
			ret.add(containerQueue.getValue());
		return ret;
	}

}
