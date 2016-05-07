/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
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
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.TimeUnit;

public class Combine extends LinkedService {

	@Keyword(description = "The service time required to perform the assembly process.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput serviceTime;

	@Keyword(description = "A list of Queue objects in which to place the arriving sub-component entities.",
	         exampleList = {"Queue1 Queue2 Queue3"})
	private final EntityListInput<Queue> waitQueueList;

	private DisplayEntity processedEntity;	// the DisplayEntity being processed

	{
		waitQueue.setHidden(true);
		match.setHidden(true);

		serviceTime = new SampleInput("ServiceTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setEntity(this);
		serviceTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(serviceTime);

		waitQueueList = new EntityListInput<>(Queue.class, "WaitQueueList", "Key Inputs", null);
		waitQueueList.setRequired(true);
		this.addInput(waitQueueList);
	}

	public Combine() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		processedEntity = null;
	}

	@Override
	public void addEntity( DisplayEntity ent ) {
		error("An entity cannot be sent directly to an Combine object. It must be sent to the appropriate queue.");
	}

	@Override
	public ArrayList<Queue> getQueues() {
		return waitQueueList.getValue();
	}

	@Override
	protected boolean startProcessing(double simTime) {

		// Do the queues have enough entities?
		ArrayList<Queue> queueList = waitQueueList.getValue();
		Integer m = Queue.selectMatchValue(queueList, null);
		if (m == null) {
			return false;
		}
		this.setMatchValue(m);

		// Remove one entity from each queue
		for (int i=queueList.size()-1; i>=0; i--) {
			DisplayEntity ent = queueList.get(i).removeFirstForMatch(m);
			if (ent == null)
				error("An entity with the specified match value %s was not found in %s.",
						m, queueList.get(i));
			this.registerEntity(ent);

			// Destroy all the entities but the first
			if (i == 0)
				processedEntity = ent;
			else
				ent.kill();
		}

		// Position the processed entity relative to the Assemble object
		this.moveToProcessPosition(processedEntity);

		return true;
	}

	@Override
	protected void endProcessing(double simTime) {

		// Send the first entity to the next element in the chain
		this.sendToNextComponent(processedEntity);
		processedEntity = null;
	}

	@Override
	protected double getProcessingTime(double simTime) {
		return serviceTime.getValue().getNextSample(simTime);
	}

}
