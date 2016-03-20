/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
import com.jaamsim.basicsim.Entity;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.IntegerListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.TimeUnit;

public class Assemble extends LinkedService {

	@Keyword(description = "The service time required to perform the assembly process.",
	         exampleList = { "3.0 h", "ExponentialDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput serviceTime;

	@Keyword(description = "A list of Queue objects in which to place the arriving sub-component entities.",
	         exampleList = {"Queue1 Queue2 Queue3"})
	private final EntityListInput<Queue> waitQueueList;

	@Keyword(description = "The number of entities required from each queue for the assembly process to begin. "
			+ "The last value in the list is used if the number of queues is greater than the number of values.",
	         exampleList = {"1 2 1"})
	private final IntegerListInput numberRequired;

	@Keyword(description = "If TRUE, the all entities used in the assembly process must have the same Match value. "
			+ "The match value for an entity determined by the Match keyword for each queue. The value is calculated "
			+ "when the entity first arrives at its queue.",
	         exampleList = {"TRUE"})
	private final BooleanInput matchRequired;

	@Keyword(description = "The prototype for entities representing the assembled part.",
	         exampleList = {"Proto"})
	private final EntityInput<DisplayEntity> prototypeEntity;

	private DisplayEntity assembledEntity;	// the generated entity representing the assembled part
	private int numberGenerated = 0;  // Number of entities generated so far

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

		IntegerVector def = new IntegerVector();
		def.add(1);
		numberRequired = new IntegerListInput("NumberRequired", "Key Inputs", def);
		this.addInput(numberRequired);

		matchRequired = new BooleanInput("MatchRequired", "Key Inputs", false);
		this.addInput(matchRequired);

		prototypeEntity = new EntityInput<>(DisplayEntity.class, "PrototypeEntity", "Key Inputs", null);
		prototypeEntity.setRequired(true);
		this.addInput(prototypeEntity);
	}

	public Assemble() {}

	@Override
	public void earlyInit() {
		super.earlyInit();

		assembledEntity = null;
		numberGenerated = 0;
	}

	@Override
	public void addEntity( DisplayEntity ent ) {
		error("An entity cannot be sent directly to an Assemble object. It must be sent to the appropriate queue.");
	}

	@Override
	public ArrayList<Queue> getQueues() {
		return waitQueueList.getValue();
	}

	/**
	* Process DisplayEntities from the Queue
	*/
	@Override
	public void startAction() {

		// Do any of the thresholds stop the generator?
		if (!this.isOpen()) {
			this.setBusy(false);
			this.setPresentState();
			return;
		}

		// Do the queues have enough entities?
		ArrayList<Queue> queueList = waitQueueList.getValue();
		if (matchRequired.getValue()) {
			Integer m = Queue.selectMatchValue(queueList, numberRequired.getValue());
			if (m == null) {
				this.setBusy(false);
				this.setPresentState();
				return;
			}
			this.setMatchValue(m);
		}
		else {
			if (!Queue.sufficientEntities(queueList, numberRequired.getValue(), null)) {
				this.setBusy(false);
				this.setPresentState();
				return;
			}
		}

		// Remove the appropriate entities from each queue
		for (int i=0; i<queueList.size(); i++) {
			Queue que = queueList.get(i);
			int ind = Math.min(i, numberRequired.getValue().size()-1);
			for (int n=0; n<numberRequired.getValue().get(ind); n++) {
				DisplayEntity ent;
				ent = que.removeFirstForMatch(getMatchValue());
				if (ent == null)
					error("An entity with the specified match value %s was not found in %s.",
							getMatchValue(), que);
				this.registerEntity(ent);
				ent.kill();
			}
		}

		// Create the entity representing the assembled part
		numberGenerated++;
		DisplayEntity proto = prototypeEntity.getValue();
		StringBuilder sb = new StringBuilder();
		sb.append(this.getName()).append("_").append(numberGenerated);
		assembledEntity = Entity.fastCopy(proto, sb.toString());
		assembledEntity.earlyInit();

		// Position the assembled part relative to the Assemble object
		this.moveToProcessPosition(assembledEntity);

		// Schedule the completion of processing
		double dt = serviceTime.getValue().getNextSample(getSimTime());
		this.scheduleProcess(dt, 5, endActionTarget);
	}

	@Override
	public void endAction() {

		// Send the assembled part to the next element in the chain
		this.sendToNextComponent(assembledEntity);
		assembledEntity = null;

		// Try to assemble another part
		this.startAction();
	}

}
