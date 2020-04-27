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

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.units.TimeUnit;

public class Combine extends LinkedService {

	@Keyword(description = "The service time required to perform the assembly process.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput serviceTime;

	@Keyword(description = "A list of Queue objects in which to place the arriving sub-component entities.",
	         exampleList = {"Queue1 Queue2 Queue3"})
	private final EntityListInput<Queue> waitQueueList;

	@Keyword(description = "If TRUE, all the matching entities are passed to the next component.\n"
	                     + "If FALSE, only the entity in the first queue is passed on.",
	         exampleList = {"TRUE"})
	private final BooleanInput retainAll;

	private DisplayEntity[] processedEntityList;  // entities being processed

	{
		waitQueue.setHidden(true);
		match.setHidden(true);
		watchList.setHidden(true);

		serviceTime = new SampleInput("ServiceTime", KEY_INPUTS, new SampleConstant(TimeUnit.class, 0.0));
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(serviceTime);

		waitQueueList = new EntityListInput<>(Queue.class, "WaitQueueList", KEY_INPUTS, new ArrayList<Queue>());
		waitQueueList.setRequired(true);
		this.addInput(waitQueueList);

		retainAll = new BooleanInput("RetainAll", KEY_INPUTS, false);
		this.addInput(retainAll);
	}

	public Combine() {
		processedEntityList = new DisplayEntity[1];
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		processedEntityList = new DisplayEntity[waitQueueList.getListSize()];
	}

	@Override
	public void addEntity( DisplayEntity ent ) {
		error("An entity cannot be sent directly to an Combine object. It must be sent to the appropriate queue.");
	}

	@Override
	public void addQueue(Queue que) {
		ArrayList<String> toks = new ArrayList<>();
		waitQueueList.getValueTokens(toks);
		toks.add(que.getName());
		KeywordIndex kw = new KeywordIndex(waitQueueList.getKeyword(), toks, null);
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

	@Override
	public ArrayList<Queue> getQueues() {
		return waitQueueList.getValue();
	}

	@Override
	protected boolean startProcessing(double simTime) {

		// Do the queues have enough entities?
		ArrayList<Queue> queueList = waitQueueList.getValue();
		String m = Queue.selectMatchValue(queueList, null);
		if (m == null) {
			return false;
		}
		this.setMatchValue(m);

		// Remove one entity from each queue
		// (performed in reverse order so that obj is set to the entity in the first queue)
		for (int i=queueList.size()-1; i>=0; i--) {
			DisplayEntity ent = queueList.get(i).removeFirstForMatch(m);
			if (ent == null)
				error("An entity with the specified match value %s was not found in %s.",
						m, queueList.get(i));

			// Destroy all the entities but the first
			if (i > 0 && !retainAll.getValue()) {
				ent.kill();
				continue;
			}

			this.registerEntity(ent);
			processedEntityList[i] = ent;
		}

		return true;
	}

	@Override
	protected void processStep(double simTime) {

		// If specified, send all the entities to the next component
		if (retainAll.getValue()) {
			for (int i=0; i<processedEntityList.length; i++) {
				this.sendToNextComponent(processedEntityList[i]);
				processedEntityList[i] = null;
			}
		}

		// Otherwise, send just the first one
		else {
			this.sendToNextComponent(processedEntityList[0]);
			processedEntityList[0] = null;
		}
	}

	@Override
	protected double getStepDuration(double simTime) {
		return serviceTime.getValue().getNextSample(simTime);
	}

	@Override
	public boolean isFinished() {
		return processedEntityList[0] == null;
	}

	@Override
	public void updateGraphics(double simTime) {
		if (processedEntityList[0] == null)
			return;
		moveToProcessPosition(processedEntityList[0]);
	}

}
