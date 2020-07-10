/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2020 JaamSim Software Inc.
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
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Keyword;

public class Combine extends AbstractCombine {

	@Keyword(description = "If TRUE, all the matching entities are passed to the next component.\n"
	                     + "If FALSE, only the entity in the first queue is passed on.",
	         exampleList = {"TRUE"})
	private final BooleanInput retainAll;

	private ArrayList<DisplayEntity> processedEntityList;  // entities being processed

	{
		retainAll = new BooleanInput("RetainAll", KEY_INPUTS, false);
		this.addInput(retainAll);
	}

	public Combine() {
		processedEntityList = new ArrayList<>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		processedEntityList.clear();
	}

	@Override
	public void addEntity( DisplayEntity ent ) {
		error("An entity cannot be sent directly to an Combine object. It must be sent to the appropriate queue.");
	}

	@Override
	protected boolean startProcessing(double simTime) {

		// Do the queues have enough entities?
		ArrayList<Queue> queueList = getQueues();
		int[] numList = getNumberRequired(simTime);
		if (isMatchRequired()) {
			String m = selectMatchValue(queueList, numList, isFirstQueue());
			if (m == null) {
				return false;
			}
			this.setMatchValue(m);
		}
		else {
			if (!sufficientEntities(queueList, numList, null)) {
				return false;
			}
		}

		// Remove the appropriate entities from each queue
		// (performed in reverse order so that obj is set to the entity in the first queue)
		for (int i = queueList.size() - 1; i >= 0; i--) {
			for (int n = 0; n < numList[i]; n++) {
				DisplayEntity ent = queueList.get(i).removeFirstForMatch(getMatchValue());
				if (ent == null)
					error("An entity with the specified match value %s was not found in %s.",
							getMatchValue(), queueList.get(i));

				// Destroy all the entities but the first
				if ((i > 0 || n > 0) && !retainAll.getValue()) {
					ent.kill();
					continue;
				}

				receiveEntity(ent);
				setEntityState(ent);
				processedEntityList.add(ent);
			}
		}

		return true;
	}

	@Override
	protected void processStep(double simTime) {

		// If specified, send all the entities to the next component
		if (retainAll.getValue()) {
			for (DisplayEntity ent : processedEntityList) {
				sendToNextComponent(ent);
			}
		}

		// Otherwise, send just the first one
		else {
			sendToNextComponent(processedEntityList.get(0));
		}
		processedEntityList.clear();
	}

	@Override
	public boolean isFinished() {
		return processedEntityList.isEmpty();
	}

	@Override
	public void updateGraphics(double simTime) {
		if (processedEntityList.isEmpty())
			return;
		moveToProcessPosition(processedEntityList.get(0));
	}

}
