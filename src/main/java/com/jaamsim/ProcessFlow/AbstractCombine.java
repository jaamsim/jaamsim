/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2023 JaamSim Software Inc.
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

import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.EntityProviders.EntityProvListInput;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Samples.SampleListInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

public abstract class AbstractCombine extends LinkedService {

	@Keyword(description = "The time required to process each set of entities.",
	         exampleList = { "3.0 h", "ExponentialDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput serviceTime;

	@Keyword(description = "The Queue objects in which to place the arriving entities.")
	private final EntityProvListInput<Queue> waitQueueList;

	@Keyword(description = "The number of entities required from each queue for processing to "
	                     + "begin. "
	                     + "The last value in the list is used if the number of queues is greater "
	                     + "than the number of values. "
	                     + "Only an integer number of entities can be assembled. "
	                     + "A decimal value will be truncated to an integer.",
	         exampleList = {"2 1", "{ 2 } { 1 }", "{ DiscreteDistribution1 } { 'this.obj.attrib1 + 1' }"})
	private final SampleListInput numberRequired;

	@Keyword(description = "If TRUE, the all entities to be processed must have the same Match "
	                     + "value. "
	                     + "The match value for an entity is determined by the Match keyword for "
	                     + "its queue. "
	                     + "The value is calculated when the entity first arrives at its queue.")
	private final BooleanProvInput matchRequired;

	@Keyword(description = "Determines which match value to use when several values have the "
	                     + "required number of entities. "
	                     + "If FALSE, the entity with the earliest arrival time in any of the "
	                     + "queues determines the match value. "
	                     + "If TRUE, the entity with the earliest arrival time in the first "
	                     + "queue determines the match value.")
	private final BooleanProvInput firstQueue;

	private final ArrayList<DisplayEntity> consumedEntityList = new ArrayList<>();

	{
		waitQueue.setHidden(true);
		match.setHidden(true);
		watchList.setHidden(true);
		selectionCondition.setHidden(true);
		nextEntity.setHidden(true);
		assignmentsAtStart.setHidden(true);

		serviceTime = new SampleInput("ServiceTime", KEY_INPUTS, 0.0d);
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(serviceTime);

		waitQueueList = new EntityProvListInput<>(Queue.class, "WaitQueueList", KEY_INPUTS, null);
		waitQueueList.setRequired(true);
		this.addInput(waitQueueList);

		numberRequired = new SampleListInput("NumberRequired", KEY_INPUTS, 1);
		numberRequired.setDimensionless(true);
		numberRequired.setUnitType(DimensionlessUnit.class);
		numberRequired.setIntegerValue(true);
		this.addInput(numberRequired);

		matchRequired = new BooleanProvInput("MatchRequired", KEY_INPUTS, false);
		this.addInput(matchRequired);

		firstQueue = new BooleanProvInput("FirstQueue", KEY_INPUTS, false);
		this.addInput(firstQueue);

	}

	public AbstractCombine() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		consumedEntityList.clear();
	}

	@Override
	public void addEntity( DisplayEntity ent ) {
		error("An entity cannot be sent directly to an Assemble object. It must be sent to the appropriate queue.");
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
		return waitQueueList.getNextEntityList(this, 0.0d);
	}

	public int[] getNumberRequired(double simTime) {

		// Number of values enter by the user
		int[] ret = new int[waitQueueList.getListSize()];
		for (int i = 0; i < numberRequired.getListSize(); i++) {
			int n = (int) numberRequired.getNextSample(i, this, simTime);
			ret[i] = n;
		}

		// Additional copies of the last value needed to complete the list
		int lastVal = ret[numberRequired.getListSize() - 1];
		for (int i = numberRequired.getListSize(); i < waitQueueList.getListSize(); i++) {
			ret[i] = lastVal;
		}
		return ret;
	}

	public boolean isMatchRequired(double simTime) {
		return matchRequired.getNextBoolean(this, simTime);
	}

	public boolean isFirstQueue(double simTime) {
		return firstQueue.getNextBoolean(this, simTime);
	}

	@Override
	protected double getStepDuration(double simTime) {
		return serviceTime.getNextSample(this, simTime);
	}

	/**
	 * Returns a match value that has sufficient numbers of entities in each
	 * queue. The first match value that satisfies the criterion is selected.
	 * @param queueList - list of queues to check.
	 * @param numberList - number of matches required for each queue.
	 * @return match value.
	 */
	public static String selectMatchValue(ArrayList<Queue> queueList, int[] numberList, boolean bool) {

		// Check whether each queue has sufficient entities for any match value
		for (int i=0; i<queueList.size(); i++) {
			if (queueList.get(i).getMaxCount() < numberList[i])
				return null;
		}

		// Find the queue with the fewest match values
		Queue shortest = null;
		int count = Integer.MAX_VALUE;
		for (Queue que : queueList) {
			if (que.getEntityTypes().size() < count) {
				count = que.getEntityTypes().size();
				shortest = que;
			}
		}

		// Find the match values that have sufficient entities in each queue
		ArrayList<String> matchList = new ArrayList<>();
		for (String m : shortest.getEntityTypes()) {
			if (sufficientEntities(queueList, numberList, m))
				matchList.add(m);
		}

		// Select the match value with the earliest entity arrival
		String ret = null;
		double earliestTime = Double.POSITIVE_INFINITY;
		for (String m : matchList) {
			for (Queue que : queueList) {
				double timeAdded = que.getTimeAdded(m);
				if (timeAdded < earliestTime) {
					ret = m;
					earliestTime = timeAdded;
				}
				if (bool)
					break;
			}
		}
		return ret;
	}

	/**
	 * Returns true if each of the queues contains sufficient entities with
	 * the specified match value for processing to begin.
	 * If the match value m is null, then all the entities in each queue are counted.
	 * @param queueList - list of queues to check.
	 * @param numberList - number of matches required for each queue.
	 * @param m - match value.
	 * @return true if there are sufficient entities in each queue.
	 */
	public static boolean sufficientEntities(ArrayList<Queue> queueList, int[] numberList, String m) {
		for (int i = 0; i < queueList.size(); i++) {
			if (queueList.get(i).getCount(m) < numberList[i])
				return false;
		}
		return true;
	}

	public void clearConsumedEntityList() {
		for (DisplayEntity ent : consumedEntityList) {
			ent.dispose();
		}
		consumedEntityList.clear();
	}

	public void addConsumedEntity(DisplayEntity ent) {
		consumedEntityList.add(ent);
	}

	@Override
	public ArrayList<DisplayEntity> getSourceEntities() {
		ArrayList<DisplayEntity> ret = super.getSourceEntities();
		for (Queue queue : getQueues()) {
			if (queue == null)
				continue;
			ret.add(queue);
		}
		return ret;
	}

	@Output(name = "ConsumedEntityList",
	 description = "The entities that were removed from the queues for processing and were then "
	             + "destroyed.",
	    sequence = 0)
	public ArrayList<DisplayEntity> getConsumedEntityList(double simTime) {
		return consumedEntityList;
	}

}
